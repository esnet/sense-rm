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
package net.es.sense.rm.api;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlUtilities;
import net.es.sense.rm.driver.api.DeltaResponse;
import net.es.sense.rm.driver.api.DeltasResponse;
import net.es.sense.rm.driver.api.Driver;
import net.es.sense.rm.driver.api.ModelResponse;
import net.es.sense.rm.driver.api.ModelsResponse;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.model.DeltaRequest;
import net.es.sense.rm.model.DeltaResource;
import net.es.sense.rm.model.DeltaState;
import net.es.sense.rm.model.ModelResource;
import org.assertj.core.util.Strings;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class TestDriver implements Driver {
  private static final String MODEL_FILE = "src/test/resources/models.json";
  private static final String DELTA_FILE = "src/test/resources/deltas.json";

  private final Map<String, ModelResource> models;
  private final Map<String, DeltaResource> deltas;

  public TestDriver() throws JAXBException, IOException, DatatypeConfigurationException {
    JsonProxy json = new JsonProxy();

    try (FileReader fileReader = new FileReader(MODEL_FILE)) {
      log.info("[TestDriver] Reading resource model file: {}", MODEL_FILE);
      List<ModelResource> input = json.deserializeList(fileReader, ModelResource.class);
      models = input.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));
      log.info("[TestDriver] Read {} model entries.", input.size());
      for (ModelResource m : models.values()) {
        log.info(m.toString());
      }
    }

    try (FileReader fileReader = new FileReader(DELTA_FILE)) {
      log.info("[TestDriver] Reading resource delta file: {}", DELTA_FILE);
      List<DeltaResource> input = json.deserializeList(fileReader, DeltaResource.class);
      deltas = input.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));
      log.info("[TestDriver] Read {} delta entries.", input.size());
      for (DeltaResource d : deltas.values()) {
        log.info(d.toString());
      }
    }
  }

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

    ModelResource model = models.get(id);
    if (model == null) {
      response.setStatus(Status.NOT_FOUND);
      response.setError(Optional.of("model does not exist, id = " + id));
      return new AsyncResult<>(response);
    }

    try {
      long creationTime = XmlUtilities.xmlGregorianCalendar(model.getCreationTime())
                  .toGregorianCalendar().getTimeInMillis();
      if (creationTime <= ifModifiedSince) {
        response.setStatus(Status.NOT_MODIFIED);
        return new AsyncResult<>(response);
      }
    } catch (DatatypeConfigurationException ex) {
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("Error converting creationTime for model id = " + id));
      return new AsyncResult<>(response);
    }

    response.setModel(Optional.of(model));
    return new AsyncResult<>(response);
  }

  @Override
  @Async
  public Future<ModelResponse> getModel(String id, String modelType) {
    return this.getModel(id, modelType, 0L);
  }

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

    Collection<ModelResource> results = new ArrayList<>();
    for (ModelResource m : models.values()) {
      try {
        long creationTime = XmlUtilities.xmlGregorianCalendar(m.getCreationTime())
                .toGregorianCalendar().getTimeInMillis();
        if (creationTime > ifModifiedSince) {
          results.add(m);
        }
      } catch (DatatypeConfigurationException ex) {
        response.setStatus(Status.INTERNAL_SERVER_ERROR);
        response.setError(Optional.of("Error converting creationTime for model id = " + m.getId()));
        return new AsyncResult<>(response);
      }
    }

    if (results.isEmpty() && !models.isEmpty()) {
      response.setStatus(Status.NOT_MODIFIED);
      return new AsyncResult<>(response);
    }

    response.setModels(results);
    return new AsyncResult<>(response);
  }

  @Override
  @Async
  public Future<ModelsResponse> getModels(String modelType) {
    return this.getModels(modelType, 0L);
  }

  @Override
  @Async
  public Future<ModelResponse> getCurrentModel(String modelType, long ifModifiedSince) {

    ModelResponse response = new ModelResponse();

    // A valid model type must be provided.
    if (!ModelUtil.isSupported(modelType)) {
      response.setStatus(Status.BAD_REQUEST);
      response.setError(Optional.of("Specified model type = " + modelType + " not supported."));
      return new AsyncResult<>(response);
    }

    if (models.isEmpty()) {
      response.setStatus(Status.NOT_FOUND);
      return new AsyncResult<>(response);
    }

    ModelResource newest = null;
    long created = 0;
    for (ModelResource m : models.values()) {
      try {
        long time = XmlUtilities.xmlGregorianCalendar(m.getCreationTime()).toGregorianCalendar().getTimeInMillis();
        if (time > created) {
          newest = m;
          created = time;
        }
      } catch (DatatypeConfigurationException ex) {
        response.setStatus(Status.INTERNAL_SERVER_ERROR);
        response.setError(Optional.of("Error converting creationTime for model id = " + m.getId()));
        return new AsyncResult<>(response);
      }
    }

    if (newest == null && !models.isEmpty()) {
      response.setStatus(Status.NOT_MODIFIED);
      return new AsyncResult<>(response);
    }

    response.setModel(Optional.of(newest));
    return new AsyncResult<>(response);
  }

  @Override
  @Async
  public Future<ModelResponse> getCurrentModel(String modelType) {
    return this.getCurrentModel(modelType, 0);
  }

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

    if (Strings.isNullOrEmpty(deltaRequest.getId())) {
      response.setStatus(Status.BAD_REQUEST);
      response.setError(Optional.of("Delta id not provided = " + deltaRequest.getId()));
      return new AsyncResult<>(response);
    }

    DeltaResource delta = deltas.get(deltaRequest.getId());
    if (delta != null) {
      response.setStatus(Status.CONFLICT);
      response.setError(Optional.of("Delta id already exists = " + deltaRequest.getId()));
      return new AsyncResult<>(response);
    }

    ModelResource model = models.get(deltaRequest.getModelId());
    if (model == null) {
      response.setStatus(Status.NOT_FOUND);
      response.setError(Optional.of("Model id not found = " + deltaRequest.getModelId()));
      return new AsyncResult<>(response);
    }

    try {
      delta = new DeltaResource();
      delta.setId(deltaRequest.getId());
      delta.setModelId(deltaRequest.getModelId());
      delta.setAddition(deltaRequest.getAddition());
      delta.setReduction(deltaRequest.getReduction());
      delta.setResult("H4sIACKIo1cAA+1Y32/aMBB+56+I6NtUx0loVZEV1K6bpkm0m5Y+7NWNTbCa2JEdCP3vZydULYUEh0CnQXlC+O6+O/ u7X1ylgozp3BJ4LH3rcpJlqQ9hnud23rO5iKDnOA50XKgEgAwnJEEnQ8vuXC30eB5XqXnQuYDqfEl+LnGVvAv/3I6CVQiFv FbF7ff7UKF4Hiice2IZmgMml5RZ8sq/0n9p82hcWFCHCtjtQacHH5AkS5qJkNWa6rDUdD2Y8ZTHPHoqtDudy6lgvpLzGclyL h59zBNE2YBIW/0y7FhvPqjw8X5h5LS40TuUEPyDYTqjeIpi6/OKltZ5IDFnkbzn1gqmxMxO0DyiEUp5qoGfj4YVxiZIfqGYCh JmlDMU/+IiW7W7FIvPOCYDOWUMhOLcT5XG4AJ60PVjyh4HKPbk8NTIBmIxSIRXmpgT4EAXOqWVT4YmwgkNX9w4V wZeu1EddEDEjIZkE4YyktNMgbCoyhhTj2Z1vwVK3PoZ3Fz/DqyvN3ddTYoq49o3bd6mLCNCffFsgqeHwZH1sS04gxmQua 3fbI1I8YIkm5xDr3xCInXmVPPAAEqztAaqtwQFtHQ7vBzJiQmeeoAW5KxwxJisP57VrOuRF2xB1ZZ37M9ixIBALCLrSG9ct 0dI8fy74NN02CQ5Yq2WPaVkE5KqaQ5UIRRRnWinq+51huIpkVbXA2dO/6ztjTZhUUXRWEnYHVUPcwY2zaOafHh553fKz dcErXCLyuuYIlnpEBYo4quVVvtzezO6K2Fd0AMG/arMWuoBLQTcWnqZ9tcNOahhX679/8muNX27IrrgWWBRbZvESFjIsV LdXYhHOYcVVAlylKb6LrtjFEvStnY2Gi4ONAn2Mxh9dJr3mYi2bDjmRWHHXaYe7N+wZk0b2FTH2rHC+EJ28NL7Se9aVp Ry9ZwwyNTDa8Uf627LdXcfM8CWk/4BTQBblaMDjb92ND2C+Gun+yNsz6ZbcdPuLBSQfLvwJ1QggDPmF6Uo5IyVt+rnCq es+AaNt7cbsh/jaxtn//6GsWYDgAEdvHddkj8Wv/3/+bCDnW+rf2DW7Xx/ASQO0KQcHgAA");
      delta.setState(DeltaState.Accepted);
      delta.setLastModified(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis()).toXMLFormat());
      deltas.put(delta.getId(), delta);
      response.setStatus(Status.CREATED);
      response.setDelta(Optional.of(delta));
    } catch (DatatypeConfigurationException ex) {
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("Commit delta failed, message = " + ex.getLocalizedMessage()));
    }

    return new AsyncResult<>(response);
  }

  @Override
  @Async
  public Future<DeltaResponse> commitDelta(String deltaId) {
    DeltaResponse response = new DeltaResponse();

    if (Strings.isNullOrEmpty(deltaId)) {
      response.setStatus(Status.BAD_REQUEST);
      response.setError(Optional.of("Delta id not provided = " + deltaId));
      return new AsyncResult<>(response);
    }

    DeltaResource delta = deltas.get(deltaId);
    if (delta == null) {
      response.setStatus(Status.NOT_FOUND);
      response.setError(Optional.of("Delta not found, id = " + deltaId));
      return new AsyncResult<>(response);
    }

    if (delta.getState() == null || delta.getState() != DeltaState.Accepted) {
      log.info("[NsiDriver] requested delta not in Accepted state, id = {}, state = {}.",
              deltaId, delta.getState());
      response.setStatus(Status.CONFLICT);
      response.setError(Optional.of("requested delta not in Accepted state, id = " + deltaId
              + ", state = " + delta.getState()));
      return new AsyncResult<>(response);
    }

    try {
      // Update our internal delta state.
      delta.setState(DeltaState.Committed);
      delta.setLastModified(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis()).toXMLFormat());
      response.setStatus(Status.NO_CONTENT);
      response.setDelta(Optional.of(delta));
    } catch (DatatypeConfigurationException ex) {
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("Commit delta failed, message = " + ex.getLocalizedMessage()));
    }

    return new AsyncResult<>(response);
  }

  @Override
  @Async
  public Future<DeltaResponse> getDelta(String id, String modelType, long ifModifiedSince) {
    DeltaResponse response = new DeltaResponse();

    // A valid model type must be provided.
    if (!ModelUtil.isSupported(modelType)) {
      response.setStatus(Status.BAD_REQUEST);
      response.setError(Optional.of("Specified model type = " + modelType + " not supported."));
      return new AsyncResult<>(response);
    }

    DeltaResource delta = deltas.get(id);
    if (delta == null) {
      response.setStatus(Status.NOT_FOUND);
      return new AsyncResult<>(response);
    }

    try {
      long lastModified = XmlUtilities.xmlGregorianCalendar(delta.getLastModified())
                  .toGregorianCalendar().getTimeInMillis();
      if (lastModified <= ifModifiedSince) {
        response.setStatus(Status.NOT_MODIFIED);
        return new AsyncResult<>(response);
      }
    } catch (DatatypeConfigurationException ex) {
      response.setStatus(Status.INTERNAL_SERVER_ERROR);
      response.setError(Optional.of("Error converting creationTime for delta id = " + id));
      return new AsyncResult<>(response);
    }

    response.setDelta(Optional.of(delta));
    return new AsyncResult<>(response);
  }

  @Override
  @Async
  public Future<DeltaResponse> getDelta(String id, String modelType) {
    return this.getDelta(id, modelType, 0L);
  }

  @Override
  @Async
  public Future<DeltasResponse> getDeltas(String modelType, long ifModifiedSince) {
    DeltasResponse response = new DeltasResponse();

    // A valid model type must be provided.
    if (!ModelUtil.isSupported(modelType)) {
      response.setStatus(Status.BAD_REQUEST);
      response.setError(Optional.of("Specified model type = " + modelType + " not supported."));
      return new AsyncResult<>(response);
    }

    Collection<DeltaResource> results = new ArrayList<>();
    deltas.values().forEach((d) -> {
      try {
        log.info("[TestDriver]: {}", d.getId());
        long creationTime = XmlUtilities.xmlGregorianCalendar(d.getLastModified())
                .toGregorianCalendar().getTimeInMillis();
        if (creationTime > ifModifiedSince) {
          results.add(d);
        }
      } catch (DatatypeConfigurationException ex) {
        throw new InternalServerErrorException("Error converting creationTime for delta id = " + d.getId());
      }
    });

    if (results.isEmpty() && !deltas.isEmpty()) {
      response.setStatus(Status.NOT_FOUND);
      return new AsyncResult<>(response);
    }

    response.setDeltas(results);
    return new AsyncResult<>(response);
  }

  @Override
  @Async
  public Future<DeltasResponse> getDeltas(String modelType) {
      return this.getDeltas(modelType, 0L);
  }
}
