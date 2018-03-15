package net.es.sense.rm.driver.api;

import java.util.concurrent.Future;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import net.es.sense.rm.model.DeltaRequest;
import org.springframework.scheduling.annotation.Async;

/**
 *
 * @author hacksaw
 */
public interface Driver {
/**@Async
  public Future<Void> start() throws Exception;

  @Async
  public Future<Void> stop() throws Exception;
**/
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
  @Async
  public Future<ModelResponse> getModel(String id, String modelType, long ifModifiedSince);

  /**
   * Get a specific MRML model identified by <b>id</b>.
   *
   * @param id The model identifier to return.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).

   * @return A Future promise to return a ModelResource matching id if one exists.
   */
  @Async
  public Future<ModelResponse> getModel(String id, String modelType);

  /**
   * Get the current MRML model.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @param ifModifiedSince Return model only if newer than this date, otherwise throw NotModifiedException.
   *
   * @return A Future promise to return a Collection of ModelResource matching query.  The
   *    returned collection may be empty if no models match the requested criteria.
   *
   * @throws InternalServerErrorException An internal error caused a failure to process request.
   *
   * @throws BadRequestException The query contains invalid parameters that halted processing.
   */
  @Async
  public Future<ModelResponse> getCurrentModel(String modelType, long ifModifiedSince);

  @Async
  public Future<ModelResponse> getCurrentModel(String modelType);

  /**
   * Get a list of MRML models.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @param ifModifiedSince Return model only if newer than this date, otherwise throw NotModifiedException.
   *
   * @return A Future promise to return a Collection of ModelResource matching query.  The
   *    returned collection may be empty if no models match the requested criteria.
   *
   * @throws InternalServerErrorException An internal error caused a failure to process request.
   *
   * @throws BadRequestException The query contains invalid parameters that halted processing.
   */
  @Async
  public Future<ModelsResponse> getModels(String modelType, long ifModifiedSince);

  @Async
  public Future<ModelsResponse> getModels(String modelType);

  @Async
  public Future<DeltaResponse> propagateDelta(DeltaRequest delta, String modelType);

  @Async
  public Future<DeltaResponse> commitDelta(String deltaId);

  /**
   * Get a specific delta resource identified by <b>deltaId</b>.
   *
   * @param deltaId The delta identifier to return.
   *
   * @param modelType Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   *
   * @param ifModifiedSince Return delta only if newer than this date, otherwise throw NotModifiedException.
   *
   * @return A Future promise to return a DeltaResource matching id if one exists.
   *
   * @throws InternalServerErrorException An internal error caused a failure to process request.
   *
   * @throws BadRequestException The query contains invalid parameters that halted processing.
   *
   * @throws NotFoundException The requested delta id could not be found.
   *
   * @throws NotModifiedException The target delta resource exists but was not modified.
   */
  @Async
  public Future<DeltaResponse> getDelta(String deltaId, String modelType, long ifModifiedSince);

  @Async
  public Future<DeltaResponse> getDelta(String deltaId, String modelType);

  /**
   *
   * @param modelType
   * @param ifModifiedSince
   * @return
   * @throws BadRequestException
   * @throws InternalServerErrorException
   * @throws NotModifiedException
   */
  @Async
  public Future<DeltasResponse> getDeltas(String modelType, long ifModifiedSince);

  @Async
  public Future<DeltasResponse> getDeltas(String modelType);
}

