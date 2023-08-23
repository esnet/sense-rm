/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016 - 2019, The Regents
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
package net.es.sense.rm.measurements;

import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.measurements.db.MeasurementResource;
import net.es.sense.rm.measurements.db.MeasurementService;
import net.es.sense.rm.measurements.db.MeasurementType;
import net.es.sense.rm.measurements.db.MetricType;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class MeasurementController {
  private final MeasurementService measurementService;

  private static final int MAX_QUEUE_SIZE = 2000;

  /**
   * Constructor for bean injection.
   *
   * @param measurementService the measurement service used to store metrics.
   */
  public MeasurementController(MeasurementService measurementService) {
      this.measurementService = measurementService;
  }

  /**
   * Write an entry to the logging system.
   *
   * @param entry
   */
  private void logger(MeasurementResource entry) {
    log.info(entry.toString());
  }

  /**
   * Add a new MeasurementResource to the measurement queue.
   *
   * @param measurement
   * @param resource
   * @param type
   * @param value
   * @return new MeasurementResource with specified parameters.
   */
  public MeasurementResource add(MeasurementType measurement, String resource, MetricType type, String value) {
    MeasurementResource entry = new MeasurementResource();
    entry.setMeasurement(measurement);
    entry.setResource(resource);
    entry.setMtype(type);
    entry.setMvalue(value);
    return add(entry);
  }

  /**
   * Add a new MeasurementResource to the measurement queue.
   *
   * @param entry
   * @return new MeasurementResource with specified parameters.
   */
  public MeasurementResource add(MeasurementResource entry) {
    measurementService.store(entry);
    logger(entry);

    if (measurementService.size() > MAX_QUEUE_SIZE) {
      measurementService.prune();
    }

    return entry;
  }

  /**
   * The number of measurements in the queue.
   *
   * @return
   */
  public long size() {
    return measurementService.size();
  }

  /**
   * Get all measurements occurring after the specified time.
   *
   * @param lastModified return all measurement results occurring after the specified time.
   * @return
   */
  public Optional<MeasurementResults> get(long lastModified) {
    // No data is identified by empty result.
    if (measurementService.size() == 0) {
      return Optional.empty();
    }

    // We need to populate the last uuid and lastModified times.
    MeasurementResource last = measurementService.last();
    if (last == null) {
      return Optional.empty();
    }

    // Collect data needed to repond to query.
    return Optional.of(new MeasurementResults(last.getId(), last.getGenerated(),
            measurementService.get(lastModified)));
  }

  /**
   * Get all measurements occurring after the specified entry identified by the uuid.
   *
   * @param uuid return all measurement results occurring after the specified uuid.
   * @return
   */
  public Optional<MeasurementResults> get(String uuid) {
    // No data is identified by empty result.
    if (measurementService.size() == 0) {
      return Optional.empty();
    }

    // We need to populate the last uuid and lastModified times.
    MeasurementResource last = measurementService.last();
    if (last == null) {
      return Optional.empty();
    }

    // Results of our query goes here.
    Collection<MeasurementResource> entries;

    // Get the referenced entry.
    MeasurementResource entry = measurementService.get(uuid);

    // If we didn't find anything matching return everything, otherwise hust the newer entries.
    if (entry == null) {
      entries = measurementService.get();
    } else {
      entries = measurementService.get(entry.getGenerated());
    }

    return Optional.of(new MeasurementResults(last.getId(), last.getGenerated(), entries));
  }

  /**
   * Get all measurements.
   *
   * @return
   */
  public Optional<MeasurementResults> get() {
    // No data is identified by empty result.
    if (measurementService.size() == 0) {
      return Optional.empty();
    }

    // We need to populate the last uuid and lastModified times.
    MeasurementResource last = measurementService.last();
    if (last == null) {
      return Optional.empty();
    }

    // Collect data needed to repond to query.
    return Optional.of(new MeasurementResults(last.getId(), last.getGenerated(),
            measurementService.get()));
  }
}
