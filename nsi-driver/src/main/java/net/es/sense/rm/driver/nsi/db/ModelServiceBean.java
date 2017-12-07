package net.es.sense.rm.driver.nsi.db;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Service
@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
public class ModelServiceBean implements ModelService {

  @Autowired
  private ModelRepository modelRepository;

  @Override
  public Collection<Model> get() {
    return Lists.newArrayList(modelRepository.findAll());
  }

  @Override
  public Model get(long lastModified, String modelId) {
    return modelRepository.findByModelIdAndVersion(modelId, lastModified);
  }

  @Override
  public Model get(String modelId) {
    return modelRepository.findByModelId(modelId);
  }

  @Override
  public Collection<Model> get(long lastModified, boolean current, String topologyId) {
    Collection<Model> result = new ArrayList();

    if (current) {
      Model findCurrentModelForTopologyId = modelRepository.findCurrentModelForTopologyId(topologyId);
      if (findCurrentModelForTopologyId != null &&
            findCurrentModelForTopologyId.getVersion() > lastModified) {
          result.add(findCurrentModelForTopologyId);
      }
    }
    else {
      Iterable<Model> findByTopologyId = modelRepository.findTopologyIdNewerThanVersion(topologyId, lastModified);
      findByTopologyId.forEach(m -> {
        result.add(m);
      });
    }

    return result;
  }

  @Override
  public Collection<Model> get(boolean current, String topologyId) {
    Collection<Model> result = new ArrayList();

    log.info("[ModelServiceBean] current = {}, topologyId = {}", current, topologyId);

    if (current) {
      Model currentModel = modelRepository.findCurrentModelForTopologyId(topologyId);
      if (currentModel != null) {
        result.add(currentModel);
        log.info("[ModelServiceBean] found topologyId = {}, modelId = {}",
                currentModel.getTopologyId(), currentModel.getModelId());
      }
      else {
        log.info("[ModelServiceBean] failed to find current {}, topologyId {}", current, topologyId);
      }
    }
    else {
      Iterable<Model> findByTopologyId = modelRepository.findByTopologyId(topologyId);
      findByTopologyId.forEach(m -> {
        result.add(m);
      });
    }

    return result;
  }

  @Override
  public boolean isPresent(String topologyId, long version) {
    return modelRepository.isVersion(topologyId, version);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public Model create(Model model) {
    if (Strings.isNullOrEmpty(model.getTopologyId()) || Strings.isNullOrEmpty(model.getModelId())) {
      return null;
    }
    return modelRepository.save(model);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public Model update(Model model) {
    Model findOne = modelRepository.findOne(model.getModelId());
    if (findOne == null) {
      return null;
    }
    return modelRepository.save(model);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(Model model) {
    modelRepository.delete(model);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(String id) {
    modelRepository.delete(id);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete() {
    for (Model model : modelRepository.findAll()) {
      modelRepository.delete(model);
    }
  }
}
