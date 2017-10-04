package net.es.sense.rm.driver.nsi.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.es.sense.rm.driver.nsi.db.Model;
import net.es.sense.rm.driver.nsi.db.ModelService;
import net.es.sense.rm.driver.nsi.dds.api.DocumentReader;
import net.es.sense.rm.driver.nsi.messages.AuditRequest;
import net.es.sense.rm.driver.nsi.messages.TimerMsg;
import net.es.sense.rm.driver.nsi.mrml.MrmlFactory;
import net.es.sense.rm.driver.nsi.mrml.NmlModel;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.apache.jena.riot.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
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
  private DocumentReader documentReader;

  @Autowired
  ModelService modelService;

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), "ModelAuditActor");

  @Override
  public void preStart() {
    log.info("[ModelAuditActor] starting.");
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
  }

  private void audit() {
    log.info("[ModelAuditActor] starting audit.");

    // Get the new document context.
    NmlModel nml = new NmlModel(documentReader);

    nml.getTopologyIds().forEach((topologyId) -> {
      MrmlFactory mrml = new MrmlFactory(nml, topologyId);
      // Check to see if this is a new version.
      if (modelService.isPresent(topologyId, mrml.getVersion())) {
        log.info("[ModelAuditActor] found matching model topologyId = {}, version = {}.", topologyId, mrml.getVersion());
      } else {
        log.info("[ModelAuditActor] adding new topology version, topologyId = {}, version = {}", topologyId, mrml.getVersion());
        UUID uuid = UUID.randomUUID();
        Model model = new Model();
        model.setTopologyId(topologyId);
        model.setModelId(uuid.toString());
        model.setVersion(mrml.getVersion());
        model.setBase(mrml.getModelAsString(Lang.TURTLE));
        modelService.create(model);
      }
    });

    // TODO: Go through and delete any models for topologies no longer avalable.
    // TODO: Write an audit to clean up old model versions.
    Collection<Model> models = modelService.get();
    log.info("[ModelAuditActor] stored models after the audit...");
    for (Model m : models) {
      log.info("id = {}, modelId= {}, topologyId = {}, version = {}, ", m.getId(), m.getModelId(), m.getTopologyId(), m.getVersion());
    }
  }

  private void audit(String topologyId) {
    log.info("[ModelAuditActor] starting audit for {}.", topologyId);

    // Get the new document context.
    NmlModel nml = new NmlModel(documentReader);
    MrmlFactory mrml = new MrmlFactory(nml, topologyId);

    // Check to see if this is a new version.
    if (modelService.isPresent(topologyId, mrml.getVersion())) {
      log.debug("[ModelAuditActor] found matching model topologyId = {}, version = {}.", topologyId, mrml.getVersion());
    } else {
      log.info("[ModelAuditActor] adding new topology version, topologyId = {}, version = {}", topologyId, mrml.getVersion());
      UUID uuid = UUID.randomUUID();
      Model model = new Model();
      model.setTopologyId(topologyId);
      model.setModelId(uuid.toString());
      model.setVersion(mrml.getVersion());
      model.setBase(mrml.getModelAsString(Lang.TURTLE));
      modelService.create(model);
    }
  }
}
