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

import net.es.sense.rm.measurements.db.MeasurementResource;

import java.util.Collection;
import java.util.Optional;

/**
 * Holder for a set of measurement results.
 *
 * @author hacksaw
 */
public class MeasurementResults {
  private final Optional<String> lastUuid;
  private final long lastModifiedTime;
  private final Collection<MeasurementResource> queue;

  public MeasurementResults(String lastUuid, long lastModifiedTime, Collection<MeasurementResource> queue) {
    this.lastUuid = Optional.ofNullable(lastUuid);
    this.lastModifiedTime = lastModifiedTime;
    this.queue = queue;
  }

  public String getUuid() {
    return lastUuid.orElse(null);
  }

  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  public Collection<MeasurementResource> getQueue() {
    return queue;
  }
}
