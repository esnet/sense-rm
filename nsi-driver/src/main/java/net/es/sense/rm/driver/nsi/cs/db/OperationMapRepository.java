package net.es.sense.rm.driver.nsi.cs.db;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * Manages NSI operation progress between the Delta processing threads and
 * the NSI ConnectionService callback thread through the use of a semaphore.
 *
 * This uses in-memory storage and assumes a restart of the SENSE-N-RM will
 * clear the outstanding operations since both delta and NSI requests will
 * time out across the restart.
 *
 * @author hacksaw
 */
@Slf4j
@Repository
public class OperationMapRepository {

  private final Map<String, Operation> map = new ConcurrentHashMap<>();

  public Operation store(Operation operation) {
    return map.put(operation.getCorrelationId(), operation);
  }

  public Operation get(String correlationId) {
    return map.get(correlationId);
  }

  public Operation delete(String correlationId) {
    return map.remove(correlationId);
  }

  public void delete(List<String> correlationIds) {
    correlationIds.forEach((correlationId) -> {
      map.remove(correlationId);
    });
  }

  public boolean wait(String correlationId) {
    Operation op = get(correlationId);

    if (op == null) {
      return false;
    }

    try {
      return op.getCompleted().tryAcquire(120, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      log.error("[OperationMapRepository] Interupted so giving up", ex);
      return false;
    }
  }

  public boolean acknowledge(String correlationId, StateType state) {
    Operation op = get(correlationId);
    if (op == null) {
      log.error("[OperationMapRepository] acknowledge failed!  Could not find correlationId = {}", correlationId);
      return false;
    }

    op.setState(state);
    op.getCompleted().release();
    return true;
  }
}
