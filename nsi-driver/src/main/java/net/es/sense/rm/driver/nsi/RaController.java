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
package net.es.sense.rm.driver.nsi;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Terminated;
import akka.testkit.TestProbe;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.cs.CsProvider;
import net.es.sense.rm.driver.nsi.db.DeltaService;
import net.es.sense.rm.driver.nsi.db.ModelService;
import net.es.sense.rm.driver.nsi.dds.DdsProvider;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

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
  private DeltaService deltaService;

  @Autowired
  private ModelService modelService;

  @Autowired
  private AuditServiceBean auditService;

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
   * Start the RaController's participation in an NSI network.
   * 
   * @throws java.security.KeyManagementException
   * @throws java.security.NoSuchAlgorithmException
   * @throws java.security.NoSuchProviderException
   * @throws java.security.KeyStoreException
   * @throws java.security.UnrecoverableKeyException
   * @throws java.io.IOException
   * @throws java.security.cert.CertificateException
   */
  public void start() throws IllegalArgumentException, KeyManagementException, NoSuchAlgorithmException,
          NoSuchProviderException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {
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

    // Perform a model audit to consolitate the loaded DDS and CS information.
    auditService.audit();

    // Start the model audit actor.
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();
    modelAuditActor = actorSystem.actorOf(springExtension.props("modelAuditActor"), "ra-ModelAuditActor");

    log.info("[RaController] {} started using AKKA system {}.", nsiProperties.getNsaId(), nsiActorSystem.getActorSystem().name());
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

  public DeltaService getDeltaService() {
    return deltaService;
  }

  public ActorRef getModelAuditActor() {
    return modelAuditActor;
  }
}
