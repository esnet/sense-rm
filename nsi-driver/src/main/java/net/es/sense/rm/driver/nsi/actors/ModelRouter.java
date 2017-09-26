package net.es.sense.rm.driver.nsi.actors;

import akka.actor.ActorRef;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import java.util.ArrayList;
import java.util.List;
import net.es.sense.rm.driver.nsi.configuration.NsiProperties;
import net.es.sense.rm.driver.nsi.messages.ModelQueryRequest;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class ModelRouter extends UntypedAbstractActor {

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiProperties nsiProperties;

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), "ModelRouter");

  private Router router;

  @Override
  public void preStart() {
    log.info("[ModelRouter] Initializing ModelActors, debug = {}.", log.isDebugEnabled());

    List<Routee> routees = new ArrayList<>();
    for (int i = 0; i < nsiProperties.getDdsPoolSize(); i++) {
      ActorRef r = getContext().actorOf(springExtension.props("modelActor"));
      getContext().watch(r);
      routees.add(new ActorRefRoutee(r));
    }
    router = new Router(new RoundRobinRoutingLogic(), routees);

    log.info("[ModelRouter] Initialization completed.");
  }

  @Override
  public void onReceive(Object msg) {
    if (msg instanceof ModelQueryRequest) {
      router.route(msg, getSender());
    } else {
      log.error("[ModelRouter] Unhandled message {}.", msg.getClass().getName());
      unhandled(msg);
    }
  }
}
