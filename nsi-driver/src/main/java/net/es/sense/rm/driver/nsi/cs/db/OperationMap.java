package net.es.sense.rm.driver.nsi.cs.db;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class OperationMap {

  private final Map<String, Operation> map = new ConcurrentHashMap<>();

  public Operation put(String correlationId, Operation operation) {
    return map.put(correlationId, operation);
  }

  public Operation get(String correlationId) {
    return map.get(correlationId);
  }

  public Operation remove(String correlationId) {
    return map.remove(correlationId);
  }

  public void removeAll(List<String> correlationIds) {
    correlationIds.forEach((correlationId) -> {
      map.remove(correlationId);
    });
  }

  public boolean wait(String correlationId) {
    Operation op = map.get(correlationId);

    if (op == null) {
      return false;
    }

    try {
      return op.getCompleted().tryAcquire(60, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      log.error("[OperationMap] Interupted so giving up", ex);
      return false;
    }
  }

  public boolean acknowledge(String correlationId, StateType state) {
    Operation op = map.get(correlationId);
    if (op == null) {
      log.error("[OperationMap] acknowledge failed!  Could not find correlationId = {}", correlationId);
      return false;
    }

    op.setState(state);
    op.getCompleted().release();
    return true;
  }
}
