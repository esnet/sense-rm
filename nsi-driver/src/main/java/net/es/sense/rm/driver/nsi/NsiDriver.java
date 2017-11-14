package net.es.sense.rm.driver.nsi;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.xml.datatype.DatatypeConfigurationException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlUtilities;
import net.es.sense.rm.driver.api.Driver;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.nsi.db.Delta;
import net.es.sense.rm.driver.nsi.db.DeltaService;
import net.es.sense.rm.driver.nsi.db.Model;
import net.es.sense.rm.driver.nsi.db.ModelService;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.model.DeltaRequest;
import net.es.sense.rm.model.DeltaResource;
import net.es.sense.rm.model.DeltaState;
import net.es.sense.rm.model.ModelResource;
import org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException;
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
  public Future<ModelResource> getModel(String id, String modelType) throws ExecutionException {
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
  public Future<Collection<ModelResource>> getModels(boolean current, String modelType) throws NotFoundException, ExecutionException {
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
          ModelResource model = new ModelResource();
          model.setId(m.getModelId());
          model.setCreationTime(XmlUtilities.longToXMLGregorianCalendar((m.getVersion() / 1000) * 1000).toXMLFormat());
          model.setModel(m.getBase());

          log.info("[getModels] return modelId = {}", model.getId());
          results.add(model);
        }
      } else {
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
  public Future<DeltaResource> propagateDelta(DeltaRequest deltaRequest, String modelType) throws ExecutionException, NotFoundException, InternalServerErrorException {
    log.info("[propagateDelta] processing delta for modelId {}", deltaRequest.getModelId());

    // Make sure the referenced model has not expired.
    ModelService modelService = raController.getModelService();
    Model model = modelService.get(deltaRequest.getModelId());
    if (model == null) {
      log.error("[NsiDriver] specified model not found, modelId = {}.", deltaRequest.getModelId());
      throw new NotFoundException("Specified model not found, modelId = " + deltaRequest.getModelId());
    }

    // We need to apply the reduction and addition to the current model so we
    // can getNewer the result of this delta for storage.
    Collection<Model> modelResult = modelService.get(true, nsiProperties.getNetworkId());
    if (modelResult == null || modelResult.isEmpty()) {
      throw new InternalServerErrorException("Could not find current model");
    }

    try {
      // Get the model on which we apply the changes.
      Model currentModel = modelResult.stream().findFirst().get();
      org.apache.jena.rdf.model.Model baseModel
              = ModelUtil.unmarshalModel(currentModel.getBase());

      // Apply the delta reduction.
      Optional<org.apache.jena.rdf.model.Model> reduction = Optional.empty();
      if (!Strings.isNullOrEmpty(deltaRequest.getReduction())) {
        reduction = Optional.ofNullable(ModelUtil.unmarshalModel(deltaRequest.getReduction()));
        reduction.ifPresent(r -> ModelUtil.applyDeltaReduction(baseModel, r));
      }

      // Apply the delta addition.
      Optional<org.apache.jena.rdf.model.Model> addition = Optional.empty();
      if (!Strings.isNullOrEmpty(deltaRequest.getAddition())) {
        addition = Optional.ofNullable(ModelUtil.unmarshalModel(deltaRequest.getAddition()));
        addition.ifPresent(a -> ModelUtil.applyDeltaAddition(baseModel, a));
      }

      // Create and store a delta object representing this request.
      DeltaService deltaService = raController.getDeltaService();
      Delta delta = new Delta();
      //delta.setDeltaId(UUID.randomUUID().toString());
      delta.setDeltaId(deltaRequest.getId());
      delta.setModelId(deltaRequest.getModelId());
      delta.setLastModified(System.currentTimeMillis());
      delta.setState(DeltaState.Accepting);
      delta.setAddition(deltaRequest.getAddition());
      delta.setReduction(deltaRequest.getReduction());
      delta.setResult(ModelUtil.marshalModel(baseModel));

      log.info("[NsiDriver] storing deltaId: {}", delta.getDeltaId());

      long id = deltaService.store(delta).getId();

      // Now process the delta which may result in an asynchronous
      // modification to the delta.
      List<String> connectionIds;
      try {
        connectionIds = raController.getCsProvider().processDelta(model, delta.getDeltaId(), reduction, addition);
      } catch (Exception ex) {
        delta = deltaService.get(id);
        delta.setState(DeltaState.Failed);
        deltaService.store(delta);
        throw ex;
      }

      // Read the delta again then update if needed.  The orchestrator should
      // not have a reference yet but just in case.
      delta = deltaService.get(id);
      if (delta.getState() == DeltaState.Accepting) {
        delta.setState(DeltaState.Accepted);
      }

      delta.setLastModified(System.currentTimeMillis());
      delta.getConnectionId().addAll(connectionIds);
      deltaService.store(delta);

      // Sent back the delta created to the orchestrator.
      DeltaResource deltaResponse = new DeltaResource();
      deltaResponse.setId(delta.getDeltaId());
      deltaResponse.setState(delta.getState());
      deltaResponse.setLastModified(XmlUtilities.longToXMLGregorianCalendar(delta.getLastModified()).toXMLFormat());
      deltaResponse.setModelId(currentModel.getModelId());
      deltaResponse.setReduction(delta.getReduction());
      deltaResponse.setAddition(delta.getAddition());
      deltaResponse.setResult(delta.getResult());

      return new AsyncResult<>(deltaResponse);
    } catch (Exception ex) {
      log.error("[NsiDriver] propagateDelta failed for modelId = {}", deltaRequest.getModelId(), ex);
      throw new ExecutionException(ex);
    }
  }

  @Override
  @Async
  public Future<DeltaResource> commitDelta(String id) throws ExecutionException, NotFoundException, TimeoutException {

    log.info("[NsiDriver] processing commitDelta for id = {}", id);

    // Make sure the referenced delta has not expired.
    DeltaService deltaService = raController.getDeltaService();
    Delta delta = deltaService.get(id);
    if (delta == null) {
      log.info("[NsiDriver] requested delta not found, id = {}.", id);
      throw new NotFoundException("Requested delta not found, id = " + id);
    }

    // Make sure we are in the correct state.
    log.info("[NsiDriver] delta state, id = {}, state = {}.", id, delta.getState().name());
    if (delta.getState().compareTo(DeltaState.Accepted) != 0) {
      log.info("[NsiDriver] requested delta not in Accepted state, id = {}, state = {}.",
              id, delta.getState().name());
      return new AsyncResult<>(null);
    }

    // Update our internal delta state.
    delta.setState(DeltaState.Committing);
    delta.setLastModified(System.currentTimeMillis());
    deltaService.store(delta);

    // Now send the NSI CS commit requests.
    List<String> connectionIds;
    try {
      connectionIds = raController.getCsProvider().commitDelta(delta.getDeltaId(), delta.getConnectionId());


    // Read the delta again then update if needed.  The orchestrator should
    // not have a reference yet but just in case.
    delta = deltaService.get(id);
    log.info("[NsiDriver] delta state, id = {}, state = {}.", id, delta.getState().name());
    if (delta.getState() == DeltaState.Committing) {
      delta.setState(DeltaState.Committed);
      log.info("[NsiDriver] delta state transition to id = {}, state = {}.", id, delta.getState().name());
    }

    delta.setLastModified(System.currentTimeMillis());
    delta.getConnectionId().clear();
    delta.getConnectionId().addAll(connectionIds);
    deltaService.store(delta);

    // Sent back the delta created to the orchestrator.
    DeltaResource deltaResponse = new DeltaResource();
    deltaResponse.setId(delta.getDeltaId());
    deltaResponse.setState(delta.getState());
    deltaResponse.setLastModified(XmlUtilities.longToXMLGregorianCalendar(delta.getLastModified()).toXMLFormat());
    deltaResponse.setModelId(delta.getModelId());
    deltaResponse.setReduction(delta.getReduction());
    deltaResponse.setAddition(delta.getAddition());
    deltaResponse.setResult(delta.getResult());

    return new AsyncResult<>(deltaResponse);
    } catch (ServiceException se) {
      log.error("NSI CS failed {}", se.getFaultInfo());
      throw new InternalServerErrorException("NSI CS failed, errorId = " + se.getFaultInfo().getErrorId() + "message = " + se.getFaultInfo().getText(), se);
    } catch (TimeoutException te) {
      log.error("NSI CS failed with timeout", te);
      throw te;
    } catch (DatatypeConfigurationException dc) {
      throw new InternalServerErrorException("XML formatters failed", dc);
    } finally {
      delta = deltaService.get(id);
      delta.setState(DeltaState.Failed);
      deltaService.store(delta);
    }
  }

  @Override
  @Async
  public Future<DeltaResource> getDelta(String id, long lastModified, String modelType) throws ExecutionException {
    log.info("[NsiDriver] processing getDelta for id = {}", id);

    // Make sure the referenced delta has not expired.
    DeltaService deltaService = raController.getDeltaService();
    Delta delta = deltaService.get(id, lastModified);
    if (delta == null) {
      log.info("[NsiDriver] requested delta not found, id = {}.", id);
      throw new NotFoundException("Requested delta not found, id = " + id);
    } else if (delta.getLastModified() < lastModified) {
      log.info("[NsiDriver] requested delta not modified, id = {}, test = {} < {}.",
              id, delta.getLastModified(), lastModified);
      return new AsyncResult<>(null);
    }

    try {
      DeltaResource response = new DeltaResource();
      response.setId(delta.getDeltaId());
      response.setModelId(delta.getModelId());
      response.setLastModified(XmlUtilities.longToXMLGregorianCalendar(delta.getLastModified()).toXMLFormat());
      response.setState(delta.getState());
      response.setResult(delta.getResult());
      response.setReduction(delta.getReduction());
      response.setAddition(delta.getAddition());
      return new AsyncResult<>(response);
    } catch (DatatypeConfigurationException ex) {
      log.error("[NsiDriver] Failed to encode delta = {}.", delta.getDeltaId());
      throw new ExecutionException(ex);
    }
  }

  @Override
  @Async
  public Future<Collection<DeltaResource>> getDeltas(long lastModified, String modelType) throws ExecutionException {
    log.info("[NsiDriver] processing getDeltas for lastModified = {}", lastModified);

    Collection<DeltaResource> results = new ArrayList<>();

    // Make sure the referenced model has not expired.
    DeltaService deltaService = raController.getDeltaService();
    Collection<Delta> deltas = deltaService.getNewer(lastModified);
    if (deltas == null || deltas.isEmpty()) {
      log.info("getDeltas: no deltas new than {}", lastModified);
    } else {
      try {
        for (Delta delta : deltas) {
          DeltaResource response = new DeltaResource();
          response.setId(delta.getDeltaId());
          response.setModelId(delta.getModelId());
          response.setLastModified(XmlUtilities.longToXMLGregorianCalendar(delta.getLastModified()).toXMLFormat());
          response.setState(delta.getState());
          response.setResult(delta.getResult());
          response.setReduction(delta.getReduction());
          response.setAddition(delta.getAddition());
          results.add(response);
        }
      } catch (DatatypeConfigurationException ex) {
        log.error("[NsiDriver] Failed to encode delta", ex);
        throw new ExecutionException(ex);
      }
    }
    return new AsyncResult<>(results);
  }
}
