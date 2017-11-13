package net.es.sense.rm.driver.nsi.cs.db;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface OperationService {

  public Operation store(Operation operation);

  public void delete(Operation operation);

  public void delete(long id);

  public void delete();

  public Collection<Operation> get();

  public Collection<Operation> getByConnectionId(String connectionId);

  public Collection<Operation> getByGlobalReservationId(String globalReservationId);

  public Operation getByCorrelationId(String connectionId);

  public Operation get(long id);

  public Collection<Operation> get(String connectionId);

}