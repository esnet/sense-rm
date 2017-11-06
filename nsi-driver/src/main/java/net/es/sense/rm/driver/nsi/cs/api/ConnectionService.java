package net.es.sense.rm.driver.nsi.cs.api;

import java.util.List;
import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Holder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.cs.lib.CsParser;
import net.es.nsi.cs.lib.SimpleStp;
import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.ChildSummaryListType;
import org.ogf.schemas.nsi._2013._12.connection.types.ChildSummaryType;
import org.ogf.schemas.nsi._2013._12.connection.types.DataPlaneStateChangeRequestType;
import org.ogf.schemas.nsi._2013._12.connection.types.DataPlaneStatusType;
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
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryResultCriteriaType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryResultType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveTimeoutRequestType;
import org.ogf.schemas.nsi._2013._12.framework.headers.CommonHeaderType;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
@WebService(serviceName = "ConnectionServiceRequester", portName = "ConnectionServiceRequesterPort", endpointInterface = "org.ogf.schemas.nsi._2013._12.connection.requester.ConnectionRequesterPort", targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/requester", wsdlLocation = "")
public class ConnectionService {

  private final ReservationService reservationService;

  private final static ObjectFactory FACTORY = new ObjectFactory();

  public ConnectionService(ReservationService reservationService) {
    this.reservationService = reservationService;
  }

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
    // Get the providerNSA identifier.
    String providerNsa = header.value.getProviderNSA();

    // Extract the uPA connection segments associated with individual networks.
    List<QuerySummaryResultType> reservations = querySummaryConfirmed.getReservation();
    log.info("[ConnectionService] querySummaryConfirmed: providerNSA = {}, # of reservations = {}",
            providerNsa, reservations.size());

    // Process each reservation returned.
    for (QuerySummaryResultType reservation : reservations) {
      // Get the parent reservation information to apply to child connections.
      ReservationStateEnumType reservationState = reservation.getConnectionStates().getReservationState();
      DataPlaneStatusType dataPlaneStatus = reservation.getConnectionStates().getDataPlaneStatus();
      log.info("[ConnectionService] querySummaryConfirmed: cid = {}, gid = {}, state = {}", reservation.getConnectionId(),
              reservation.getGlobalReservationId(), reservationState);

      // If this reservation is in the process of being created, or failed
      // creation, then there will be no associated criteria.
      if (reservation.getCriteria().isEmpty()) {
        processReservationNoCriteria(
                providerNsa,
                reservation.getGlobalReservationId(),
                reservation.getDescription(),
                reservation.getConnectionId(),
                reservationState,
                dataPlaneStatus);
      } else {
        processSummaryCriteria(
                providerNsa,
                reservation.getGlobalReservationId(),
                reservation.getDescription(),
                reservation.getConnectionId(),
                reservationState,
                dataPlaneStatus,
                reservation.getCriteria());
      }
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  private void processReservationNoCriteria(
          String providerNsa,
          String gid,
          String description,
          String cid,
          ReservationStateEnumType reservationState,
          DataPlaneStatusType dataPlaneStatus) {
    log.info("[ConnectionService] processReservationNoCriteria: connectionId = {}", cid);

    Reservation reservation = reservationService.get(providerNsa, cid);
    if (reservation != null) {
      // We have already stored this so update only if state has changed.
      if (reservationState.compareTo(reservation.getReservationState()) == 0
              && dataPlaneStatus.isActive() == reservation.isDataPlaneActive()) {
        // No changes so no work to do.
        return;
      }

      // We have had a state change so update the reservation.
      reservation.setReservationState(reservationState);
      reservation.setDataPlaneActive(dataPlaneStatus.isActive());
      reservation.setDiscovered(System.currentTimeMillis());
    } else {
      reservation = new Reservation();
      reservation.setGlobalReservationId(gid);
      reservation.setDescription(description);
      reservation.setDiscovered(System.currentTimeMillis());
      reservation.setProviderNsa(providerNsa);
      reservation.setConnectionId(cid);
      reservation.setReservationState(reservationState);
      reservation.setDataPlaneActive(dataPlaneStatus.isActive());
      reservation.setVersion(0);
    }

    log.info("[ConnectionService] processReservationNoCriteria: storing reservation = {}", reservation);
    reservationService.store(reservation);
  }

  private void processSummaryCriteria(
          String providerNsa,
          String gid,
          String description,
          String cid,
          ReservationStateEnumType reservationState,
          DataPlaneStatusType dataPlaneStatus,
          List<QuerySummaryResultCriteriaType> criteriaList) {

    log.info("[ConnectionService] processSummaryCriteria: connectionId = {}", cid);

    // There will be one criteria for each version of this reservation. We
    // will check to see if there are any new versions than what is already
    // stored.
    for (QuerySummaryResultCriteriaType criteria : criteriaList) {
      log.info("[ConnectionService] processCriteria: cid = {}, version = {}, serviceType = {}",
              cid, criteria.getVersion(), criteria.getServiceType());

      ChildSummaryListType children = criteria.getChildren();
      if (children == null || children.getChild().isEmpty()) {
        // We are at a leaf child so check to see if we need to store this reservation information.
        Reservation reservation = reservationService.get(providerNsa, cid);
        if (reservation != null && reservation.getVersion() >= criteria.getVersion()) {
          // We have already stored this so update only if state has changed.
          if (reservationState.compareTo(reservation.getReservationState()) != 0
                  || dataPlaneStatus.isActive() != reservation.isDataPlaneActive()) {
            reservation.setReservationState(reservationState);
            reservation.setDataPlaneActive(dataPlaneStatus.isActive());
            reservation.setDiscovered(System.currentTimeMillis());

            log.info("[ConnectionService] processReservation: updating resvervation = {}", reservation);
            reservationService.store(reservation);
          }
          continue;
        }

        reservation = new Reservation();
        reservation.setGlobalReservationId(gid);
        reservation.setDescription(description);
        reservation.setDiscovered(System.currentTimeMillis());
        reservation.setProviderNsa(providerNsa);
        reservation.setConnectionId(cid);
        reservation.setReservationState(reservationState);
        reservation.setDataPlaneActive(dataPlaneStatus.isActive());
        reservation.setVersion(criteria.getVersion());
        reservation.setServiceType(criteria.getServiceType().trim());
        reservation.setStartTime(getStartTime(criteria.getSchedule().getStartTime()));
        reservation.setEndTime(getEndTime(criteria.getSchedule().getEndTime()));

        // Now we need to determine the network based on the STP used in the service.
        try {
          serializeP2PS(criteria.getServiceType(), criteria.getAny(), reservation);

          // Replace the existing entry with this new criteria if we already have one.
          log.info("[ConnectionService] processReservation: store resvervation = {}", reservation);
          reservationService.store(reservation);
        }
        catch (JAXBException ex) {
          log.error("[ConnectionService] processReservation failed for connectionId = {}",
                  reservation.getConnectionId());
        }
      } else {
        // We still have children so this must be an aggregator.
        for (ChildSummaryType child : children.getChild()) {
          Reservation reservation = reservationService.get(child.getProviderNSA(), child.getConnectionId());
          if (reservation != null && reservation.getVersion() >= criteria.getVersion()) {
            // We have already stored this so update only if state has changed.
            if (reservationState.compareTo(reservation.getReservationState()) != 0
                    || dataPlaneStatus.isActive() != reservation.isDataPlaneActive()) {
              reservation.setReservationState(reservationState);
              reservation.setDataPlaneActive(dataPlaneStatus.isActive());
              reservation.setDiscovered(System.currentTimeMillis());
              reservationService.store(reservation);
            }
            continue;
          }

          reservation = new Reservation();
          reservation.setDiscovered(System.currentTimeMillis());
          reservation.setGlobalReservationId(gid);
          reservation.setProviderNsa(child.getProviderNSA());
          reservation.setConnectionId(child.getConnectionId());
          reservation.setVersion(criteria.getVersion());
          reservation.setServiceType(child.getServiceType().trim());
          reservation.setReservationState(reservationState);
          reservation.setDataPlaneActive(dataPlaneStatus.isActive());
          reservation.setStartTime(getStartTime(criteria.getSchedule().getStartTime()));
          reservation.setEndTime(getEndTime(criteria.getSchedule().getEndTime()));

          // Now we need to determine the network based on the STP used in the service.
          try {
            serializeP2PS(child.getServiceType(), child.getAny(), reservation);

            // Replace the existing entry with this new criteria if we already have one.
            log.info("[ConnectionService] processReservation: store resvervation = {}", reservation);
            reservationService.store(reservation);
          }
          catch (JAXBException ex) {
            log.error("[ConnectionService] processReservation failed for connectionId = {}",
                    reservation.getConnectionId());
          }
        }
      }
    }
  }

  private void serializeP2PS(String serviceType, List<Object> any, Reservation reservation) throws JAXBException {
    if (Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(serviceType)
                || Nsi.NSI_SERVICETYPE_EVTS_OPENNSA.equalsIgnoreCase(serviceType)) {
          reservation.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
          log.info("[ConnectionService] serializeP2PS: serviceType = {}", serviceType);
          for (Object object : any) {
            log.info("[ConnectionService] serializeP2PS: object = {}", object.getClass().getCanonicalName());

            if (object instanceof org.apache.xerces.dom.ElementNSImpl) {
              org.apache.xerces.dom.ElementNSImpl element = (org.apache.xerces.dom.ElementNSImpl) object;
              log.info("[ConnectionService] serializeP2PS: getBaseURI = {}, localName = {}", element.getBaseURI(), element.getLocalName());

              if ("p2ps".equalsIgnoreCase(element.getLocalName())) {
                P2PServiceBaseType p2p = CsParser.getInstance().node2p2ps((Node) element);
                SimpleStp stp = new SimpleStp(p2p.getSourceSTP());
                reservation.setTopologyId(stp.getNetworkId());
                reservation.setService(CsParser.getInstance().p2ps2xml(p2p));
                break;
              }
            }
          }
        }
  }

  public GenericAcknowledgmentType queryRecursiveConfirmed(QueryRecursiveConfirmedType queryRecursiveConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

/**
  public GenericAcknowledgmentType queryRecursiveConfirmed(
          QueryRecursiveConfirmedType queryRecursiveConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {

    log.debug("[ConnectionService] queryRecursiveConfirmed: reservationService = {}", reservationService);

    // Get the providerNSA identifier.
    String providerNsa = header.value.getProviderNSA();

    // Extract the uPA connection segments associated with individual networks.
    List<QueryRecursiveResultType> reservations = queryRecursiveConfirmed.getReservation();
    log.info("[ConnectionService] queryRecursiveConfirmed: providerNSA = {}, # of reservations = {}",
            providerNsa, reservations.size());

    // Process each reservation returned.
    for (QueryRecursiveResultType reservation : reservations) {
      // Get the parent reservation information to apply to child connections.
      ReservationStateEnumType reservationState = reservation.getConnectionStates().getReservationState();
      DataPlaneStatusType dataPlaneStatus = reservation.getConnectionStates().getDataPlaneStatus();
      log.info("[ConnectionService] queryRecursiveConfirmed: cid = {}, gid = {}, state = {}", reservation.getConnectionId(),
              reservation.getGlobalReservationId(), reservationState);

      processRecursiveCriteria(
              providerNsa,
              reservation.getGlobalReservationId(),
              reservation.getConnectionId(),
              reservationState,
              dataPlaneStatus,
              reservation.getCriteria());
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  private void processRecursiveCriteria(
          String providerNsa,
          String gid,
          String cid,
          ReservationStateEnumType reservationState,
          DataPlaneStatusType dataPlaneStatus,
          List<QueryRecursiveResultCriteriaType> criteriaList) {

    // There will be one criteria for each version of this reservation. We
    // will check to see if there are any new versions than what is already
    // stored.
    for (QueryRecursiveResultCriteriaType criteria : criteriaList) {
      log.info("[ConnectionService] processCriteria: cid = {}, version = {}, serviceType = {}",
              cid, criteria.getVersion(), criteria.getServiceType());

      ChildRecursiveListType children = criteria.getChildren();
      if (children == null || children.getChild().isEmpty()) {
        // We are at a leaf child so check to see if we need to store this reservation information.
        Reservation existing = reservationService.get(providerNsa, cid);
        if (existing != null && existing.getVersion() >= criteria.getVersion()) {
          // We have already stored this so update only if state has changed.
          if (reservationState.compareTo(existing.getReservationState()) != 0
                  || dataPlaneStatus.isActive() != existing.isDataPlaneActive()) {
            existing.setReservationState(reservationState);
            existing.setDataPlaneActive(dataPlaneStatus.isActive());
            existing.setDiscovered(System.currentTimeMillis());
            reservationService.update(existing);
          }
          continue;
        }

        Reservation reservation = new Reservation();
        reservation.setDiscovered(System.currentTimeMillis());
        reservation.setGlobalReservationId(gid);
        reservation.setProviderNsa(providerNsa);
        reservation.setConnectionId(cid);
        reservation.setVersion(criteria.getVersion());
        reservation.setServiceType(criteria.getServiceType().trim());
        reservation.setStartTime(getStartTime(criteria.getSchedule().getStartTime()));
        reservation.setEndTime(getEndTime(criteria.getSchedule().getEndTime()));

        // Now we need to determine the network based on the STP used in the service.
        if (Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(reservation.getServiceType())
                || Nsi.NSI_SERVICETYPE_EVTS_OPENNSA.equalsIgnoreCase(reservation.getServiceType())) {
          reservation.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
          for (Object any : criteria.getAny()) {
            if (any instanceof JAXBElement) {
              JAXBElement jaxb = (JAXBElement) any;
              if (jaxb.getDeclaredType() == P2PServiceBaseType.class) {
                log.debug("[ConnectionService] processRecursiveCriteria: found P2PServiceBaseType");
                reservation.setService(XmlUtilities.jaxbToString(P2PServiceBaseType.class, jaxb));

                // Get the network identifier from and STP.
                P2PServiceBaseType p2p = (P2PServiceBaseType) jaxb.getValue();
                SimpleStp stp = new SimpleStp(p2p.getSourceSTP());
                reservation.setTopologyId(stp.getNetworkId());
                break;
              }
            }
          }
        }

        // Replace the existing entry with this new criteria if we already have one.
        if (existing != null) {
          reservation.setId(existing.getId());
          reservationService.update(reservation);
        } else {
          reservationService.create(reservation);
        }
      } else {
        // We still have children so this must be an aggregator.
        children.getChild().forEach((child) -> {
          child.getConnectionStates();
          processRecursiveCriteria(
                  child.getProviderNSA(),
                  gid,
                  child.getConnectionId(),
                  child.getConnectionStates().getReservationState(),
                  child.getConnectionStates().getDataPlaneStatus(),
                  child.getCriteria());
        });
      }
    }
  }
**/

  private long getStartTime(JAXBElement<XMLGregorianCalendar> time) {
    if (time == null || time.getValue() == null) {
      return 0;
    }

    return time.getValue().toGregorianCalendar().getTimeInMillis();
  }

  private long getEndTime(JAXBElement<XMLGregorianCalendar> time) {
    if (time == null || time.getValue() == null) {
      return Long.MAX_VALUE;
    }

    return time.getValue().toGregorianCalendar().getTimeInMillis();
  }

  public GenericAcknowledgmentType queryNotificationConfirmed(QueryNotificationConfirmedType queryNotificationConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  public GenericAcknowledgmentType queryResultConfirmed(QueryResultConfirmedType queryResultConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
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