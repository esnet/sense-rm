package net.es.sense.rm.driver.nsi.db;

import com.google.common.collect.Lists;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
@Transactional
public class ModelReader {

  @Autowired
  private ModelRepository modelRepository;

  public Model getModel(long lastModified, String modelId) {
    return modelRepository.findByModelIdAndVersion(modelId, lastModified);
  }

  public Collection<Model> getModels(long lastModified, boolean current, String topologyId) {
    if (current) {
      return Lists.newArrayList(modelRepository.findCurrentModelForTopologyId(topologyId));
    }

    return Lists.newArrayList(modelRepository.findTopologyIdNewerThanVersion(topologyId, lastModified));
  }

}
