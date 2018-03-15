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

import java.util.Collection;

/**
 * A interface definition for accessing delta objects in storage.
 *
 * @author hacksaw
 */
public interface DeltaService {

  // Storage management.
  public Delta store(Delta delta);

  public void delete(Delta delta);

  public void delete(long idx);

  public void deleteByDeltaId(String deltaId);

  public void delete();

  // Querying.

  /**
   * Get all delta resources stored in the database.
   *
   * @return A collection of delta resources.
   */
  public Collection<Delta> get();

  /**
   * Get a delta resources based on index key.
   * @param idx Index key for the delta resource.
   * @return The matching delta resources if it exists.
   */
  public Delta get(long idx);

  /**
   * Get a delta resource based on delta identifier.
   * @param deltaId Delta identifier.
   * @return The matching delta resources if it exists.
   */
  public Delta get(String deltaId);

  /**
   *
   * @param deltaId
   * @param lastModified
   * @return
   */
  public Delta get(String deltaId, long lastModified);

  /**
   *
   * @param deltaId
   * @param modelId
   * @param lastModified
   * @return
   */
  public Delta get(String deltaId, String modelId, long lastModified);

  /**
   *
   * @param lastModified
   * @return
   */
  public Collection<Delta> getNewer(long lastModified);

  /**
   *
   * @return
   */
  public long count();
}
