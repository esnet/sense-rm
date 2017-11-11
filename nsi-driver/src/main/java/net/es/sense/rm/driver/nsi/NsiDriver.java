package net.es.sense.rm.driver.nsi;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.NotFoundException;
import javax.xml.datatype.DatatypeConfigurationException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlUtilities;
import net.es.sense.rm.driver.api.Driver;
import net.es.sense.rm.driver.nsi.db.Model;
import net.es.sense.rm.driver.nsi.db.ModelService;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.model.DeltaRequest;
import net.es.sense.rm.model.DeltaResource;
import net.es.sense.rm.model.ModelResource;
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
  public Future<ModelResource> getModel(String modelType, String id) throws ExecutionException {
    String networkId = nsiProperties.getNetworkId();
    if (Strings.isNullOrEmpty(networkId)) {
      log.error("[NsiDriver] no network specified in configuration.");
      throw new ExecutionException(new IllegalArgumentException("No network specified in configuration."));
    }

    // Convert the internal model representation to the driver interface model.
    try {
      ModelService modelService = raController.getModelService();
      Model model = modelService.get(id);
      if (model == null) {
        return new AsyncResult<>(null);
      }

      ModelResource result = new ModelResource();
      result.setId(model.getModelId());
      result.setCreationTime(XmlUtilities.longToXMLGregorianCalendar((model.getVersion() / 1000) * 1000).toXMLFormat());
      result.setModel(model.getBase());
      return new AsyncResult<>(result);
    } catch (IllegalArgumentException | DatatypeConfigurationException ex) {
      log.error("[NsiDriver] ontology creation failed for networkId = {}.", networkId);
      throw new ExecutionException(ex);
    }
  }

  @Override
  @Async
  public Future<Collection<ModelResource>> getModels(boolean current, String modelType) throws ExecutionException {
    String networkId = nsiProperties.getNetworkId();
    log.info("[getModels] getting model for network = {}", networkId);

    if (Strings.isNullOrEmpty(networkId)) {
      log.error("[NsiDriver] no network specified in configuration.");
      throw new ExecutionException(new IllegalArgumentException("No network specified in configuration."));
    }

    // Convert the topologies NML topologies to MRML.
    try {
      Collection<ModelResource> results = new ArrayList<>();
      ModelService modelService = raController.getModelService();
      Collection<Model> models = modelService.get(current, networkId);
      if (models != null) {
        for (Model m : models) {
          log.info("[getModels] model = {}", m);
          ModelResource model = new ModelResource();
          model.setId(m.getModelId());
          model.setCreationTime(XmlUtilities.longToXMLGregorianCalendar((m.getVersion() / 1000) * 1000).toXMLFormat());
          model.setModel(m.getBase());

          log.info("[getModels] return modelId = {}", model.getId());
          results.add(model);
        }
      }
      else {
        log.info("[getModels] could not fine any entries for network = {}", networkId);
      }

      return new AsyncResult<>(results);
    } catch (IllegalArgumentException | DatatypeConfigurationException ex) {
      log.error("[NsiDriver] ontology creation failed for networkId = {}.", networkId);
      throw new ExecutionException(ex);
    }
  }

  @Override
  @Async
  public Future<DeltaResource> propagateDelta(String modelType, DeltaRequest delta) throws ExecutionException, NotFoundException {
    log.info("[propagateDelta] processing delta for modelId {}", delta.getModelId());

    // Make sure the referenced model has not expired.
    ModelService modelService = raController.getModelService();
    Model model = modelService.get(delta.getModelId());
    if (model == null) {
      log.error("[NsiDriver] specified model not found, modelId = {}.", delta.getModelId());
      throw new NotFoundException("Specified model not found, modelId = " + delta.getModelId());
    }

    DeltaResource deltaResponse = new DeltaResource();
    return new AsyncResult<>(deltaResponse);
  }

  @Override
  @Async
  public Future<DeltaResource> commitDelta(String id) throws ExecutionException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  @Async
  public Future<DeltaResource> getDelta(long lastModified, String id) throws ExecutionException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  @Async
  public Future<Collection<DeltaResource>> getDeltas(long lastModified, String modelType) throws ExecutionException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
