/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.sense.rm.driver.nsi.db;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Service
@Transactional(propagation=Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE, readOnly=true)
public class ModelServiceBean implements ModelService {

  @Autowired
  private ModelRepository modelRepository;

  @Override
  public Collection<Model> get() {
    return Lists.newArrayList(modelRepository.findAll());
  }

  @Override
  public Model get(long idx) {
    return modelRepository.findByIdx(idx);
  }

  @Override
  public boolean isPresent(String topologyId, String version) {
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
    Model findOne = modelRepository.findByIdx(model.getIdx());
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
  public void delete(String modelId) {
    modelRepository.deleteByModelId(modelId);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(long idx) {
    modelRepository.deleteByIdx(idx);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete() {
    for (Model model : modelRepository.findAll()) {
      modelRepository.delete(model);
    }
  }

  // Used by nsi-driver API to implement driver interface.
  @Override
  public Model getCurrent(String topologyId) {
    log.info("[ModelServiceBean] getCurrent() topologyId = {}", topologyId);

    return modelRepository.findCurrentModelForTopologyId(topologyId);
  }

  @Override
  public Model getByModelId(String modelId) {
    return modelRepository.findByModelId(modelId);
  }

  @Override
  public Collection<Model> getByTopologyId(String topologyId) {
    Collection<Model> result = new ArrayList();
    Iterable<Model> list = modelRepository.findByTopologyId(topologyId);
    list.forEach(m -> {
      result.add(m);
    });

    return result;
  }

  @Override
  public Collection<Model> getByTopologyId(String topologyId, long created) {
    Collection<Model> result = new ArrayList();

    Iterable<Model> findByTopologyId = modelRepository.findTopologyIdNewerThanCreated(topologyId, created);
    findByTopologyId.forEach(m -> {
      result.add(m);
    });

    return result;
  }

  @Override
  public long countByTopologyId(String topologyId) {
    Long count = modelRepository.countByTopologyId(topologyId);
    return count == null ? 0L : count;
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void purge(String topologyId, int size) {
    Pageable newest = PageRequest.of(0, size, Sort.by(Direction.DESC, "created"));
    Page<Model> top = modelRepository.findByTopologyId(topologyId, newest);
    List<Long> list = top.map((m)-> m.getCreated()).getContent();
    Long last = list.get(list.size() - 1);
    modelRepository.deleteByTopologyIdAndLessThanCreated(topologyId, last);
  }
}
