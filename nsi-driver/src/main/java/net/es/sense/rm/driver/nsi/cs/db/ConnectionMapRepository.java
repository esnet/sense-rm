package net.es.sense.rm.driver.nsi.cs.db;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author hacksaw
 */
@Repository
public interface ConnectionMapRepository extends CrudRepository<ConnectionMap, Long> {

  public ConnectionMap findByGlobalReservationId(@Param("globalReservationId") String globalReservationId);
  public ConnectionMap findBySwitchingSubnetId(@Param("switchingSubnetId") String switchingSubnetId);

}
