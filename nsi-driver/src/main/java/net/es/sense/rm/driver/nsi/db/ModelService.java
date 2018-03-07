package net.es.sense.rm.driver.nsi.db;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface ModelService {

  public Model create(Model model);

  public void delete(Model model);

  public void delete(long idx);

  public void delete(String modelId);

  public void delete();

  public Collection<Model> get();

  public Model get(long idx);

  public Model getByModelId(String modelId, long lastModified);

  public boolean isPresent(String topologyId, long version);

  public Model update(Model model);

  // Used by nsi-driver API to implement driver interface.
  public Model getByModelId(String modelId);
  public Model getCurrent(String topologyId);
  public Collection<Model> getByTopologyId(String topologyId, long lastModified);
  public long countByTopologyId(String topologyId);
}
