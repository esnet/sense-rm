package net.es.sense.rm.driver.api;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import net.es.sense.rm.model.DeltaRequest;
import net.es.sense.rm.model.DeltaResource;
import net.es.sense.rm.model.ModelResource;
import org.springframework.scheduling.annotation.Async;

/**
 *
 * @author hacksaw
 */
public interface Driver {
  @Async
  public Future<ModelResource> getModel(String modelId, String modelType) throws ExecutionException;

  /**
   * Get a list of MRML models matching the specified query parameters.
   * 
   * @param current If <b>true</b> only the current model will be returned (if one exists).
   * 
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   * 
   * @return A Future promise to return a Collection of ModelResource matching query.  The
   *    returned collection may be empty if no models match the requested criteria.
   * 
   * @throws InternalServerErrorException An internal error caused a failure to process request.
   * 
   * @throws BadRequestException The query contains invalid parameters that halted processing.
   */
  @Async
  public Future<Collection<ModelResource>> getModels(boolean current, String modelType) 
          throws BadRequestException, InternalServerErrorException;

  @Async
  public Future<DeltaResource> propagateDelta(DeltaRequest delta, String modelType)
          throws ExecutionException, NotFoundException, InternalServerErrorException;

  @Async
  public Future<DeltaResource> commitDelta(String deltaId) throws ExecutionException, NotFoundException, TimeoutException ;

  @Async
  public Future<DeltaResource> getDelta(String deltaId, long lastModified, String modelType) throws ExecutionException;

  @Async
  public Future<Collection<DeltaResource>> getDeltas(long lastModified, String modelType) throws ExecutionException;
}

