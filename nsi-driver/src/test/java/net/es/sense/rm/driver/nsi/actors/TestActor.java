package net.es.sense.rm.driver.nsi.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import net.es.sense.rm.driver.nsi.messages.Message;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
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
      log.error("[TestActor::onReceive] Unhandled event {}", Message.getDebug(msg));
      unhandled(msg);
    }
  }
}
