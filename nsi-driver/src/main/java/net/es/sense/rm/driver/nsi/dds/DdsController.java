package net.es.sense.rm.driver.nsi.dds;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.testkit.TestProbe;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.dds.messages.StartMsg;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.concurrent.Future;
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
    registrationRouter.tell(msg, null);

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

  /**
   * Terminate the DdsController's participation in the NSI network. This will also terminate the AKKA system.
   */
  public void terminate() throws UnsupportedOperationException {
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();
    if (actorSystem == null) {
      throw new UnsupportedOperationException("DdsActorSystem has not been initialized.");
    }

    Date uptime = new Date();
    uptime.setTime(actorSystem.uptime());
    log.info("[DdsController] Shutting down actor system after uptime: {}", uptime);

    Future<Terminated> terminate = actorSystem.terminate();
    while (!terminate.isCompleted()) {
      log.info("[DdsController] Waiting for shutdown...");
      try {
        Thread.sleep(100);
      } catch (InterruptedException ie) {
        log.info("[DdsController] sleep interupted...");
      }
    }
  }

  /**
   * Shutdown the active DDS actors but leave AKKA running.
   *
   * @param actor
   * @throws UnsupportedOperationException
   */
  public void shutdown(ActorRef actor) throws UnsupportedOperationException {
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();
    if (actorSystem == null) {
      throw new UnsupportedOperationException("DdsActorSystem has not been initialized.");
    }

    TestProbe probe = new TestProbe(actorSystem);
    probe.watch(actor);
    log.info("[DdsController] waiting for actor termination...");

    // Poison pill will be queued with a priority of 100 as the last message.
    log.info("[DdsController] Sending poison pill...");
    actor.tell(PoisonPill.getInstance(), ActorRef.noSender());

    Terminated expectTerminated
            = probe.expectTerminated(actor, Duration.create(2, TimeUnit.SECONDS));
    while (!expectTerminated.getExistenceConfirmed()) {
      log.info("[DdsController] waiting for actor termination...");
      expectTerminated = probe.expectTerminated(actor, Duration.create(2, TimeUnit.SECONDS));
    }
  }
}
