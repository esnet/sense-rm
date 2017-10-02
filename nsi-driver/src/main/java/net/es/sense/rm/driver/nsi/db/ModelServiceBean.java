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
    if (current) {
      Model result = modelRepository.findCurrentModelForTopologyId(topologyId);
      return (result.getVersion() > lastModified) ? Lists.newArrayList(result) : new ArrayList<>();
    }

    return Lists.newArrayList(modelRepository.findTopologyIdNewerThanVersion(topologyId, lastModified));
  }

  @Override
  public Collection<Model> get(boolean current, String topologyId) {
    return Lists.newArrayList(modelRepository.findCurrentModelForTopologyId(topologyId));
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
