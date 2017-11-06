package net.es.sense.rm.driver.nsi.cs;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class CsController {

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiActorSystem nsiActorSystem;

  private ActorRef connectionActor;

  public void start() {
    // Initialize the actors.
    log.info("[CsController] Starting NSI CS system initialization...");
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();

    try {
      connectionActor = actorSystem.actorOf(springExtension.props("connectionActor"), "nsi-connectionActor");
    } catch (Exception ex) {
      log.error("[CsController] Failed to initialize actor", ex);
    }

    log.info("[CsController] Completed NSI CS system initialization.");
  }

  public ActorRef GetConnectionActor() {
    return connectionActor;
  }

  public void terminate() {
    nsiActorSystem.shutdown(connectionActor);
  }
}
