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
package net.es.sense.rm.measurements.db;

import java.util.Collection;
import java.util.Optional;
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
public class MeasurementServiceBean implements MeasurementService {

  @Autowired
  private MeasurementRepository measurementRepository;

  @Override
  public long size() {
    return measurementRepository.count();
  }

  @Override
  public Collection<MeasurementResource> prune() {
    Collection<MeasurementResource> list = measurementRepository.findLast10ByOrderByGeneratedAsc();
    if (list != null) {
      list.forEach((m) -> {
        try {
          measurementRepository.delete(m);
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
          // Ignore this.
        }
      });
    }

    return list;
  }

  @Override
  public MeasurementResource last() {
    Collection<MeasurementResource> results = measurementRepository.findFirst1ByOrderByGeneratedDesc();
    if (results != null) {
      Optional<MeasurementResource> findFirst = results.stream().findFirst();
      if (findFirst.isPresent()) {
        return findFirst.get();
      }
    }
    return null;
  }

  @Override
  public MeasurementResource first() {
    Collection<MeasurementResource> results = measurementRepository.findFirst1ByOrderByGeneratedAsc();
    if (results != null) {
      Optional<MeasurementResource> findFirst = results.stream().findFirst();
      if (findFirst.isPresent()) {
        return findFirst.get();
      }
    }
    return null;
  }

  @Override
  public Collection<MeasurementResource> get(long lastModified) {
    return measurementRepository.findAllNewer(lastModified);
  }

  @Override
  public MeasurementResource get(String id) {
    return measurementRepository.findOneById(id);
  }

  @Override
  public Collection<MeasurementResource> get() {
    return measurementRepository.findAllByOrderByGeneratedAsc();
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public MeasurementResource store(MeasurementResource measurement) {
    return measurementRepository.save(measurement);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(MeasurementResource measurement) {
    measurementRepository.delete(measurement);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(String id) {
    measurementRepository.deleteById(id);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete() {
    measurementRepository.deleteAll();
  }
}
