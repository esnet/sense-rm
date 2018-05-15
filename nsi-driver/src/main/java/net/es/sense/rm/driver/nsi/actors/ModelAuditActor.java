package net.es.sense.rm.driver.nsi.actors;

import akka.actor.Cancellable;
import akka.actor.UntypedAbstractActor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.AuditService;
import net.es.sense.rm.driver.nsi.messages.AuditRequest;
import net.es.sense.rm.driver.nsi.messages.TimerMsg;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
@Scope("prototype")
public class ModelAuditActor extends UntypedAbstractActor {

  @Autowired
  private NsiActorSystem nsiActorSystem;

  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private AuditService auditService;

  private Cancellable scheduled;

  @Override
  public void preStart() {
    log.info("[ModelAuditActor] starting.");
    TimerMsg message = new TimerMsg();
    scheduled = nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getModelAuditTimer() + 20,
            TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
  }

  public void cancel() {
    log.info("[ModelAuditActor] schedule cancelled: {} ", scheduled.isCancelled());
    scheduled.cancel();
    log.info("[ModelAuditActor] schedule cancelled: {} ", scheduled.isCancelled());
  }

  @Override
  public void onReceive(Object msg) {
    if (msg instanceof TimerMsg) {
      // Kick of audit and schedule next update.
      try {
        auditService.audit();
      } catch (Exception ex) {
        log.error("[ModelAuditActor] audit failed", ex);
      }

      TimerMsg message = (TimerMsg) msg;
      scheduled = nsiActorSystem.getActorSystem().scheduler().scheduleOnce(
              Duration.create(nsiProperties.getModelAuditTimer(), TimeUnit.SECONDS),
              this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
    } else if (msg instanceof AuditRequest) {
      try {
        auditService.audit(((AuditRequest) msg).getTopologyId());
      } catch (Exception ex) {
        log.error("[ModelAuditActor] audit failed, {}", ex);
      }
    } else {
      unhandled(msg);
    }
  }
}