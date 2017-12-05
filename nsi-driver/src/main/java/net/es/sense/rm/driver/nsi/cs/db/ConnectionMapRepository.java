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

  public ConnectionMap findByDescription(@Param("description") String description);
  public Iterable<ConnectionMap> findByDeltaId(@Param("deltaId") String deltaId);
  public Iterable<ConnectionMap> findBySwitchingSubnetId(@Param("switchingSubnetId") String switchingSubnetId);
  public Iterable<ConnectionMap> findByDeltaIdAndSwitchingSubnetId(
          @Param("deltaId") String deltaId,
          @Param("switchingSubnetId") String switchingSubnetId);

}
