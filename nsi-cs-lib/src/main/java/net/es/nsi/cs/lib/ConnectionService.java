package net.es.nsi.cs.lib;

import java.util.List;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import lombok.extern.slf4j.Slf4j;
import org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.DataPlaneStateChangeRequestType;
import org.ogf.schemas.nsi._2013._12.connection.types.ErrorEventType;
import org.ogf.schemas.nsi._2013._12.connection.types.GenericAcknowledgmentType;
import org.ogf.schemas.nsi._2013._12.connection.types.GenericConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.GenericErrorType;
import org.ogf.schemas.nsi._2013._12.connection.types.GenericFailedType;
import org.ogf.schemas.nsi._2013._12.connection.types.MessageDeliveryTimeoutRequestType;
import org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory;
import org.ogf.schemas.nsi._2013._12.connection.types.QueryNotificationConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.QueryRecursiveConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.QueryResultConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryResultType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveTimeoutRequestType;
import org.ogf.schemas.nsi._2013._12.framework.headers.CommonHeaderType;

/**
 *
 * @author hacksaw
 */
@Slf4j
@WebService(serviceName = "ConnectionServiceRequester", portName = "ConnectionServiceRequesterPort", endpointInterface = "org.ogf.schemas.nsi._2013._12.connection.requester.ConnectionRequesterPort", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/requester", wsdlLocation = "WEB-INF/wsdl/ConnectionService/ogf_nsi_connection_requester_v2_0.wsdl")
public class ConnectionService {
  private final static ObjectFactory FACTORY = new ObjectFactory();

  public GenericAcknowledgmentType reserveConfirmed(ReserveConfirmedType reserveConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    log.info("[ConnectionService] reserveConfirmed: {}", reserveConfirmed.getConnectionId());
    CommonHeaderType value = header.value;
    log.info("[ConnectionService] provider NSA = {}", value.getProviderNSA());
    value.setProviderNSA("EAT SHIT");
    return FACTORY.createGenericAcknowledgmentType();
  }

  public GenericAcknowledgmentType reserveFailed(GenericFailedType reserveFailed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType reserveCommitConfirmed(GenericConfirmedType reserveCommitConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType reserveCommitFailed(GenericFailedType reserveCommitFailed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType reserveAbortConfirmed(GenericConfirmedType reserveAbortConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType provisionConfirmed(GenericConfirmedType provisionConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType releaseConfirmed(GenericConfirmedType releaseConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType terminateConfirmed(GenericConfirmedType parameters, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType querySummaryConfirmed(QuerySummaryConfirmedType querySummaryConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    List<QuerySummaryResultType> reservations = querySummaryConfirmed.getReservation();
    log.info("[ConnectionService] querySummaryConfirmed: {}", reservations.size());
    for (QuerySummaryResultType reservation : reservations) {
      log.info("[ConnectionService] querySummaryConfirmed: connectionId = {}, {}", reservation.getConnectionId(), reservation.getConnectionStates().getReservationState());
    }
    CommonHeaderType value = header.value;
    log.info("[ConnectionService] provider NSA = {}", value.getProviderNSA());
    value.setProviderNSA("I got the querySummaryConfirmed");
    return FACTORY.createGenericAcknowledgmentType();
  }

  public GenericAcknowledgmentType queryRecursiveConfirmed(QueryRecursiveConfirmedType queryRecursiveConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    throw new UnsupportedOperationException("queryRecursiveConfirmed: Not implemented yet.");
    /**
    //TODO implement this method
    List<QueryRecursiveResultType> reservations = queryRecursiveConfirmed.getReservation();
    log.info("[ConnectionService] queryRecursiveConfirmed: {}", reservations.size());
    for (QueryRecursiveResultType reservation : reservations) {
      log.info("[ConnectionService] queryRecursiveConfirmed: connectionId = {}, {}", reservation.getConnectionId(), reservation.getConnectionStates().getReservationState());
    }
    CommonHeaderType value = header.value;
    log.info("[ConnectionService] provider NSA = {}", value.getProviderNSA());
    value.setProviderNSA("I got the queryRecursiveConfirmed");
    return FACTORY.createGenericAcknowledgmentType(); **/
  }

  public GenericAcknowledgmentType queryNotificationConfirmed(QueryNotificationConfirmedType queryNotificationConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType queryResultConfirmed(QueryResultConfirmedType queryResultConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    log.info("[ConnectionService] queryResultConfirmed: {}", queryResultConfirmed.getResult().size());
    CommonHeaderType value = header.value;
    log.info("[ConnectionService] provider NSA = {}", value.getProviderNSA());
    value.setProviderNSA("EAT SHIT");
    return FACTORY.createGenericAcknowledgmentType();
  }

  public GenericAcknowledgmentType error(GenericErrorType error, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType errorEvent(ErrorEventType errorEvent, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType dataPlaneStateChange(DataPlaneStateChangeRequestType dataPlaneStateChange, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType reserveTimeout(ReserveTimeoutRequestType reserveTimeout, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType messageDeliveryTimeout(MessageDeliveryTimeoutRequestType messageDeliveryTimeout, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

}
