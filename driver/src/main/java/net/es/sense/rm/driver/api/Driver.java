package net.es.sense.rm.driver.api;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
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

  @Async
  public Future<Collection<ModelResource>> getModels(boolean current, String modelType) throws ExecutionException;

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

