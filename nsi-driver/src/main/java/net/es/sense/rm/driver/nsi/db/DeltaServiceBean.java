package net.es.sense.rm.driver.nsi.db;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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
public class DeltaServiceBean implements DeltaService {

  @Autowired
  private DeltaRepository deltaRepository;

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public Delta store(Delta delta) {
    if (Strings.isNullOrEmpty(delta.getDeltaId()) || Strings.isNullOrEmpty(delta.getModelId())) {
      log.error("[DeltaServiceBean] Delta contains a null/empty require field {}", delta);
      return null;
    }

    return deltaRepository.save(delta);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(Delta delta) {
    deltaRepository.delete(delta);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(long idx) {
    deltaRepository.deleteByIdx(idx);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void deleteByDeltaId(String deltaId) {
    deltaRepository.deleteByDeltaId(deltaId);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete() {
    for (Delta delta : deltaRepository.findAll()) {
      deltaRepository.delete(delta);
    }
  }

  @Override
  public Collection<Delta> get() {
    return Lists.newArrayList(deltaRepository.findAll());
  }

  @Override
  public Delta get(long idx) {
    return deltaRepository.findByIdx(idx);
  }

  @Override
  public Collection<Delta> getNewer(long lastModified) {
    return Lists.newArrayList(deltaRepository.findAllNewer(lastModified));
  }

  @Override
  public Delta get(String deltaId, long lastModified) {
    return deltaRepository.findByDeltaId(deltaId, lastModified);
  }

  @Override
  public Delta get(String deltaId, String modelId, long lastModified) {
    return deltaRepository.findByDeltaIdAndModelId(deltaId, modelId, lastModified);
  }

  @Override
  public Delta get(String deltaId) {
    return deltaRepository.findByDeltaId(deltaId);
  }

  @Override
  public long count() {
    return deltaRepository.count();
  }
}
