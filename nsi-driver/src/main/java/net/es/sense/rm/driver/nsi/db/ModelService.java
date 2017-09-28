package net.es.sense.rm.driver.nsi.db;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface ModelService {

  Model create(Model model);

  void delete(Model model);

  Collection<Model> get();

  Model get(long lastModified, String modelId);

  Collection<Model> get(long lastModified, boolean current, String topologyId);

  boolean isPresent(String topologyId, long version);

  Model update(Model model);

}
