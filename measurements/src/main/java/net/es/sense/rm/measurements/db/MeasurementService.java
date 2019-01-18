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

/**
 *
 * @author hacksaw
 */
public interface MeasurementService {

  public long size();

  public Collection<MeasurementResource> prune();

  public Collection<MeasurementResource> get(long lastModified);

  public MeasurementResource get(String id);

  public Collection<MeasurementResource> get();

  public MeasurementResource first();

  public MeasurementResource last();

  public MeasurementResource store(MeasurementResource measurement);

  public void delete(MeasurementResource measurement);

  public void delete(String id);

  public void delete();

}