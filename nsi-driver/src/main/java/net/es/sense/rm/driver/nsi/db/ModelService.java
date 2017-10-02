package net.es.sense.rm.driver.nsi.db;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface ModelService {

  Model create(Model model);

  void delete(Model model);

  void delete(String id);

  void delete();

  Collection<Model> get();

  Model get(long lastModified, String modelId);

  Model get(String modelId);

  Collection<Model> get(long lastModified, boolean current, String topologyId);

  Collection<Model> get(boolean current, String topologyId);

  boolean isPresent(String topologyId, long version);

  Model update(Model model);

}
