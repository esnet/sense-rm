/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.sense.rm.driver.nsi.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class TestActor extends UntypedAbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().system(), "TestActor");

  @Override
  public void preStart() throws Exception {
    super.preStart();
    log.info("[TestActor] starting...");
  }

  @Override
  public void onReceive(Object msg) {
    if (msg instanceof String) {
        getSender().tell(msg, self());
    } else {
      unhandled(msg);
    }
  }
}
