package net.es.sense.rm.driver.nsi.dds.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.messages.Message;
import net.es.sense.rm.driver.nsi.messages.StartMsg;
import net.es.sense.rm.driver.nsi.messages.TimerMsg;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * This actor fires periodically to inspect the document directory on permanent
 * storage for any new or updated documents.  These are not cached documents, but
 * documents the load nsi-dds instance will advertise.  All constructor parameters
 * and properties configured via beans.xml file.
 *
 * @author hacksaw
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class LocalDocumentActor extends UntypedAbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
  private final NsiActorSystem nsiActorSystem;
  private final NsiProperties nsiProperties;

  /**
   * Constructor initialized by Spring.
   *
   * @param nsiActorSystem Reference to the AKKA system.
   * @param nsiProperties The configuration properties.
   */
  public LocalDocumentActor(NsiActorSystem nsiActorSystem, NsiProperties nsiProperties) {
    this.nsiActorSystem = nsiActorSystem;
    this.nsiProperties = nsiProperties;
  }

  /**
   * Initialize the actor by scheduling a timer message.
   */
  @Override
  public void preStart() {
    log.debug("[LocalDocumentActor] preStart().");
    TimerMsg message = new TimerMsg("LocalDocumentActor:preStart", this.self().path());
    nsiActorSystem.getActorSystem().scheduler()
        .scheduleOnce(Duration.create(nsiProperties.getDdsAuditTimer(), TimeUnit.SECONDS),
            this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
  }

  /**
   * Process an incoming message to the actor.  This is typically a timer
   * message triggering a load of documents from the local document repository.
   *
   * @param msg Message to process.
   */
  @Override
  public void onReceive(Object msg) {
    log.debug("[LocalDocumentActor] onReceive {}.", Message.getDebug(msg));

    // We can ignore the broadcast start message.
    if (msg instanceof StartMsg) {
      log.debug("[LocalDocumentActor] ignoring unimplemented StartMsg.");
    } if (msg instanceof TimerMsg message) {
      // TODO: Do nothing at this point.  We have this here just in case we need to expand.

      // Update the sender of this message for tracing.
      message.setInitiator("LocalDocumentActor:onReceive");
      message.setPath(this.self().path());

      // Insert code here to handle local documents (i.e. create and push to remote DDS server?
      nsiActorSystem.getActorSystem().scheduler()
          .scheduleOnce(Duration.create(nsiProperties.getDdsAuditTimer(), TimeUnit.SECONDS),
              this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
    } else {
      log.error("[LocalDocumentActor] onReceive unhandled message {} {}",
          this.getSender(), Message.getDebug(msg));
      unhandled(msg);
    }
  }
}
