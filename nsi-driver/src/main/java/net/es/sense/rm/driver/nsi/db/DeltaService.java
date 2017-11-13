package net.es.sense.rm.driver.nsi.db;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface DeltaService {

  public Delta store(Delta delta);

  public void delete(Delta delta);

  public void delete(String id);

  public void deleteByDeltaId(String deltaId);

  public void delete();

  public Collection<Delta> get();

  public Delta get(long id);

  public Delta get(String deltaId, long lastModified);

  public Delta get(String deltaId, String modelId, long lastModified);

  public Collection<Delta> getNewer(long lastModified);

  public Delta get(String deltaId);
}
