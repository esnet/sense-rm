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
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.testkit.TestProbe;
import com.typesafe.config.ConfigFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringApplicationContext;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class NsiActorSystem {

  // The application context is needed to initialize the Akka Spring Extension.
  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiProperties nsiProperties;

  private ActorSystem actorSystem;

  @PostConstruct
  public void init() throws UnsupportedOperationException {
    if (springExtension == null) {
      throw new UnsupportedOperationException("springExtension has not been initialized.");
    }

    log.info("[NsiActorSystem] Initializing DDS actor framework for uRA = {}", nsiProperties.getNsaId());

    // Initialize the AKKA actor system.
    actorSystem = ActorSystem.create("SENSE-NSI-System", ConfigFactory.load());

    // Initialize the application context in the Akka Spring Extension
    springExtension.initialize(applicationContext);

    log.info("[NsiActorSystem] ... actor framework initialized for uRA = {}", nsiProperties.getNsaId());
  }

  public static NsiActorSystem getInstance() {
    NsiActorSystem dds = SpringApplicationContext.getBean("nsiActorSystem", NsiActorSystem.class);
    return dds;
  }

  public ActorSystem getActorSystem() throws UnsupportedOperationException {
    if (actorSystem == null) {
      throw new UnsupportedOperationException("actorSystem has not been initialized.");
    }
    return actorSystem;
  }

  /**
   * Terminate the DdsController's participation in the NSI network. This will also terminate the AKKA system.
   */
  public void terminate() {
    Date uptime = new Date();
    uptime.setTime(actorSystem.uptime());
    log.info("[NsiActorSystem] Shutting down actor system after uptime: {}", uptime);

    Future<Terminated> terminate = actorSystem.terminate();
    while (!terminate.isCompleted()) {
      log.info("[NsiActorSystem] Waiting for shutdown...");
      try {
        Thread.sleep(100);
      } catch (InterruptedException ie) {
        log.info("[NsiActorSystem] sleep interupted...");
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
    TestProbe probe = new TestProbe(actorSystem);
    probe.watch(actor);
    log.info("[NsiActorSystem] waiting for actor termination...");

    // Poison pill will be queued with a priority of 100 as the last message.
    log.info("[NsiActorSystem] Sending poison pill...");
    actor.tell(PoisonPill.getInstance(), ActorRef.noSender());

    Terminated expectTerminated
            = probe.expectTerminated(actor, Duration.create(2, TimeUnit.SECONDS));
    while (!expectTerminated.getExistenceConfirmed()) {
      log.info("[NsiActorSystem] waiting for actor termination...");
      expectTerminated = probe.expectTerminated(actor, Duration.create(2, TimeUnit.SECONDS));
    }
  }
}
