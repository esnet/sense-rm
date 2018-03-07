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

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.ws.soap.SOAPFaultException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlUtilities;
import net.es.sense.rm.driver.api.DeltaResponse;
import net.es.sense.rm.driver.api.DeltasResponse;
import net.es.sense.rm.driver.api.Driver;
import net.es.sense.rm.driver.api.ModelResponse;
import net.es.sense.rm.driver.api.ModelsResponse;
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

  private String networkId;

  /**
   * Start the NSI driver.
   */
  @PostConstruct
  public void init() {
    // Get the NSI network identifier we are modelling in MRML.
    networkId = nsiProperties.getNetworkId();
    log.info("[NsiDriver] getting model for network = {}", networkId);

    if (Strings.isNullOrEmpty(networkId)) {
      log.error("[NsiDriver] no network specified in configuration.");
      throw new InternalServerErrorException("No network specified in configuration.");
    }

    raController.start();
  }

  /**
   * Stop the NSI Driver.
   */
  @PreDestroy
  public void destroy() {
    raController.stop();
  }

  /**
   * Get a specific MRML model identified by <b>id</b>.
   *
   * @param id The model identifier to return.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @param ifModifiedSince Return model only if newer than this date, otherwise throw NotModifiedException.
   *
   * @return A Future promise to return a ModelResponse matching id if one exists.
   */
  @Override
  @Async
  public Future<ModelResponse> getModel(String id, String modelType, long ifModifiedSince) {

    ModelResponse response = new ModelResponse();

    // A valid model type must be provided.
    if (!ModelUtil.isSupported(modelType)) {
      response.setStatus(Status.BAD_REQUEST);
      response.setError(Optional.of("Specified model type = " + modelType + " not supported."));
      return new AsyncResult<>(response);
    }

    // Convert the internal referencedModel representation to the driver interface referencedModel.
    try {
      ModelService modelService = raController.getModelService();
      Model model = modelService.getByModelId(id);
      if (model == null) {
        response.setStatus(Status.NOT_FOUND);
        response.setError(Optional.of("model does not exist, id = " + id));
        return new AsyncResult<>(response);
      }

      if (model.getVersion() <= ifModifiedSince) {
        response.setStatus(Status.NOT_MODIFIED);
        return new AsyncResult<>(response);
      }

      ModelResource result = new ModelResource();
      result.setId(model.getModelId());
      result.setCreationTime(XmlUtilities.longToXMLGregorianCalendar((model.getVersion() / 1000) * 1000).toXMLFormat());
      result.setModel(model.getBase());
      response.setModel(Optional.of(result));
      return new AsyncResult<>(response);
    } catch (IllegalArgumentException | DatatypeConfigurationException ex) {
      log.error("[NsiDriver] ontology creation failed for networkId = {}.", networkId);
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("Could not generate model for networkId = " + networkId));
      return new AsyncResult<>(response);
    }
  }

  /**
   * Get a specific MRML model identified by <b>id</b>.
   *
   * @param id The model identifier to return.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).

   * @return A Future promise to return a ModelResponse matching id if one exists.
   */
  @Override
  @Async
  public Future<ModelResponse> getModel(String id, String modelType) {
    return this.getModel(id, modelType, 0L);
  }

  /**
   * Get a list of MRML models matching the specified query parameters.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @param ifModifiedSince Return model only if newer than this date, otherwise throw NotModifiedException.
   *
   * @return A Future promise to return a Collection of ModelResponse matching query.  The
   *    returned collection may be empty if no models match the requested criteria.
   */
  @Override
  @Async
  public Future<ModelsResponse> getModels(String modelType, long ifModifiedSince) {

    ModelsResponse response = new ModelsResponse();

    // A valid model type must be provided.
    if (!ModelUtil.isSupported(modelType)) {
      response.setStatus(Status.BAD_REQUEST);
      response.setError(Optional.of("Specified model type = " + modelType + " not supported."));
      return new AsyncResult<>(response);
    }

    // The RA Controller maintains a database of MRML models created from NML and NSI connections.
    ModelService modelService = raController.getModelService();
    try {
      Collection<ModelResource> results = new ArrayList<>();
      Collection<Model> models = modelService.getByTopologyId(networkId, ifModifiedSince);

      // If we did get a set of results we need to map them into a ModelResource to return.
      if (models != null) {
        for (Model m : models) {
          ModelResource model = new ModelResource();
          model.setId(m.getModelId());
          model.setCreationTime(XmlUtilities
                  .longToXMLGregorianCalendar((m.getVersion() / 1000) * 1000).toXMLFormat());
          model.setModel(m.getBase());

          log.info("[NsiDriver] found matching modelId = {}", model.getId());
          results.add(model);
        }
      } else if (modelService.countByTopologyId(networkId) != 0) {
        // There are models for this topology but none are newer than provided ifModifiedSince.
        response.setStatus(Status.NOT_MODIFIED);
        return new AsyncResult<>(response);
      } else {
        log.info("[NsiDriver] no matching model entries returned for network = {}", networkId);
      }

      response.setModels(results);
      return new AsyncResult<>(response);
    } catch (IllegalArgumentException | DatatypeConfigurationException ex) {
      log.error("[NsiDriver] ontology creation failed for networkId = {}.", networkId, ex);
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("Could not generate model for networkId = " + networkId));
      return new AsyncResult<>(response);
    }
  }

  /**
   * Get a list of MRML models.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @return A Future promise to return a Collection of ModelResponse matching query.  The
   *    returned collection may be empty if no models match the requested criteria.
   */
  @Override
  @Async
  public Future<ModelsResponse> getModels(String modelType) {
    return this.getModels(modelType, 0L);
  }

  /**
   * Get a the current MRML model matching the specified query parameters.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @param ifModifiedSince Return model only if newer than this date, otherwise throw NotModifiedException.
   *
   * @return A Future promise to return a ModelResponse matching query.  The
   *    returned model may be empty if there is no current model.
   */
  @Override
  @Async
  public Future<ModelResponse> getCurrentModel(String modelType, long ifModifiedSince) {

    ModelResponse response = new ModelResponse();

    // A valid model type must be provided.
    if (!ModelUtil.isSupported(modelType)) {
      response.setStatus(Response.Status.BAD_REQUEST);
      response.setError(Optional.of("Specified model type = " + modelType + " not supported."));
      return new AsyncResult<>(response);
    }

    // The RA Controller maintains a database of MRML models created from NML and NSI connections.
    try {
      ModelService modelService = raController.getModelService();
      Model model = modelService.getCurrent(networkId);

      // If we did get a set of results we need to map them into a ModelResource to return.
      if (model != null) {
        if (model.getVersion() <= ifModifiedSince) {
          response.setStatus(Response.Status.NOT_MODIFIED);
          return new AsyncResult<>(response);
        }

        ModelResource result = new ModelResource();
        result.setId(model.getModelId());
        result.setCreationTime(XmlUtilities
                .longToXMLGregorianCalendar((model.getVersion() / 1000) * 1000).toXMLFormat());
        result.setModel(model.getBase());

        log.info("[NsiDriver] found current matching modelId = {}", model.getIdx());
        response.setModel(Optional.of(result));
        return new AsyncResult<>(response);
      }
    } catch (IllegalArgumentException | DatatypeConfigurationException ex) {
      log.error("[NsiDriver] ontology creation failed for networkId = {}.", networkId, ex);
      response.setStatus(Response.Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("Could not generate model for networkId = " + networkId));
      return new AsyncResult<>(response);
    }

    log.info("[NsiDriver] no matching model entries returned for network = {}", networkId);
    response.setStatus(Response.Status.NOT_FOUND);
    response.setError(Optional.of("no current model exists"));
    return new AsyncResult<>(response);
  }

  /**
   * Get a the current MRML model.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @return A Future promise to return a ModelResponse containing the current MRML model.  The
   *    returned collection may be empty if there is no current model.
   */
  @Override
  @Async
  public Future<ModelResponse> getCurrentModel(String modelType) {
    return this.getCurrentModel(modelType, 0);
  }

  /**
   * Apply a MRML model delta to a specific model resource.
   *
   * @param deltaRequest The request specifying the model delta.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @return A Future promise to return a DeltaResponse containing an accepted or failed delta.
   */
  @Override
  @Async
  public Future<DeltaResponse> propagateDelta(DeltaRequest deltaRequest, String modelType) {
    DeltaResponse response = new DeltaResponse();

    // A valid model type must be provided.
    if (!ModelUtil.isSupported(modelType)) {
      response.setStatus(Status.BAD_REQUEST);
      response.setError(Optional.of("Specified model type = " + modelType + " not supported."));
      return new AsyncResult<>(response);
    }

    log.info("[propagateDelta] processing deltaId = {}, modelId = {}",
            deltaRequest.getId(), deltaRequest.getModelId());

    // Make sure the referenced referencedModel has not expired.
    ModelService modelService = raController.getModelService();
    Model referencedModel = modelService.getByModelId(deltaRequest.getModelId());
    if (referencedModel == null) {
      log.error("[NsiDriver] specified model not found, modelId = {}.", deltaRequest.getModelId());
      response.setStatus(Status.NOT_FOUND);
      response.setError(Optional.of("Specified model not found, modelId = " + deltaRequest.getModelId()));
      return new AsyncResult<>(response);
    }

    // We need to apply the reduction and addition to the current referenced model.
    Model currentModel = modelService.getCurrent(nsiProperties.getNetworkId());
    if (currentModel == null) {
      log.error("[NsiDriver] Could not find current model for networkId = {}", nsiProperties.getNetworkId());
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("Could not find current model for networkId = " + nsiProperties.getNetworkId()));
      return new AsyncResult<>(response);
    }

    try {
      // Get the referencedModel on which we apply the changes.
      org.apache.jena.rdf.model.Model rdfModel = ModelUtil.unmarshalModel(currentModel.getBase());

      // Apply the delta reduction.
      Optional<org.apache.jena.rdf.model.Model> reduction = Optional.empty();
      if (!Strings.isNullOrEmpty(deltaRequest.getReduction())) {
        reduction = Optional.ofNullable(ModelUtil.unmarshalModel(deltaRequest.getReduction()));
        reduction.ifPresent(r -> ModelUtil.applyDeltaReduction(rdfModel, r));
      }

      // Apply the delta addition.
      Optional<org.apache.jena.rdf.model.Model> addition = Optional.empty();
      if (!Strings.isNullOrEmpty(deltaRequest.getAddition())) {
        addition = Optional.ofNullable(ModelUtil.unmarshalModel(deltaRequest.getAddition()));
        addition.ifPresent(a -> ModelUtil.applyDeltaAddition(rdfModel, a));
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
      delta.setResult(ModelUtil.marshalModel(rdfModel));
      long id = deltaService.store(delta).getIdx();

      log.info("[NsiDriver] stored deltaId = {}", delta.getDeltaId());

      // Now process the delta which may result in an asynchronous
      // modification to the delta.  Keep track of the connectionId
      // so that we can commit connections associated with this
      // delta.
      try {
        raController.getCsProvider().processDelta(
                referencedModel, delta.getDeltaId(), reduction, addition);
      } catch (Exception ex) {
        log.error("[NsiDriver] NSI CS processing of delta failed,  deltaId = {}", delta.getDeltaId());
        delta = deltaService.get(id);
        delta.setState(DeltaState.Failed);
        deltaService.store(delta);

        response.setStatus(Status.INTERNAL_SERVER_ERROR);
        response.setError(Optional.of(ex.getLocalizedMessage()));
        return new AsyncResult<>(response);
      }

      // Read the delta again then update if needed.  The orchestrator should
      // not have a reference yet but just in case.
      delta = deltaService.get(id);
      if (delta.getState() == DeltaState.Accepting) {
        delta.setState(DeltaState.Accepted);
      }

      delta.setLastModified(System.currentTimeMillis());
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

      response.setStatus(Status.CREATED);
      response.setDelta(Optional.of(deltaResponse));
      return new AsyncResult<>(response);
    } catch (Exception ex) {
      log.error("[NsiDriver] propagateDelta failed for modelId = {}", deltaRequest.getModelId(), ex);
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of(ex.getLocalizedMessage()));
      return new AsyncResult<>(response);
    }
  }

  /**
   * Commit the model delta referenced by id.
   *
   * @param id The delta id to commit.
   *
   * @return A Future promise to return a DeltaResponse containing the committed delta.
   */
  @Override
  @Async
  public Future<DeltaResponse> commitDelta(String id) {
    log.info("[NsiDriver] processing commitDelta for id = {}", id);

    DeltaResponse response = new DeltaResponse();

    // Make sure the referenced delta has not expired.
    DeltaService deltaService = raController.getDeltaService();
    Delta delta = deltaService.get(id);
    if (delta == null) {
      log.info("[NsiDriver] requested delta not found, id = {}.", id);
      response.setStatus(Status.NOT_FOUND);
      response.setError(Optional.of("Requested delta not found, id = " + id));
      return new AsyncResult<>(response);
    }

    // Make sure we are in the correct state.
    log.info("[NsiDriver] delta state, id = {}, state = {}.", id, delta.getState().name());
    if (delta.getState().compareTo(DeltaState.Accepted) != 0) {
      log.info("[NsiDriver] requested delta not in Accepted state, id = {}, state = {}.",
              id, delta.getState().name());
      response.setStatus(Status.CONFLICT);
      response.setError(Optional.of("Requested delta not in Accepted state, id = " + id
              + ", state = " + delta.getState().name()));
      return new AsyncResult<>(response);
    }

    // Update our internal delta state.
    delta.setState(DeltaState.Committing);
    delta.setLastModified(System.currentTimeMillis());
    deltaService.store(delta);

    // Now send the NSI CS commit requests.
    try {
      raController.getCsProvider().commitDelta(delta.getDeltaId());

      // Read the delta again then update if needed.  The orchestrator should
      // not have a reference yet but just in case.
      delta = deltaService.get(id);
      log.info("[NsiDriver] delta state, id = {}, state = {}.", id, delta.getState().name());
      if (delta.getState() == DeltaState.Committing) {
        delta.setState(DeltaState.Committed);
        log.info("[NsiDriver] delta state transition to id = {}, state = {}.", id, delta.getState().name());
      }

      delta.setLastModified(System.currentTimeMillis());
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
      response.setStatus(Status.NO_CONTENT);
      response.setDelta(Optional.of(deltaResponse));
      return new AsyncResult<>(response);
    } catch (ServiceException se) {
      log.error("NSI CS failed {}", se.getFaultInfo());
      delta = deltaService.get(id);
      delta.setState(DeltaState.Failed);
      deltaService.store(delta);
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("NSI CS failed, errorId = " + se.getFaultInfo().getErrorId() + "message = " + se.getFaultInfo().getText()));
      return new AsyncResult<>(response);
    } catch (IllegalArgumentException | TimeoutException | DatatypeConfigurationException | SOAPFaultException ex) {
      log.error("NSI CS failed", ex);
      delta = deltaService.get(id);
      delta.setState(DeltaState.Failed);
      deltaService.store(delta);
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("NSI CS failed, message = " + ex.getLocalizedMessage()));
      return new AsyncResult<>(response);
    }
  }

  /**
   * Get a specific delta resource identified by <b>id</b>.
   *
   * @param id The delta identifier to return.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @param ifModifiedSince Return model only if newer than this date, otherwise throw NotModifiedException.
   *
   * @return A Future promise to return a DeltaResponse matching id if one exists.
   */
  @Override
  @Async
  public Future<DeltaResponse> getDelta(String id, String modelType, long ifModifiedSince) {
    log.info("[NsiDriver] processing getDelta for id = {}", id);

    DeltaResponse response = new DeltaResponse();

    // A valid model type must be provided.
    if (!ModelUtil.isSupported(modelType)) {
      response.setStatus(Status.BAD_REQUEST);
      response.setError(Optional.of("Specified model type = " + modelType + " not supported."));
      return new AsyncResult<>(response);
    }

    // Make sure the referenced delta has not expired.
    DeltaService deltaService = raController.getDeltaService();
    Delta delta = deltaService.get(id);
    if (delta == null) {
      response.setStatus(Status.NOT_FOUND);
      response.setError(Optional.of("Requested delta not found, id = " + id));
      return new AsyncResult<>(response);
    }

    if (delta.getLastModified() <= ifModifiedSince) {
      log.info("[NsiDriver] requested delta not modified, id = {}.", id);
      response.setStatus(Status.NOT_MODIFIED);
      return new AsyncResult<>(response);
    }

    try {
      DeltaResource resource = new DeltaResource();
      resource.setId(delta.getDeltaId());
      resource.setModelId(delta.getModelId());
      resource.setLastModified(XmlUtilities.longToXMLGregorianCalendar(delta.getLastModified()).toXMLFormat());
      resource.setState(delta.getState());
      resource.setResult(delta.getResult());
      resource.setReduction(delta.getReduction());
      resource.setAddition(delta.getAddition());
      response.setDelta(Optional.of(resource));
      return new AsyncResult<>(response);
    } catch (DatatypeConfigurationException ex) {
      log.error("[NsiDriver] Failed to encode delta = {}.", delta.getDeltaId());
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("Failed to encode delta id = " + delta.getDeltaId()));
      return new AsyncResult<>(response);
    }
  }

  /**
   * Get a specific delta resource identified by <b>id</b>.
   *
   * @param id The identifier of the delta to return.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @return A Future promise to return a DeltaResponse matching id if one exists.
   */
  @Override
  @Async
  public Future<DeltaResponse> getDelta(String id, String modelType) {
    return this.getDelta(id, modelType, 0L);
  }

  /**
   * Get a list of delta resources matching the specified query parameters.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @param ifModifiedSince
   *
   * @return A Future promise to return a DeltasResponse matching the specified criteria.
   */
  @Override
  @Async
  public Future<DeltasResponse> getDeltas(String modelType, long ifModifiedSince) {
    log.info("[NsiDriver] processing getDeltas for lastModified = {}", ifModifiedSince);

    DeltasResponse response = new DeltasResponse();

    // A valid model type must be provided.
    if (!ModelUtil.isSupported(modelType)) {
      response.setStatus(Status.BAD_REQUEST);
      response.setError(Optional.of("Specified model type = " + modelType + " not supported."));
      return new AsyncResult<>(response);
    }

    Collection<DeltaResource> results = new ArrayList<>();

    // Make sure the referenced referencedModel has not expired.
    DeltaService deltaService = raController.getDeltaService();
    Collection<Delta> deltas = deltaService.getNewer(ifModifiedSince);
    if (deltas == null || deltas.isEmpty()) {
      if (deltaService.count() != 0) {
        // We have deltas but none are newer than the specified date.
        log.info("[NsiDriver] no deltas new than {}", ifModifiedSince);
        response.setStatus(Status.NOT_MODIFIED);
      } else {
        log.info("[NsiDriver] no deltas in database.");
        response.setDeltas(results);
      }

      return new AsyncResult<>(response);
    }

    try {
      for (Delta delta : deltas) {
        DeltaResource resource = new DeltaResource();
        resource.setId(delta.getDeltaId());
        resource.setModelId(delta.getModelId());
        resource.setLastModified(XmlUtilities.longToXMLGregorianCalendar(delta.getLastModified()).toXMLFormat());
        resource.setState(delta.getState());
        resource.setResult(delta.getResult());
        resource.setReduction(delta.getReduction());
        resource.setAddition(delta.getAddition());
        results.add(resource);
      }
    } catch (DatatypeConfigurationException ex) {
      log.error("[NsiDriver] Failed to encode delta", ex);
      throw new InternalServerErrorException("Failed to encode delta response");
    }

    response.setDeltas(results);
    return new AsyncResult<>(response);
  }

  /**
   * Get a list of delta resources.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   * @return
   */
  @Override
  @Async
  public Future<DeltasResponse> getDeltas(String modelType) {
    return this.getDeltas(modelType, 0L);
  }
}
