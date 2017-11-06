package net.es.sense.rm.driver.nsi;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.testkit.TestProbe;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.cs.CsProvider;
import net.es.sense.rm.driver.nsi.db.ModelService;
import net.es.sense.rm.driver.nsi.dds.DdsProvider;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * This class provides NSI uRA functionality including DDS integration.
 *
 * @author hacksaw
 */
@Slf4j
@lombok.Data
@lombok.NoArgsConstructor
@Component
public class RaController {

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiActorSystem nsiActorSystem;

  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private DdsProvider ddsProvider;

  @Autowired
  private CsProvider csProvider;

  @Autowired
  private ModelService modelService;

  // The AKKA actors we start in the RA.
  private ActorRef modelAuditActor;

  @PostConstruct
  public void init() {
    log.info("[RaController] created.");
  }

  @PreDestroy
  public void destroy() {
    log.info("[RaController] destroyed.");
  }

  /**
   * Start the RaController's participation in an NSI network. If an AKKA system was not provided during construction
   * then we created one.
   */
  public void start() throws IllegalArgumentException {
    log.info("[RaController] Starting RA...");

    if (nsiProperties == null) {
      throw new IllegalArgumentException("Cannot find ApplicationConfiguration context");
    }

    if (nsiActorSystem == null) {
      throw new IllegalArgumentException("Cannot find ActorSystem context");
    }

    // Start the DDS controller.
    ddsProvider.start();

    // Start the CS controller.
    csProvider.start();

    log.info("[RaController] {} started using AKKA system {}.", nsiProperties.getNsaId(), nsiActorSystem.getActorSystem().name());

    // Start the model audit actor.
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();
    modelAuditActor = actorSystem.actorOf(springExtension.props("modelAuditActor"), "ra-ModelAuditActor");

    log.info("[RaController] Completed RA system initialization.");
  }

  /**
   * Stop the RaController's participation in the NSI network. This will also terminate the AKKA system.
   */
  public void stop() throws UnsupportedOperationException {
    if (nsiActorSystem == null) {
      throw new UnsupportedOperationException("actorSystem has not been initialized: must invoke start().");
    }
    scala.concurrent.Future<Terminated> terminate = nsiActorSystem.getActorSystem().terminate();
    int count = 0;
    while (!terminate.isCompleted() && count < 20) {
      log.info("[RaController] Waiting for shutdown...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ie) {
        log.info("[RaController] sleep interupted...");
      }
      count++;
    }
  }

  public void shutdown(ActorRef actor) {
    if (nsiActorSystem == null) {
      throw new UnsupportedOperationException("actorSystem has not been initialized: must invoke start().");
    }

    TestProbe probe = new TestProbe(nsiActorSystem.getActorSystem());
    probe.watch(actor);
    log.info("[RaController] waiting for actor termination...");

    // Poison pill will be queued with a priority of 100 as the last message.
    log.info("[RaController] Sending poison pill...");
    actor.tell(PoisonPill.getInstance(), ActorRef.noSender());

    Terminated expectTerminated
            = probe.expectTerminated(actor, Duration.create(2, TimeUnit.SECONDS));
    while (!expectTerminated.getExistenceConfirmed()) {
      log.info("[RaController] waiting for actor termination...");
      expectTerminated = probe.expectTerminated(actor, Duration.create(2, TimeUnit.SECONDS));
    }
  }

  public ModelService getModelService() {
    return modelService;
  }
}
