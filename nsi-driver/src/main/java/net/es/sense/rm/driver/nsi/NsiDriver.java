package net.es.sense.rm.driver.nsi;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.api.Delta;
import net.es.sense.rm.driver.api.DeltaRequest;
import net.es.sense.rm.driver.api.Driver;
import net.es.sense.rm.driver.api.Model;
import net.es.sense.rm.driver.nsi.db.ModelService;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class NsiDriver implements Driver {

  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private RaController raController;

  @PostConstruct
  public void init() {
    raController.start();
  }

  @PreDestroy
  public void destroy() {
    raController.stop();
  }

  @Override
  @Async
  public Future<Model> getModel(long lastModified, String modelType, String id) throws ExecutionException {
    String networkId = nsiProperties.getNetworkId();
    if (Strings.isNullOrEmpty(networkId)) {
      log.error("[NsiDriver] no network specified in configuration.");
      throw new ExecutionException(new IllegalArgumentException("No network specified in configuration."));
    }

    // Convert the internal model representation to the driver interface model.
    try {
      ModelService modelService = raController.getModelService();
      net.es.sense.rm.driver.nsi.db.Model model = modelService.get(lastModified, id);
      if (model == null) {
        return new AsyncResult<>(null);
      }
      Model result = Model.builder()
              .id(model.getModelId())
              .creationTime(model.getVersion())
              .model(model.getBase())
              .build();
      return new AsyncResult<>(result);
    } catch (IllegalArgumentException ex) {
      log.error("[NsiDriver] ontology creation failed for networkId = {}.", networkId);
      throw new ExecutionException(ex);
    }
  }

  @Override
  @Async
  public Future<Collection<Model>> getModels(long lastModified, boolean current, String modelType) throws ExecutionException {
    String networkId = nsiProperties.getNetworkId();
    if (Strings.isNullOrEmpty(networkId)) {
      log.error("[NsiDriver] no network specified in configuration.");
      throw new ExecutionException(new IllegalArgumentException("No network specified in configuration."));
    }

    // Convert the topologies NML topologies to MRML.
    try {
      Collection<Model> results = new ArrayList<>();
      ModelService modelService = raController.getModelService();
      Collection<net.es.sense.rm.driver.nsi.db.Model> models = modelService.get(lastModified, current, networkId);
      models.stream().map((m) -> Model.builder()
              .id(m.getModelId())
              .creationTime(m.getVersion())
              .model(m.getBase())
              .build()).forEachOrdered((model) -> {
                results.add(model);
      });
      return new AsyncResult<>(results);
    } catch (IllegalArgumentException ex) {
      log.error("[NsiDriver] ontology creation failed for networkId = {}.", networkId);
      throw new ExecutionException(ex);
    }
  }

  @Override
  @Async
  public Future<Delta> propagateDelta(DeltaRequest delta) throws ExecutionException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  @Async
  public Future<Delta> commitDelta(String id) throws ExecutionException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  @Async
  public Future<Delta> getDelta(long lastModified, String id) throws ExecutionException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  @Async
  public Future<Collection<Delta>> getDeltas(long lastModified, String modelType) throws ExecutionException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}