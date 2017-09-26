package net.es.sense.rm.driver.nsi.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.util.ArrayList;
import java.util.Collection;
import net.es.sense.rm.driver.nsi.db.Model;
import net.es.sense.rm.driver.nsi.db.ModelRepository;
import net.es.sense.rm.driver.nsi.messages.ModelQueryRequest;
import net.es.sense.rm.driver.nsi.messages.ModelQueryResult;
import net.es.sense.rm.driver.nsi.messages.ModelQueryType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class ModelActor extends UntypedAbstractActor {

  @Autowired
  ModelRepository modelRepository;

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), "ModelActor");

  @Override
  public void preStart() {
    log.debug("[ModelActor] starting...");
  }

  @Override
  @Transactional
  public void onReceive(Object msg) {
    if (msg instanceof ModelQueryRequest) {
      try {
        ModelQueryRequest query = (ModelQueryRequest) msg;
        Collection<Model> models = new ArrayList<>();
        switch (query.getType()) {
          case QUERY_MODEL:
            log.info("[ModelActor] QUERY_MODEL {} {}", query.getId(), query.getLastModified());
            Model qm = modelRepository.findByModelIdAndVersion(query.getId(), query.getLastModified());
            log.info("[ModelActor] result {}", qm);

            if (qm != null) {
              models.add(qm);
            }
            break;
          case QUERY_MODELS:
            if (query.isCurrent()) {
              Model qms = modelRepository.findCurrentModelForTopologyId(query.getTopologyId());
              if (qms != null) {
                models.add(qms);
              }
            } else {
              Iterable<Model> iqms = modelRepository.findTopologyIdNewerThanVersion(query.getTopologyId(), query.getLastModified());
              iqms.forEach(q -> models.add(q));
            }
            break;
        }

        ModelQueryResult result = new ModelQueryResult(ModelQueryType.QUERY_RESULT, models);
        getSender().tell(result, self());
      } catch (Exception ex) {
        log.error("[ModelActor] caught exception", ex);
        ModelQueryResult result = new ModelQueryResult(ModelQueryType.QUERY_ERROR, null);
        getSender().tell(result, self());
      }
    } else {
      log.error("[ModelActor] Invalid message type {}", msg.getClass().getName());
      ModelQueryResult result = new ModelQueryResult(ModelQueryType.QUERY_ERROR, null);
      getSender().tell(result, self());
    }
  }
}
