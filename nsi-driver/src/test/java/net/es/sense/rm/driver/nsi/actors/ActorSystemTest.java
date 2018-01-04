package net.es.sense.rm.driver.nsi.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
  ApplicationContext.class,
  SpringExtension.class,
  NsiProperties.class,
  NsiActorSystem.class,
  ActorUnitTestConfiguration.class })
@ActiveProfiles("test")
public class ActorSystemTest {

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiActorSystem nsiActorSystem;

  private static final String MESSAGE = "test message";

  @Test
  public void test() throws Exception {
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();
    log.info("[ActorSystemTest] Testing AKKA system {}", actorSystem.name());
    ActorRef testActor = actorSystem.actorOf(springExtension.props("testActor"), "boo-testActor");

    log.info("[ActorSystemTest] sending message to actor.");
    Timeout timeout = new Timeout(Duration.create(5, "seconds"));
    scala.concurrent.Future<Object> future = Patterns.ask(testActor, MESSAGE, timeout);
    log.info("[ActorSystemTest] waiting for response.");
    String result = (String) Await.result(future, timeout.duration());
    log.info("[ActorSystemTest] response received = {}.", result);

    Assert.assertEquals(MESSAGE, result);
  }
}
