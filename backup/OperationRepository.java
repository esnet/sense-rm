package net.es.sense.rm.driver.nsi.cs.db;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author hacksaw
 */
@Repository
public interface OperationRepository extends CrudRepository<Operation, Long> {

  public Iterable<Operation> findByGlobalReservationId(String globalReservationId);
  public Iterable<Operation> findByConnectionId(String connectionId);
  public Operation findByCorrelationId(String CorrelationId);

}
