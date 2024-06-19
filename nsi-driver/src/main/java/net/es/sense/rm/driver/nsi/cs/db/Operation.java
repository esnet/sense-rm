package net.es.sense.rm.driver.nsi.cs.db;

import lombok.Synchronized;
import org.ogf.schemas.nsi._2013._12.framework.types.ServiceExceptionType;

import java.io.Serializable;
import java.util.concurrent.Semaphore;

/**
 * This class is used to track the status of an individual NSI operation,
 * providing a blocking semaphore allowing the delta request thread
 * initiating an NSI request to block on a result returned via an NSI
 * ConnectionService callback API thread.
 *
 * If an error is encountered within the NSI ConnectionService callback API
 * thread the state will be set to "failed" and the service exception will
 * be provided describing the error.
 *
 * @author hacksaw
 */
@lombok.Builder
public class Operation implements Serializable {
  private final Semaphore completed = new Semaphore(0);
  private String uniqueId;
  private String correlationId;
  private OperationType operation;
  private StateType state;
  private ServiceExceptionType exception;

  public Semaphore getCompleted() {
    return completed;
  }

  /**
   * @return the operation
   */
  @Synchronized
  public OperationType getOperation() {
    return operation;
  }

  /**
   * @param operation the operation to set
   */
  @Synchronized
  public void setOperation(OperationType operation) {
    this.operation = operation;
  }

  /**
   * @return the correlationId
   */
  @Synchronized
  public String getCorrelationId() {
    return correlationId;
  }

  /**
   * @param correlationId the correlationId to set
   */
  @Synchronized
  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  /**
   * @return the uniqueId
   */
  @Synchronized
  public String getUniqueId() {
    return uniqueId;
  }

  /**
   * @param uniqueId the uniqueId that correlates related structures.
   */
  @Synchronized
  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  /**
   * @return the state
   */
  @Synchronized
  public StateType getState() {
    return state;
  }

  /**
   * @param state the state to set
   */
  @Synchronized
  public void setState(StateType state) {
    this.state = state;
  }

  /**
   * @return the exception
   */
  @Synchronized
  public ServiceExceptionType getException() {
    return exception;
  }

  /**
   * @param exception the exception to set
   */
  @Synchronized
  public void setException(ServiceExceptionType exception) {
    this.exception = exception;
  }
}
