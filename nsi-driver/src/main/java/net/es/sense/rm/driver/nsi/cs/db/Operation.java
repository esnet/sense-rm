package net.es.sense.rm.driver.nsi.cs.db;

import java.io.Serializable;
import java.util.concurrent.Semaphore;
import lombok.Synchronized;
import org.ogf.schemas.nsi._2013._12.framework.types.ServiceExceptionType;

/**
 *
 * @author hacksaw
 */
public class Operation implements Serializable {
  private final Semaphore completed = new Semaphore(0);
  private String correlationId;
  private StateType state;
  private ServiceExceptionType exception;

  public Semaphore getCompleted() {
    return completed;
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
  public ServiceExceptionType getException() {
    return exception;
  }

  /**
   * @param exception the exception to set
   */
  public void setException(ServiceExceptionType exception) {
    this.exception = exception;
  }
}
