package net.es.sense.rm.driver.nsi.dds.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.dds.db.Document;
import net.es.sense.rm.driver.nsi.dds.db.DocumentService;
import net.es.sense.rm.driver.nsi.dds.messages.TimerMsg;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * This actor fires periodically to inspect the DDS document cache for any expired documents.
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class DocumentExpiryActor extends UntypedAbstractActor {

  @Autowired
  private NsiActorSystem nsiActorSystem;

  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private DocumentService documentService;

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), "DocumentExpiryActor");

  @Override
  public void preStart() {
    TimerMsg message = new TimerMsg();
    nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getDdsAuditTimer(),
            TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
  }

  @Override
  public void onReceive(Object msg) {
    if (msg instanceof TimerMsg) {
      expire();
      TimerMsg message = (TimerMsg) msg;
      nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getDdsAuditTimer(),
              TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
    } else {
      unhandled(msg);
    }
  }

  /**
   * Expire any documents in the cache past expire time plus the expiryInterval
   * offset.  We give this extra padding to allow clients to get any delete
   * updates that were sent (deletes are done in the DDS protocol by setting
   * expire time to now.
   */
  public void expire() {
    Date now = new Date();
    now.setTime(now.getTime() + nsiProperties.getDdsExpiryInterval() * 1000);

    Collection<Document> expired = Lists.newArrayList(documentService.getExpired(now.getTime()));

    for (Document document : expired) {
      log.debug("[DocumentExpiryActor] document has expired, id = {}, expires = {} ", document.getId(), new Date(document.getExpires()));
      documentService.delete(document.getId());

      // Send notification to user API??
    }
  }
}
