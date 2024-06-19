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
package net.es.sense.rm.driver.nsi.cs.db;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * This CrudRepository class handles storage for the ConnectionMap structure.
 *
 * @author hacksaw
 */
@Repository
public interface ConnectionMapRepository extends CrudRepository<ConnectionMap, Long> {

  public void deleteByUniqueId(@Param("uniqueId") String uniqueId);
  public ConnectionMap findOneById(@Param("id") long id);
  public ConnectionMap findByUniqueId(@Param("uid") String uid);
  public Iterable<ConnectionMap> findByDeltaId(@Param("deltaId") String deltaId);
  public Iterable<ConnectionMap> findBySwitchingSubnetId(@Param("switchingSubnetId") String switchingSubnetId);
  public Iterable<ConnectionMap> findByDeltaIdAndSwitchingSubnetId(
          @Param("deltaId") String deltaId,
          @Param("switchingSubnetId") String switchingSubnetId);
}
