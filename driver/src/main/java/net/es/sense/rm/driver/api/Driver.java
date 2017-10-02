package net.es.sense.rm.driver.api;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;

/**
 *
 * @author hacksaw
 */
public interface Driver {
  @Async
  public Future<Model> getModel(String modelType, String id) throws ExecutionException;

  @Async
  public Future<Collection<Model>> getModels(boolean current, String modelType) throws ExecutionException;

  @Async
  public Future<Delta> propagateDelta(DeltaRequest delta) throws ExecutionException;

  @Async
  public Future<Delta> commitDelta(String id) throws ExecutionException;

  @Async
  public Future<Delta> getDelta(long lastModified, String id) throws ExecutionException;

  @Async
  public Future<Collection<Delta>> getDeltas(long lastModified, String modelType) throws ExecutionException;
}
