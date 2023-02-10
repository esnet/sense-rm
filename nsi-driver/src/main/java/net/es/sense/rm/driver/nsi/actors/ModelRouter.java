/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
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
import net.es.sense.rm.driver.nsi.messages.ModelQueryRequest;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
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
  LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiProperties nsiProperties;

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
