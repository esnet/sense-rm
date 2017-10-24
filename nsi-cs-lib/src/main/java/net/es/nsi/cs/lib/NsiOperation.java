package net.es.nsi.cs.lib;

import org.ogf.schemas.nsi._2013._12.connection.types.GenericRequestType;

/**
 *
 * @author hacksaw
 */
public class NsiOperation {

  private String connectionId;
  private String globalReservationId;

  public GenericRequestType getGenericRequestType() {
    GenericRequestType requestType = new GenericRequestType();
    requestType.setConnectionId(getConnectionId());
    return requestType;
  }

  /**
   * @return the connectionId
   */
  public String getConnectionId() {
    return connectionId;
  }

  /**
   * @param connectionId the connectionId to set
   */
  public void setConnectionId(String connectionId) {
    this.connectionId = connectionId;
  }

  /**
   * @return the globalReservationId
   */
  public String getGlobalReservationId() {
    return globalReservationId;
  }

  /**
   * @param globalReservationId the globalReservationId to set
   */
  public void setGlobalReservationId(String globalReservationId) {
    this.globalReservationId = globalReservationId;
  }
}
