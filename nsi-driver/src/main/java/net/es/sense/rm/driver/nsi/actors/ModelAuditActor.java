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

import akka.actor.Cancellable;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import net.es.sense.rm.driver.nsi.AuditService;
import net.es.sense.rm.driver.nsi.cs.CsOperations;
import net.es.sense.rm.driver.nsi.cs.db.OperationMapRepository;
import net.es.sense.rm.driver.nsi.messages.AuditRequest;
import net.es.sense.rm.driver.nsi.messages.Message;
import net.es.sense.rm.driver.nsi.messages.TerminateRequest;
import net.es.sense.rm.driver.nsi.messages.TimerMsg;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * This actor performs an audit on the topology model.
 *
 * @author hacksaw
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ModelAuditActor extends UntypedAbstractActor {
  LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private final NsiActorSystem nsiActorSystem;
  private final NsiProperties nsiProperties;
  private final AuditService auditService;
  private final OperationMapRepository operationMap;
  private Cancellable scheduled;

  /**
   * Constructor for bean to be instantiated.
   *
   * @param nsiActorSystem
   * @param nsiProperties
   * @param auditService
   * @param operationMap
   */
  public ModelAuditActor(NsiActorSystem nsiActorSystem, NsiProperties nsiProperties,
                         AuditService auditService, OperationMapRepository operationMap) {
    this.nsiActorSystem = nsiActorSystem;
    this.nsiProperties = nsiProperties;
    this.auditService = auditService;
    this.operationMap = operationMap;
  }

  @Override
  public void preStart() {
    log.info("[ModelAuditActor] starting.");
    TimerMsg message = new TimerMsg();
    scheduled = nsiActorSystem.getActorSystem().scheduler()
        .scheduleOnce(Duration.create(nsiProperties.getModelAuditTimer() + 20, TimeUnit.SECONDS),
            this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
  }

  public void cancel() {
    log.info("[ModelAuditActor] cancelling schedule isCancelled = {} ", scheduled.isCancelled());
    scheduled.cancel();
    log.info("[ModelAuditActor] cancelled schedule isCancelled = {} ", scheduled.isCancelled());
  }

  @Override
  public void onReceive(Object msg) {
    log.debug("[ModelAuditActor] onReceive {} {}", Message.getDebug(msg), msg.getClass());

    if (msg instanceof TimerMsg timer) {
      // Kick of audit and schedule next update.
      try {
        auditService.audit();
      } catch (Exception ex) {
        log.error(ex, "[ModelAuditActor] audit failed");
      }

      timer.setInitiator("ModelAuditActor:onReceive");
      timer.setPath(this.self().path());

      scheduled = nsiActorSystem.getActorSystem().scheduler().scheduleOnce(
              Duration.create(nsiProperties.getModelAuditTimer(), TimeUnit.SECONDS),
              this.getSelf(), timer, nsiActorSystem.getActorSystem().dispatcher(), null);
    } else if (msg instanceof AuditRequest ar) {
      log.info("[ModelAuditActor] AuditRequest received for topologyId={}", ar.getTopologyId());
      try {
        auditService.audit(ar.getTopologyId());
      } catch (Exception ex) {
        log.error(ex, "[ModelAuditActor] audit failed");
      }
    } else if (msg instanceof TerminateRequest req) {
      try {
        log.info("[ModelAuditActor] TerminateRequest for cid = {}", req.getConnectionId());

        CsOperations cs = new CsOperations(nsiProperties, operationMap);
        cs.terminate(req.getConnectionId());
        cs.confirm();

        log.info("[ModelAuditActor] TerminateRequest completed for cid = {}, exceptions = {}",
                req.getConnectionId(), cs.getExceptions().size());
      } catch (Exception ex) {
        log.error("[ModelAuditActor] TerminateRequest failed", ex);
      }
    } else {
      log.error("[ModelAuditActor] Unhandled message {}.", msg.getClass().getName());
      unhandled(msg);
    }
  }
}
