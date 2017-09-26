package net.es.sense.rm.driver.nsi;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.testkit.TestProbe;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.configuration.NsiActorSystem;
import net.es.sense.rm.driver.nsi.configuration.NsiProperties;
import net.es.sense.rm.driver.nsi.db.ModelReader;
import net.es.sense.rm.driver.nsi.dds.DdsProvider;
import net.es.sense.rm.driver.nsi.dds.api.DocumentReader;
import net.es.sense.rm.driver.nsi.spring.SpringApplicationContext;
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
  private DocumentReader documentReader;

  @Autowired
  private ModelReader modelReader;

  // The AKKA actors we start in the RA.
  private ActorRef modelAuditActor;
  //private ActorRef modelRouter;

  /**
   * Get a managed instance of an RaController.
   *
   * @return The Spring managed RaController instance.
   */
  public static RaController getInstance() {
    RaController controller = SpringApplicationContext.getBean("raController", RaController.class);
    return controller;
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

    log.info("[RaController] {} started using AKKA system {}.", nsiProperties.getNsaId(), nsiActorSystem.getActorSystem().name());

    // Start the model audit actor.
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();
    modelAuditActor = actorSystem.actorOf(springExtension.props("modelAuditActor"), "ra-ModelAuditActor");

    // Now we start a router for dispatching Model search and read events.
    //modelRouter = actorSystem.actorOf(springExtension.props("modelRouter"), "ra-ModelRouter");

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
    while (!terminate.isCompleted()) {
      log.info("[RaController] Waiting for shutdown...");
      try {
        Thread.sleep(100);
      } catch (InterruptedException ie) {
        log.info("[RaController] sleep interupted...");
      }
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

  public DocumentReader getDocumentReader() {
    return documentReader;
  }

  public ModelReader getModelReader() {
    return modelReader;
  }

  public DdsProvider getDdsProvider() {
    return ddsProvider;
  }

  /**
  @Async
  public Future<Model> getModel(long lastModified, String id) throws ExecutionException, Exception {
    ModelQueryRequest mqr = ModelQueryRequest.builder()
            .type(ModelQueryType.QUERY_MODEL)
            .lastModified(lastModified)
            .id(id)
            .build();

    log.info("[RaController] sending query to model router.");
    Timeout timeout = new Timeout(Duration.create(5, "seconds"));
    scala.concurrent.Future<Object> future = Patterns.ask(modelRouter, mqr, timeout);
    log.info("[RaController] asking for topology {}", mqr);
    ModelQueryResult result = (ModelQueryResult) Await.result(future, timeout.duration());
    log.info("[RaController] response received, {}.", result);

    Collection<Model> models = result.getModels();

    if (ModelQueryType.QUERY_RESULT == result.getType()) {
      if (models == null || models.isEmpty()) {
        return new AsyncResult<>(null);
      } else if (models.size() > 1) {
        log.error("[RaController] multiple results returned for query.");
        models.forEach((model) -> {
          log.error("[RaController] {}", model);
        });
        throw new ExecutionException(new IllegalArgumentException("[RaController] multiple results returned for query."));
      } else {
        return new AsyncResult<>(models.iterator().next());
      }
    } else {
      throw new ExecutionException(new IllegalArgumentException("[RaController] invalid operation for context " + result.getType()));
    }
  }
  * **/
}
