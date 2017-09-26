package net.es.sense.rm.driver.nsi.dds.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.concurrent.TimeUnit;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.dds.messages.TimerMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
/**
 * This actor fires periodically to inspect the document directory on permanent storage for any new or updated
 * documents.
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class LocalDocumentActor extends UntypedAbstractActor {

  @Autowired
  private NsiActorSystem nsiActorSystem;

  @Autowired
  private NsiProperties nsiProperties;

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), "LocalDocumentActor");

  @Override
  public void preStart() {
    log.debug("[LocalDocumentActor] preStart().");
    TimerMsg message = new TimerMsg();
    nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getDdsAuditTimer(),
            TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
  }

  @Override
  public void onReceive(Object msg) {
    log.debug("[LocalDocumentActor] onRecieve({}).", msg.getClass().getName());
    if (msg instanceof TimerMsg) {
      TimerMsg message = (TimerMsg) msg;
      // Insert code here to handle local documents (i.e. create and push to remote DDS server?
      nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getDdsAuditTimer(),
              TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
    } else {
      unhandled(msg);
    }
  }
}
