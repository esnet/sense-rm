package net.es.sense.rm.driver.nsi.dds;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.dds.messages.StartMsg;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * This is a controller class that initializes the actor system.
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class DdsController {

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiActorSystem nsiActorSystem;

  @Autowired
  private NsiProperties nsiProperties;

  private ActorRef localDocumentActor;
  private ActorRef documentExpiryActor;
  private ActorRef registrationRouter;

  /**
   * public Cancellable scheduleNotification(Object message, long delay) throws BeansException { NotificationRouter
   * notificationRouter = (NotificationRouter) applicationContext.getBean("notificationRouter"); Cancellable
   * scheduleOnce = notificationRouter.scheduleNotification(message, delay); return scheduleOnce; }
   *
   * public void sendNotification(Object message) { NotificationRouter notificationRouter = (NotificationRouter)
   * applicationContext.getBean("notificationRouter"); notificationRouter.sendNotification(message); }
*
   */
  public void start() {
    // Initialize the actors.
    log.info("[DdsController] Starting DDS system initialization...");
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();

    try {
      localDocumentActor = actorSystem.actorOf(springExtension.props("localDocumentActor"), "dds-localDocumentActor");
      documentExpiryActor = actorSystem.actorOf(springExtension.props("documentExpiryActor"), "dds-documentExpiryActor");
      registrationRouter = actorSystem.actorOf(springExtension.props("registrationRouter"), "dds-registrationRouter");
    } catch (Exception ex) {
      log.error("[DdsController] Failed to initialize actor", ex);
    }

    // Kick off those that need to be started.
    StartMsg msg = new StartMsg();
    nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(60, TimeUnit.SECONDS),
            registrationRouter, msg, nsiActorSystem.getActorSystem().dispatcher(), null);

    log.info("[DdsController] Completed DDS system initialization.");
  }

  public ActorRef GetLocalDocumentActor() {
    return localDocumentActor;
  }

  public ActorRef GetDocumentExpiryActor() {
    return documentExpiryActor;
  }

  public ActorRef GetRegistrationRouter() {
    return registrationRouter;
  }

  public void terminate() {
    // We need to kill each of our actors but not touch the actorSystem.
    nsiActorSystem.shutdown(localDocumentActor);
    nsiActorSystem.shutdown(documentExpiryActor);
    nsiActorSystem.shutdown(registrationRouter);
  }
}
