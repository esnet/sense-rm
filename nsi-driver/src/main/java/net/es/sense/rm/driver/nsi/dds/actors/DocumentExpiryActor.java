package net.es.sense.rm.driver.nsi.dds.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.common.collect.Lists;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.dds.db.Document;
import net.es.sense.rm.driver.nsi.dds.db.DocumentService;
import net.es.sense.rm.driver.nsi.messages.Message;
import net.es.sense.rm.driver.nsi.messages.StartMsg;
import net.es.sense.rm.driver.nsi.messages.TimerMsg;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * This actor fires periodically to inspect the DDS document cache for any expired documents.
 *
 * @author hacksaw
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DocumentExpiryActor extends UntypedAbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private final NsiActorSystem nsiActorSystem;
  private final DocumentService documentService;
  private final NsiProperties nsiProperties;

  /**
   * Constructor for the DocumentExpiryActor.
   *
   * @param nsiActorSystem
   * @param documentService
   * @param nsiProperties
   */
  public DocumentExpiryActor(NsiActorSystem nsiActorSystem, DocumentService documentService,
                             NsiProperties nsiProperties) {
    log.info("[DocumentExpiryActor] Constructing");
    this.nsiActorSystem = nsiActorSystem;
    this.documentService = documentService;
    this.nsiProperties = nsiProperties;
  }

  /**
   * Initialize the actor by scheduling a timer message.
   */
  @Override
  public void preStart() {
    TimerMsg message = new TimerMsg("DocumentExpiryActor:preStart", this.self().path());
    nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getDdsAuditTimer(),
            TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
  }

  /**
   * Process an incoming message to the actor.  This is typically a timer
   * message triggering an audit for expired documents in the cache.
   *
   * @param msg
   */
  @Override
  public void onReceive(Object msg) {
    log.debug("[DocumentExpiryActor] onReceive {}", Message.getDebug(msg));

    // We can ignore the broadcast start message.
    if (msg instanceof StartMsg) {
      log.debug("[DocumentExpiryActor] ignoring unimplemented StartMsg.");
    } else if (msg instanceof TimerMsg message) {
      log.info("[DocumentExpiryActor]: auditing expired documents.");
      expire();
      message.setInitiator("DocumentExpiryActor:onReceive");
      message.setPath(this.getSelf().path());
      nsiActorSystem.getActorSystem().scheduler()
          .scheduleOnce(Duration.create(nsiProperties.getDdsAuditTimer(), TimeUnit.SECONDS), this.getSelf(),
              message, nsiActorSystem.getActorSystem().dispatcher(), this.getSelf());
    } else {
      log.error("[DocumentExpiryActor] onReceive unhandled message {} {}", this.getSender(), Message.getDebug(msg));
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
