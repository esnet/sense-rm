package net.es.sense.rm.driver.nsi.db;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface DeltaService {

  // Storage management.
  public Delta store(Delta delta);

  public void delete(Delta delta);

  public void delete(long idx);

  public void deleteByDeltaId(String deltaId);

  public void delete();

  // Querying.

  /**
   * Get all delta resources stored in the database.
   * @return A collection of delta resources.
   */
  public Collection<Delta> get();

  /**
   * Get a delta resources based on index key.
   * @param idx Index key for the delta resource.
   * @return The matching delta resources if it exists.
   */
  public Delta get(long idx);

  /**
   * Get a delta resource based on delta identifier.
   * @param deltaId Delta identifier.
   * @return The matching delta resources if it exists.
   */
  public Delta get(String deltaId);

  /**
   *
   * @param deltaId
   * @param lastModified
   * @return
   */
  public Delta get(String deltaId, long lastModified);

  /**
   *
   * @param deltaId
   * @param modelId
   * @param lastModified
   * @return
   */
  public Delta get(String deltaId, String modelId, long lastModified);

  /**
   *
   * @param lastModified
   * @return
   */
  public Collection<Delta> getNewer(long lastModified);

  /**
   *
   * @return
   */
  public long count();
}
