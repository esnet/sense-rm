package net.es.sense.rm.driver.nsi.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.es.sense.rm.driver.nsi.RaController;
import net.es.sense.rm.driver.nsi.configuration.NsiActorSystem;
import net.es.sense.rm.driver.nsi.configuration.NsiProperties;
import net.es.sense.rm.driver.nsi.db.Model;
import net.es.sense.rm.driver.nsi.db.ModelRepository;
import net.es.sense.rm.driver.nsi.messages.AuditRequest;
import net.es.sense.rm.driver.nsi.messages.TimerMsg;
import net.es.sense.rm.driver.nsi.mrml.MrmlFactory;
import net.es.sense.rm.driver.nsi.mrml.NmlModel;
import org.apache.jena.riot.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class ModelAuditActor extends UntypedAbstractActor {

  @Autowired
  private NsiActorSystem nsiActorSystem;

  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private RaController raController;

  @Autowired
  ModelRepository modelRepository;

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), "ModelAuditActor");

  @Override
  public void preStart() {
    TimerMsg message = new TimerMsg();
    nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getModelAuditTimer(),
            TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
  }

  @Override
  public void onReceive(Object msg) {
    if (msg instanceof TimerMsg) {
      // Kick of audit and schedule next update.
      try {
        audit();
      } catch (IllegalArgumentException ex) {
        log.error("[ModelAuditActor] audit failed.", ex);
      }

      TimerMsg message = (TimerMsg) msg;
      nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getModelAuditTimer(),
              TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
    } else if (msg instanceof AuditRequest) {
      try {
        audit(((AuditRequest) msg).getTopologyId());
      } catch (IllegalArgumentException ex) {
        log.error("[ModelAuditActor] audit failed.", ex);
      }
    } else {
      unhandled(msg);
    }

    //getSender().tell(subscription, self());
  }

  @Transactional
  private void audit() {
    // Get the new document context.
    NmlModel nml = new NmlModel(raController.getDocumentReader());

    nml.getTopologyIds().forEach((topologyId) -> {
      MrmlFactory mrml = new MrmlFactory(nml, topologyId);
      // Check to see if this is a new version.
      if (modelRepository.isVersion(topologyId, mrml.getVersion())) {
        log.debug("[ModelAuditActor] found matching model topologyId = {}, version = {}.", topologyId, mrml.getVersion());
      } else {
        log.info("[ModelAuditActor] adding new topology version, topologyId = {}, version = {}", topologyId, mrml.getVersion());
        UUID uuid = UUID.randomUUID();
        Model model = new Model();
        model.setTopologyId(topologyId);
        model.setModelId(uuid.toString());
        model.setVersion(mrml.getVersion());
        model.setBase(mrml.getModelAsString(Lang.TURTLE));
        modelRepository.save(model);
      }
    });
  }

  private void audit(String topologyId) {
    // Get the new document context.
    NmlModel nml = new NmlModel(raController.getDocumentReader());
    MrmlFactory mrml = new MrmlFactory(nml, topologyId);

    // Check to see if this is a new version.
    if (modelRepository.isVersion(topologyId, mrml.getVersion())) {
      log.debug("[ModelAuditActor] found matching model topologyId = {}, version = {}.", topologyId, mrml.getVersion());
    } else {
      log.info("[ModelAuditActor] adding new topology version, topologyId = {}, version = {}", topologyId, mrml.getVersion());
      UUID uuid = UUID.randomUUID();
      Model model = new Model();
      model.setTopologyId(topologyId);
      model.setModelId(uuid.toString());
      model.setVersion(mrml.getVersion());
      model.setBase(mrml.getModelAsString(Lang.TURTLE));
      modelRepository.save(model);
    }
  }
}
