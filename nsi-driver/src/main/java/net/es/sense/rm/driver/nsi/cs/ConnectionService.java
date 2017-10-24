package net.es.sense.rm.driver.nsi.cs;

import javax.jws.WebService;

/**
 *
 * @author hacksaw
 */
@WebService(serviceName = "ConnectionServiceRequester", portName = "ConnectionServiceRequesterPort", endpointInterface = "org.ogf.schemas.nsi._2013._12.connection.requester.ConnectionRequesterPort", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/requester", wsdlLocation = "WEB-INF/wsdl/ConnectionService/ogf_nsi_connection_requester_v2_0.wsdl")
public class ConnectionService {

  public void reserveConfirmed(java.lang.String connectionId, java.lang.String globalReservationId, java.lang.String description, org.ogf.schemas.nsi._2013._12.connection.types.ReservationConfirmCriteriaType criteria) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void reserveFailed(java.lang.String connectionId, org.ogf.schemas.nsi._2013._12.connection.types.ConnectionStatesType connectionStates, org.ogf.schemas.nsi._2013._12.framework.types.ServiceExceptionType serviceException) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void reserveCommitConfirmed(java.lang.String connectionId) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void reserveCommitFailed(java.lang.String connectionId, org.ogf.schemas.nsi._2013._12.connection.types.ConnectionStatesType connectionStates, org.ogf.schemas.nsi._2013._12.framework.types.ServiceExceptionType serviceException) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void reserveAbortConfirmed(java.lang.String connectionId) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void provisionConfirmed(java.lang.String connectionId) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void releaseConfirmed(java.lang.String connectionId) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void terminateConfirmed(java.lang.String connectionId) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void querySummaryConfirmed(java.util.List<org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryResultType> reservation, javax.xml.datatype.XMLGregorianCalendar lastModified) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void queryRecursiveConfirmed(java.util.List<org.ogf.schemas.nsi._2013._12.connection.types.QueryRecursiveResultType> reservation) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public org.ogf.schemas.nsi._2013._12.connection.types.GenericAcknowledgmentType queryNotificationConfirmed(org.ogf.schemas.nsi._2013._12.connection.types.QueryNotificationConfirmedType queryNotificationConfirmed) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void queryResultConfirmed(java.util.List<org.ogf.schemas.nsi._2013._12.connection.types.QueryResultResponseType> result) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void error(org.ogf.schemas.nsi._2013._12.framework.types.ServiceExceptionType serviceException) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void errorEvent(java.lang.String connectionId, long notificationId, javax.xml.datatype.XMLGregorianCalendar timeStamp, org.ogf.schemas.nsi._2013._12.connection.types.EventEnumType event, java.lang.String originatingConnectionId, java.lang.String originatingNSA, org.ogf.schemas.nsi._2013._12.framework.types.TypeValuePairListType additionalInfo, org.ogf.schemas.nsi._2013._12.framework.types.ServiceExceptionType serviceException) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void dataPlaneStateChange(java.lang.String connectionId, long notificationId, javax.xml.datatype.XMLGregorianCalendar timeStamp, org.ogf.schemas.nsi._2013._12.connection.types.DataPlaneStatusType dataPlaneStatus) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void reserveTimeout(java.lang.String connectionId, long notificationId, javax.xml.datatype.XMLGregorianCalendar timeStamp, int timeoutValue, java.lang.String originatingConnectionId, java.lang.String originatingNSA) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public void messageDeliveryTimeout(java.lang.String connectionId, long notificationId, javax.xml.datatype.XMLGregorianCalendar timeStamp, java.lang.String correlationId) throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}
