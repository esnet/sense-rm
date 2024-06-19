/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.sense.rm.driver.nsi.cs.api;

import akka.actor.ActorRef;
import com.google.common.base.Strings;
import jakarta.jws.WebService;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.Holder;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.common.jaxb.JaxbParser;
import net.es.nsi.cs.lib.CsParser;
import net.es.nsi.cs.lib.SimpleStp;
import net.es.sense.rm.driver.nsi.RaController;
import net.es.sense.rm.driver.nsi.cs.db.*;
import net.es.sense.rm.driver.nsi.messages.AuditRequest;
import net.es.sense.rm.driver.nsi.messages.TerminateRequest;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.*;
import org.ogf.schemas.nsi._2013._12.framework.headers.CommonHeaderType;
import org.ogf.schemas.nsi._2013._12.framework.types.ServiceExceptionType;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparing;

/**
 * This is the NSI CS 2.1 web service requester endpoint used to receive responses from our associated uPA.
 * Communication between the requester thread and this requester endpoint is controlled using a semaphore
 * allowing the request thread to block on the returned response. Reservation state is updated through the
 * ReservationService which maintains reservations in the database.  WebService endpoints within this class
 * are called by the spring web container when the connection service endpoint is invoked by an external
 * entity.
 *
 * @author hacksaw
 */
@Component
@WebService(
        serviceName = "ConnectionServiceRequester",
        portName = "ConnectionServiceRequesterPort",
        endpointInterface = "org.ogf.schemas.nsi._2013._12.connection.requester.ConnectionRequesterPort",
        targetNamespace = "http://schemas.ogf.org/nsi/2013/12/connection/requester",
        wsdlLocation = "")
public class ConnectionService {
  // Trying to get logging to work in this class ....
  private final Logger log = LoggerFactory.getLogger(getClass());

  // The runtime NSI configuration information.
  private final NsiProperties nsiProperties;

  // We store reservations using the reservation service.
  private final ReservationService reservationService;

  // We synchronize with the requester thread using the operationMap that holds a semaphore.
  private final OperationMapRepository operationMap;

  // AKKA actor system to route fire-and-forget operations.
  private final RaController raController;

  // Our NSI CS object factory for creating protocol objects.
  private final static ObjectFactory FACTORY = new ObjectFactory();

  /**
   * We initialize the ConnectionService component with the needed references since this component does not support
   * auto-wiring.
   *
   * @param nsiProperties The runtime NSI configuration information.
   * @param reservationService We store reservations using the reservation service.
   * @param operationMap We synchronize with the requester thread using the operationMap that holds a semaphore.
   * @param raController Handle to the AKKA actor system.
   */
  public ConnectionService(NsiProperties nsiProperties, ReservationService reservationService,
          OperationMapRepository operationMap, RaController raController) {
    this.nsiProperties = nsiProperties;
    this.reservationService = reservationService;
    this.operationMap = operationMap;
    this.raController = raController;
  }

  /**
   * Endpoint receiving the NSI CS reserveConfirmed response message.
   *
   * @param reserveConfirmed The incoming NSI reserved confirmed message.
   * @param header The NIS common header associated with the incoming message.
   * @return GenericAcknowledgmentType acknowledging the reserveConfirmed has been accepted for processing.
   * @throws ServiceException When an issue processing the reserveConfirmed has occurred.
   */
  public GenericAcknowledgmentType reserveConfirmed(
          ReserveConfirmedType reserveConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {

    // We have some attributes in the SOAP header.
    CommonHeaderType value = header.value;
    String correlationId = value.getCorrelationId();
    String connectionId = reserveConfirmed.getConnectionId();
    ReservationConfirmCriteriaType criteria = reserveConfirmed.getCriteria();

    try {
      log.info("[ConnectionService] reserveConfirmed received for correlationId = {}, reserveConfirmed:\n{}",
          correlationId, CsParser.getInstance().reserveConfirmedType2xml(reserveConfirmed));
    } catch (JAXBException ex) {
      log.error("[ConnectionService] reserveConfirmed could not encode log message, correlationId = {}",
          correlationId, ex);
    }

    // Look up the operation map corresponding to this response message.
    Operation op = operationMap.get(correlationId);
    Reservation reservation;
    if (op == null) {
      // There is no pending operation for this reserveConfirmed so something must
      // have gone wrong like a timeout delay.  Try to recover.
      log.error("[ConnectionService] reserveConfirmed can't find outstanding operation for correlationId = {}",
          correlationId);
      reservation = reservationService.getByAnyConnectionId(value.getProviderNSA(), connectionId)
          .stream().max(comparing(Reservation::getVersion)).orElse(null);
      if (reservation == null) {
        log.error("[ConnectionService] reserveConfirmed could not find reservation for connectionId = {}",
            connectionId);
      }
    } else {
      log.error("[ConnectionService] reserveConfirmed found outstanding operation uniqueId = {} for correlationId = {}",
          op.getUniqueId(), value.getCorrelationId());
      reservation = reservationService.getByUniqueId(op.getUniqueId());
      if (reservation == null) {
        log.error("[ConnectionService] reserveConfirmed cannot find reservation for uniqueId = {}, correlationId = {}",
            op.getUniqueId(), value.getCorrelationId());
      }
    }

    // We have two cases to handle: 1) This is an unknown reservation, so we need to
    // create one, 2) it is a known reservation so we need to update the existing
    // one with this new information.
    if (reservation == null) {
      // Handle the unknown reservation case.
      DataPlaneStatusType dataPlaneStatus = FACTORY.createDataPlaneStatusType();
      dataPlaneStatus.setVersion(criteria.getVersion());
      dataPlaneStatus.setActive(false);
      dataPlaneStatus.setVersionConsistent(true);

      // Parse the associated criteria structure.
      reservation = processConfirmedCriteria(
          header.value.getProviderNSA(),
          nsiProperties.getNetworkId(),
          reserveConfirmed.getGlobalReservationId(),
          reserveConfirmed.getDescription(),
          connectionId,
          ReservationStateEnumType.RESERVE_HELD,
          ProvisionStateEnumType.RELEASED,
          LifecycleStateEnumType.CREATED,
          dataPlaneStatus,
          criteria);
    } else {
      // Check to see if the reservation versions match.
      if (reservation.getVersion() != criteria.getVersion()) {
        log.error("[ConnectionService] reserveConfirmed: reservation cid = {}, has inconsistent versions {}:{}",
            connectionId, reservation.getVersion(), criteria.getVersion());
        reservation.setVersion(criteria.getVersion());
      }

      // Check to see if there was a change in startTime.
      long startTime = CsUtils.getStartTime(criteria.getSchedule().getStartTime());
      if (startTime != reservation.getStartTime()) {
        log.error("[ConnectionService] reserveConfirmed: reservation cid = {}, has changed startTime {}:{}",
            connectionId, reservation.getStartTime(), startTime);
        reservation.setStartTime(startTime);
      }

      // Check to see if there was a change in endTime.
      long endTime = CsUtils.getStartTime(criteria.getSchedule().getEndTime());
      if (endTime != reservation.getEndTime()) {
        log.error("[ConnectionService] reserveConfirmed: reservation cid = {}, has changed endTime {}:{}",
            connectionId, reservation.getEndTime(), endTime);
        reservation.setEndTime(endTime);
      }

      // Add a serialized version of the service.
      reservation.setServiceType(criteria.getServiceType());
      try {
        CsUtils.serializeP2PS(criteria.getServiceType(), criteria.getAny(), reservation);
      } catch (JAXBException ex) {
          log.error("[ConnectionService] reserveConfirmed serializeP2PS failed for connectionId = {}",
              connectionId, ex);
      }

      // Update reservation state machine.
      reservation.setReservationState(ReservationStateEnumType.RESERVE_HELD);

      // Update last discovered time.
      reservation.setDiscovered(System.currentTimeMillis());
    }

    // We have not seen this reservation before so store it.
    log.info("[ConnectionService] reserveConfirmed: storing new reservation, cid = {}, reservation:\n{}",
        connectionId, reservation);
    reservationService.store(reservation);

    if (op != null) {
      log.info("[ConnectionService] reserveConfirmed: releasing operation for correlationId = {}",
          correlationId);

      op.setState(StateType.reserved);
      op.getCompleted().release();
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Converts the ReservationConfirmCriteriaType into a reservation object
   * for storage in the database.  ConfirmCriteria does not contain any
   * information about child reservation created as a result of us issuing
   * the reservation through an aggregator.  As a result, a NSI query later
   * in process may have additional child connection details.
   *
   * @param providerNsa The providerNSA to which we issued the reservation.
   * @param networkId The networkId this SENSE-NSI-RM instance is managing.
   * @param gid The global identifier of the reservation.
   * @param description The description of the reservation.
   * @param cid The connection identifier associated with this reservation.
   * @param reservationState Reservation state.
   * @param provisionState Provision state.
   * @param lifecycleState Life cycle state.
   * @param dataPlaneStatus Dataplane state.
   * @param criteria The reservation criteria.
   * @return The reservation contained in the confirmed criteria.
   */
  private Reservation processConfirmedCriteria(
          String providerNsa,
          String networkId,
          String gid,
          String description,
          String cid,
          ReservationStateEnumType reservationState,
          ProvisionStateEnumType provisionState,
          LifecycleStateEnumType lifecycleState,
          DataPlaneStatusType dataPlaneStatus,
          ReservationConfirmCriteriaType criteria) {

    log.info("[ConnectionService] processConfirmedCriteria: connectionId = {}", cid);

    // We should look into the P2PS service for the connection endpoints to
    // determine the networkId but we will assume if we are receiving the
    // confirmation we correctly sent the request.
    Reservation reservation = new Reservation();
    reservation.setProviderNsa(providerNsa);
    reservation.setGlobalReservationId(gid);
    reservation.setDescription(description);
    reservation.setConnectionId(cid);
    reservation.setReservationState(reservationState);
    reservation.setProvisionState(provisionState);
    reservation.setLifecycleState(lifecycleState);
    reservation.setDataPlaneActive(dataPlaneStatus.isActive());
    reservation.setVersion(criteria.getVersion());
    reservation.setServiceType(criteria.getServiceType());
    reservation.setStartTime(CsUtils.getStartTime(criteria.getSchedule().getStartTime()));
    reservation.setEndTime(CsUtils.getEndTime(criteria.getSchedule().getEndTime()));
    reservation.setDiscovered(System.currentTimeMillis());

    // Now we need to determine the network based on the STP used in the service.
    try {
      CsUtils.serializeP2PS(criteria.getServiceType(), criteria.getAny(), reservation);

      if (Strings.isNullOrEmpty(reservation.getTopologyId())) {
        reservation.setTopologyId(networkId);
      }

      return reservation;
    } catch (JAXBException ex) {
      log.error("[ConnectionService] processReservation failed for connectionId = {}",
              reservation.getConnectionId(), ex);
      return null;
    }
  }

  /**
   * A utility method to get the network identifier based on STP within he P2P
   * service structure.
   *
   * @param serviceType The service type associated with the reservation.
   * @param anyList The list of XML ANY extension objects from the reservation structure.
   * @return Returns an Optional String containing the networkId if present.
   * @throws JAXBException When the P2P structure cannot be parsed.
   */
  public static Optional<String> getNetworkId(String serviceType, List<Object> anyList) throws JAXBException {
    // Now we need to determine the network based on the STP used in the service.
    if (Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(serviceType) ||
            Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_1.equalsIgnoreCase(serviceType) ||
            Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_2.equalsIgnoreCase(serviceType) ||
            Nsi.NSI_SERVICETYPE_EVTS_OSCARS.equalsIgnoreCase(serviceType)) {
      for (Object any : anyList) {
        if (any instanceof JAXBElement) {
          JAXBElement jaxb = (JAXBElement) any;
          if (jaxb.getDeclaredType() == P2PServiceBaseType.class) {
            // Get the network identifier from and STP
            P2PServiceBaseType p2p = (P2PServiceBaseType) jaxb.getValue();
            SimpleStp stp = new SimpleStp(p2p.getSourceSTP());
            return Optional.of(stp.getNetworkId());
          }
        } else if (any instanceof org.w3c.dom.Element) {
          org.w3c.dom.Element element = (org.w3c.dom.Element) any;
          if ("p2ps".equalsIgnoreCase(element.getLocalName())) {
            P2PServiceBaseType p2ps = CsParser.getInstance().node2p2ps((Node) element);
            SimpleStp stp = new SimpleStp(p2ps.getSourceSTP());
            return Optional.of(stp.getNetworkId());
          }
        }
      }
    }

    return Optional.empty();
  }

  /**
   * Lookup a reservation in the database based on stored operation (if available) or
   * other provided criteria.
   *
   * @param providerNsa The providerNSA identifier returned in the NSI-CS response.
   * @param correlationId The correlationId for the NSI-CS message exchange.
   * @param connectionId The connectionId for the NSI-CS reservation.
   * @return The reservation object from the database if found.
   */
  private Reservation getReservation(String providerNsa, String correlationId, String connectionId) {
    Operation op = operationMap.get(correlationId);
    if (op == null) {
      log.error("[ConnectionService] getReservation: can't find outstanding operation for correlationId = {}",
          correlationId);

      // Maybe the operation timed out and has already been removed?  Look
      // up the reservation using the connectionId.
      return reservationService.getByProviderNsaAndConnectionId(providerNsa, connectionId)
          .stream().max(comparing(Reservation::getVersion)).orElse(null);
    } else {
      // We need to find and update the associated reservation state machine.
      log.info("[ConnectionService] getReservation: found outstanding operation uniqueId = {} for correlationId = {}",
          op.getUniqueId(), correlationId);

      // Use the uniqueId stored in operation to look up reservation.
      return reservationService.getByUniqueId(op.getUniqueId());
    }
  }

  /**
   * Endpoint receiving the NSI CS reserveFailed message.
   *
   * @param reserveFailed
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType reserveFailed(GenericFailedType reserveFailed,
          Holder<CommonHeaderType> header) throws ServiceException {

    // Pull specific attributes from NSI-CS SOAP header.
    String providerNsa = header.value.getProviderNSA();
    String correlationId = header.value.getCorrelationId();
    String connectionId = reserveFailed.getConnectionId();
    String error = JaxbParser.jaxb2String(ServiceExceptionType.class, reserveFailed.getServiceException());

    log.info("[ConnectionService] reserveFailed: received for correlationId = {}, connectionId = {}, error = {}",
        correlationId, connectionId, error);

    // Get reservation associated with this reserveFailed message.
    Reservation reservation = getReservation(providerNsa, correlationId, connectionId);
    if (reservation == null) {
      log.error("[ConnectionService] reserveFailed: could not find reservation for connectionId = {}",
          connectionId);
    } else {
      // We update the stored reservation.
      log.info("[ConnectionService] reserveFailed: storing reservation update, cid = {}", connectionId);

      if (reservationService.setReserveFailed(reservation.getId(), ReservationStateEnumType.RESERVE_FAILED,
          Reservation.ErrorState.NSIRESERVEFAILED,
          String.format("reserveFailed on cid = %s, serviceException = %s", connectionId, error),
          System.currentTimeMillis()) != 1) {
        log.info("[ConnectionService] error updating cid = {}", connectionId);
      }
    }

    // Look up operation corresponding to the message correlationId.
    Operation op = operationMap.get(correlationId);
    if (op == null) {
      log.error("[ConnectionService] reserveFailed: can't find outstanding operation for correlationId = {}",
          correlationId);
    } else {
      log.info("[ConnectionService] reserveFailed: releasing outstanding operation for correlationId = {}",
          correlationId);

      // Now compete the operation.
      op.setState(StateType.failed);
      op.setException(reserveFailed.getServiceException());
      op.getCompleted().release();
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the NSI CS reserveCommitConfirmed message.
   *
   * @param reserveCommitConfirmed
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType reserveCommitConfirmed(GenericConfirmedType reserveCommitConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {

    // Pull specific attributes from NSI-CS SOAP header.
    String providerNsa = header.value.getProviderNSA();
    String correlationId = header.value.getCorrelationId();
    String connectionId = reserveCommitConfirmed.getConnectionId();

    log.info("[ConnectionService] reserveCommitConfirmed received for correlationId = {}, cid = {}",
        correlationId, connectionId);

    // Get reservation associated with this reserveFailed message.
    Reservation reservation = getReservation(providerNsa, correlationId, connectionId);
    if (reservation == null) {
      log.error("[ConnectionService] reserveCommitConfirmed: could not find reservation for connectionId = {}",
          connectionId);
    } else {
      // Update the reservation status to the stable state of RESERVE_START.
      log.info("[ConnectionService] reserveCommitConfirmed: storing reservation update, cid = {}", connectionId);

      if (reservationService.setReservationState(reservation.getId(), ReservationStateEnumType.RESERVE_START,
          System.currentTimeMillis()) != 1) {
        log.info("[ConnectionService] reserveCommitConfirmed: error updating cid = {}", connectionId);
      }
    }

    // Look up operation corresponding to the message correlationId.
    Operation op = operationMap.get(correlationId);
    if (op == null) {
      log.error("[ConnectionService] reserveCommitConfirmed: can't find outstanding operation for correlationId = {}",
          correlationId);
    } else {
      log.info("[ConnectionService] reserveCommitConfirmed: releasing outstanding operation for correlationId = {}",
          correlationId);

      // Now compete the operation.
      op.setState(StateType.committed);
      op.getCompleted().release();
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the NSI CS reserveCommitFailed message.
   *
   * @param reserveCommitFailed
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType reserveCommitFailed(GenericFailedType reserveCommitFailed,
          Holder<CommonHeaderType> header) throws ServiceException {

    // Pull specific attributes from NSI-CS SOAP header.
    String providerNsa = header.value.getProviderNSA();
    String correlationId = header.value.getCorrelationId();
    String connectionId = reserveCommitFailed.getConnectionId();
    String error = JaxbParser.jaxb2String(ServiceExceptionType.class, reserveCommitFailed.getServiceException());

    log.info("[ConnectionService] reserveCommitFailed: received for correlationId = {}, cid = {}, error = {}",
            correlationId, connectionId, error);

    // First we update the corresponding reservation in the database.  If we
    // are talking to an aggregator this CID is the parent reservation the commit
    // was done against. The child connections will get updated next querySummary.
    Reservation reservation = getReservation(providerNsa, correlationId, connectionId);
    if (reservation == null) {
      log.error("[ConnectionService] reserveCommitFailed: could not find reservation for connectionId = {}",
          connectionId);
    } else {
      // Update the reservation status to the stable state of RESERVE_START.
      log.info("[ConnectionService] reserveCommitFailed: storing reservation update, cid = {}", connectionId);

      if (reservationService.setReserveFailed(reservation.getId(), ReservationStateEnumType.RESERVE_FAILED,
          Reservation.ErrorState.NSIRESERVECOMMIT,
          String.format("reserveCommitFailed on cid = %s, serviceException = %s", connectionId, error),
          System.currentTimeMillis()) != 1) {
        log.info("[ConnectionService] reserveCommitFailed: error updating cid = {}", connectionId);
      }
    }

    // Look up operation corresponding to the message correlationId.
    Operation op = operationMap.get(correlationId);
    if (op == null) {
      log.error("[ConnectionService] reserveCommitFailed: can't find outstanding operation for correlationId = {}",
          correlationId);
    } else {
      log.info("[ConnectionService] reserveCommitFailed: releasing outstanding operation for correlationId = {}",
          correlationId);

      // Now compete the operation.
      op.setState(StateType.failed);
      op.setException(reserveCommitFailed.getServiceException());
      op.getCompleted().release();
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the NSI CS reserveAbortConfirmed message.
   *
   * @param reserveAbortConfirmed
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType reserveAbortConfirmed(GenericConfirmedType reserveAbortConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {

    String providerNsa = header.value.getProviderNSA();
    String correlationId = header.value.getCorrelationId();
    String connectionId = reserveAbortConfirmed.getConnectionId();

    log.info("[ConnectionService] reserveAbortConfirmed: received for correlationId = {}, cid = {}",
        correlationId, connectionId);

    // First we update the corresponding reservation in the database.  If we
    // are talking to an aggregator this CID is the parent reservation the abort
    // was done against. The child connections will get updated next querySummary.
    Reservation reservation = getReservation(providerNsa, correlationId, connectionId);
    if (reservation == null) {
      log.error("[ConnectionService] reserveAbortConfirmed: could not find reservation for connectionId = {}",
          connectionId);
    } else {
      // Update the reservation status to the stable state of RESERVE_START.
      log.info("[ConnectionService] reserveAbortConfirmed: storing reservation update, cid = {}", connectionId);

      if (reservationService.setReservationState(reservation.getId(), ReservationStateEnumType.RESERVE_START,
          System.currentTimeMillis()) != 1) {
        log.info("[ConnectionService] reserveAbortConfirmed: error updating cid = {}", connectionId);
      }
    }

    // Look up operation corresponding to the message correlationId.
    Operation op = operationMap.get(correlationId);
    if (op == null) {
      log.error("[ConnectionService] reserveAbortConfirmed: can't find outstanding operation for correlationId = {}",
          correlationId);
    } else {
      log.info("[ConnectionService] reserveAbortConfirmed: releasing outstanding operation for correlationId = {}",
          correlationId);

      // Now compete the operation.
      op.setState(StateType.aborted);
      op.getCompleted().release();
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the NSI CS provisionConfirmed message.
   *
   * @param provisionConfirmed
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType provisionConfirmed(GenericConfirmedType provisionConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {

    String providerNsa = header.value.getProviderNSA();
    String correlationId = header.value.getCorrelationId();
    String connectionId = provisionConfirmed.getConnectionId();

    log.info("[ConnectionService] provisionConfirmed: received for correlationId = {}, connectionId: {}",
        correlationId, connectionId);

    // First we update the corresponding reservation in the database.  If we
    // are talking to an aggregator this CID is the parent reservation the provision
    // was done against. The child connections will get updated next querySummary.
    Reservation reservation = getReservation(providerNsa, correlationId, connectionId);
    if (reservation == null) {
      log.error("[ConnectionService] provisionConfirmed: could not find reservation for connectionId = {}",
          connectionId);
    } else if (reservation.getLifecycleState() != LifecycleStateEnumType.CREATED) {
        // Timing issue - reservation is terminated.
        log.info("[ConnectionService] provisionConfirmed: reservation not in CREATED state = {}, cid = {}",
            reservation.getLifecycleState(), connectionId);
    } else {
      // We have to determine if the stored reservation needs to be updated.
      log.info("[ConnectionService] provisionConfirmed: storing reservation update, cid = {}", connectionId);

      if (reservationService.setProvisionState(reservation.getId(), ProvisionStateEnumType.PROVISIONED,
          System.currentTimeMillis()) != 1) {
        log.info("[ConnectionService] provisionConfirmed: error updating cid = {}", connectionId);
      }
    }

    // Look up operation corresponding to the message correlationId.
    Operation op = operationMap.get(correlationId);
    if (op == null) {
      log.error("[ConnectionService] provisionConfirmed: can't find outstanding operation for correlationId = {}",
          correlationId);
    } else {
      log.info("[ConnectionService] provisionConfirmed: releasing outstanding operation for correlationId = {}",
          correlationId);

      // Now compete the operation.
      op.setState(StateType.provisioned);
      op.getCompleted().release();
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the NSI CS releaseConfirmed message.
   *
   * @param releaseConfirmed
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType releaseConfirmed(GenericConfirmedType releaseConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {

    String providerNsa = header.value.getProviderNSA();
    String correlationId = header.value.getCorrelationId();
    String connectionId = releaseConfirmed.getConnectionId();

    log.info("[ConnectionService] releaseConfirmed: received for correlationId = {}, connectionId: {}",
        correlationId, connectionId);

    // First we update the corresponding reservation in the database.  If we
    // are talking to an aggregator this CID is the parent reservation the release
    // was done against. The child connections will get updated next querySummary.
    Reservation reservation = getReservation(providerNsa, correlationId, connectionId);
    if (reservation == null) {
      log.error("[ConnectionService] releaseConfirmed: could not find reservation for connectionId = {}",
          connectionId);
    } else if (reservation.getLifecycleState() != LifecycleStateEnumType.CREATED) {
      // Timing issue - reservation is terminated.
      log.info("[ConnectionService] releaseConfirmed: reservation not in CREATED state = {}, cid = {}",
          reservation.getLifecycleState(), connectionId);
    } else {
      // We have to determine if the stored reservation needs to be updated.
      log.info("[ConnectionService] releaseConfirmed: storing reservation update, cid = {}", connectionId);

      if (reservationService.setProvisionState(reservation.getId(), ProvisionStateEnumType.RELEASED,
          System.currentTimeMillis()) != 1) {
        log.info("[ConnectionService] releaseConfirmed: error updating cid = {}", connectionId);
      }
    }

    // Look up operation corresponding to the message correlationId.
    Operation op = operationMap.get(correlationId);
    if (op == null) {
      log.error("[ConnectionService] releaseConfirmed: can't find outstanding operation for correlationId = {}",
          correlationId);
    } else {
      log.info("[ConnectionService] releaseConfirmed: releasing outstanding operation for correlationId = {}",
          correlationId);

      // Now compete the operation.
      op.setState(StateType.released);
      op.getCompleted().release();
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the NSI CS terminateConfirmed message.
   *
   * @param terminateConfirmed
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType terminateConfirmed(GenericConfirmedType terminateConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    String providerNsa = header.value.getProviderNSA();
    String correlationId = header.value.getCorrelationId();
    String connectionId = terminateConfirmed.getConnectionId();

    log.info("[ConnectionService] terminateConfirmed: received for correlationId = {}, connectionId: {}",
        correlationId, connectionId);

    // First we update the corresponding reservation in the database.  If we
    // are talking to an aggregator this CID is the parent reservation the terminate
    // was done against. The child connections will get updated next querySummary.
    Reservation reservation = getReservation(providerNsa, correlationId, connectionId);
    if (reservation == null) {
      log.error("[ConnectionService] terminateConfirmed: could not find reservation for connectionId = {}",
          connectionId);
    } else {
      // We have to determine if the stored reservation needs to be updated.
      log.info("[ConnectionService] terminateConfirmed: storing reservation update, cid = {}", connectionId);

      if (reservationService.setLifecycleState(reservation.getId(), LifecycleStateEnumType.TERMINATED,
          System.currentTimeMillis()) != 1) {
        log.info("[ConnectionService] terminateConfirmed: error updating cid = {}", connectionId);
      }
    }

    // Look up operation corresponding to the message correlationId.
    Operation op = operationMap.get(correlationId);
    if (op == null) {
      log.error("[ConnectionService] terminateConfirmed: can't find outstanding operation for correlationId = {}",
          correlationId);
    } else {
      log.info("[ConnectionService] terminateConfirmed: releasing outstanding operation for correlationId = {}",
          correlationId);

      // Now compete the operation.
      op.setState(StateType.terminated);
      op.getCompleted().release();
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the NSI CS querySummaryConfirmed message.  These results are used
   * to create/update/delete reservations in the database.  The querySummaryConfirmed will
   * contain reservations not created by the SENSE-NSI-RM, and if we are connected to an
   * aggregator, then we will also have child connections.
   *
   * @param querySummaryConfirmed Structure containing the reservation information.
   * @param header NSI protocol header.
   * @return An acknowledgement indicating message has been accepted.
   * @throws ServiceException
   */
  public GenericAcknowledgmentType querySummaryConfirmed(QuerySummaryConfirmedType querySummaryConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {

    log.info("[ConnectionService] querySummaryConfirmed received, correlationId = {}",
            header.value.getCorrelationId());

    // We have a specific class to process the results from a QuerySummary operations.
    QuerySummary q = new QuerySummary(nsiProperties.getNetworkId(), reservationService);
    q.process(querySummaryConfirmed, header);
    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * We are not using this message from the NSI protocol.
   *
   * @param queryRecursiveConfirmed
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType queryRecursiveConfirmed(QueryRecursiveConfirmedType queryRecursiveConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  // We have disabled support for queryRecursive and will rely on the
  // aggregator NSA to maintain an accurate state if we are not connected
  // directly to a uPA.

  /*
  public GenericAcknowledgmentType queryRecursiveConfirmed(QueryRecursiveConfirmedType queryRecursiveConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {

    log.info("[ConnectionService] queryRecursiveConfirmed: reservationService = {}", reservationService);

    // Get the providerNSA identifier.
    String providerNsa = header.value.getProviderNSA();

    // Extract the uPA connection segments associated with individual networks.
    List<QueryRecursiveResultType> reservations = queryRecursiveConfirmed.getReservation();
    log.info("[ConnectionService] queryRecursiveConfirmed: providerNSA = {}, # of reservations = {}",
            providerNsa, reservations.size());
[ConnectionService] reserveTimeout for correlationId =
    // Process each reservation returned.
    for (QueryRecursiveResultType reservation : reservations) {
      // Get the parent reservation information to apply to child connections.
      ReservationStateEnumType reservationState = reservation.getConnectionStates().getReservationState();
      DataPlaneStatusType dataPlaneStatus = reservation.getConnectionStates().getDataPlaneStatus();
      log.info("[ConnectionService] queryRecursiveConfirmed: cid = {}, gid = {}, state = {}",
              reservation.getConnectionId(), reservation.getGlobalReservationId(), reservationState);

      processRecursiveCriteria(providerNsa, reservation.getGlobalReservationId(), reservation.getConnectionId(),
              reservationState, dataPlaneStatus, reservation.getCriteria());
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  private void processRecursiveCriteria(String providerNsa, String gid, String cid, ReservationStateEnumType reservationState, DataPlaneStatusType dataPlaneStatus, List<QueryRecursiveResultCriteriaType> criteriaList) {

    // There will be one criteria for each version of this reservation. We
    // will check to see if there are any new versions than what is already
    // stored.
    for (QueryRecursiveResultCriteriaType criteria : criteriaList) {
      log.info("[ConnectionService] processCriteria: cid = {}, version = {}, serviceType = {}", cid,
              criteria.getVersion(), criteria.getServiceType());

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

        }

        // Replace the existing entry with this new criteria if we already have one. if (existing != null) {
        reservation.setId(existing.getId());
        reservationService.update(reservation);
      } else {
        reservationService.create(reservation);
      }
    }else { // We still have children so this must be an aggregator.
   children.getChild().forEach((child) -> { child.getConnectionStates(); processRecursiveCriteria(
   child.getProviderNSA(), gid, child.getConnectionId(), child.getConnectionStates().getReservationState(),
   child.getConnectionStates().getDataPlaneStatus(), child.getCriteria()); }); }
  }
}

*/

  /**
   * We are not using this message from the NSI protocol.
   *
   * @param queryNotificationConfirmed
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType queryNotificationConfirmed(
          QueryNotificationConfirmedType queryNotificationConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  /**
   * We are not using this message from the NSI protocol.
   *
   * @param queryResultConfirmed
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType queryResultConfirmed(
          QueryResultConfirmedType queryResultConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {
    //TODO implement this method
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  /**
   * Endpoint receiving the NSI CS Error message.
   *
   * @param error
   * @param header
   * @return
   * @throws ServiceException
   * @throws JAXBException
   */
  public GenericAcknowledgmentType error(GenericErrorType error,
          Holder<CommonHeaderType> header) throws ServiceException, JAXBException {

    String providerNSA = header.value.getProviderNSA();
    String corrleationId = header.value.getCorrelationId();
    ServiceExceptionType serviceException = error.getServiceException();
    String cid = error.getServiceException().getConnectionId();
    String encoded = JaxbParser.jaxb2String(ServiceExceptionType.class, serviceException);

    log.info("[ConnectionService] error() received for providerNSA = {}, correlationId = {}, error: \n{}",
            providerNSA, corrleationId, CsParser.getInstance().genericError2xml(error));

    // We need to inform the requesting thread of the error.
    Operation op = operationMap.get(corrleationId);
    if (op == null) {
      log.error("[ConnectionService] error() add corrective action for condition, correlationId = {}, cid = {}, error:\n{}",
              corrleationId, cid, encoded);
    } else {
      op.setState(StateType.failed);
      op.setException(error.getServiceException());
      op.getCompleted().release();
    }

    // Update any impacted reservations with error information.
    Collection<Reservation> reservations = reservationService.getByAnyConnectionId(providerNSA, cid);
    if (reservations == null || reservations.isEmpty()) {
      // We have not seen this reservation before so ignore it.
      log.error("[ConnectionService] error() could not find cid = {}", cid);
    } else {
      for (Reservation r : reservations) {
        log.info("[ConnectionService] error() updating cid = {}, reservation {}", cid, r.toString());

        if (reservationService.setErrorState(r.getId(), Reservation.ErrorState.NSIERROR,
            String.format("error, cid = %s, serviceException = %s", cid, encoded), System.currentTimeMillis()) != 1) {
          log.info("[ConnectionService] error updating cid = {}", cid);
        }

        //r.setErrorState(Reservation.ErrorState.NSIERROR);
        //r.setErrorMessage(String.format("error, cid = %s, serviceException = %s", cid, encoded));
        //r.setDiscovered(System.currentTimeMillis());
        //log.info("[ConnectionService] writing updated cid = {}, reservation {}", cid, r.toString());
        //reservationService.store(r);
      }
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the autonomous NSI CS errorEvent message.
   *
   * @param errorEvent
   * @param header
   * @return
   * @throws ServiceException
   * @throws JAXBException
   */
  public GenericAcknowledgmentType errorEvent(ErrorEventType errorEvent, Holder<CommonHeaderType> header) throws ServiceException {

    // Something bad happened.  Dump the error and fail the operation.
    CommonHeaderType value = header.value;
    String providerNSA = value.getProviderNSA();
    String correlationId = value.getCorrelationId();
    String cid = errorEvent.getConnectionId();
    String error = errorEvent2xml(errorEvent);

    log.error("[ConnectionService] errorEvent received, providerNSA = {}, correlationId = {}, errorEvent:\n{}",
        providerNSA, correlationId, errorEvent2xml(errorEvent));

    // We need to inform the requesting thread of the error.
    Operation op = operationMap.get(value.getCorrelationId());
    if (op == null) {
      log.error("[ConnectionService] errorEvent can't find outstanding operation for correlationId = {}",
              correlationId);

      // Looks like this is a spontaneous event we need to handle.  These are
      // all dataplane related events so we should terminate the reservation
      // since there is no way to inform the SENSE orchestrator of the issue
      // at the moment.
      switch (errorEvent.getEvent()) {
        case ACTIVATE_FAILED:
        case DEACTIVATE_FAILED:
        case DATAPLANE_ERROR:
        case FORCED_END:
        default:
          log.error("[ConnectionService] errorEvent decision to terminate cid = {}, originating cid = {}",
                  cid, errorEvent.getOriginatingConnectionId());

          // TODO: Send a terminate for this reservation since it has gone bad.
          TerminateRequest req = new TerminateRequest("ConnectionService:errorEvent",
              errorEvent.getConnectionId());
          raController.getModelAuditActor().tell(req, ActorRef.noSender());
          break;
      }

    } else {
      // Is there a situation where we need to terminate the reservation or
      // just let the requesting thread handle it?
      op.setState(StateType.failed);
      op.setException(errorEvent.getServiceException());
      op.getCompleted().release();
    }

     // Update any impacted reservations with error information.
    Collection<Reservation> reservations = reservationService.getByAnyConnectionId(providerNSA, cid);
    if (reservations == null || reservations.isEmpty()) {
      // We have not seen this reservation before so ignore it.
      log.error("[ConnectionService] errorEvent() could not find cid = {}", cid);
    } else {
      for (Reservation r : reservations) {
        log.info("[ConnectionService] errorEvent() updating cid = {}, reservation {}", cid, r.toString());

        if (reservationService.setErrorState(r.getId(), Reservation.ErrorState.NSIERROREVENT,
            String.format("errorEvent, cid = %s, errorEvent = %s", cid, error), System.currentTimeMillis()) != 1) {
          log.info("[ConnectionService] error updating cid = {}", cid);
        }

        //r.setErrorState(Reservation.ErrorState.NSIERROREVENT);
        //r.setErrorMessage(String.format("errorEvent, cid = %s, errorEvent = %s", cid, error));
        //r.setDiscovered(System.currentTimeMillis());
        //log.info("[ConnectionService] errorEvent() writing updated cid = {}, reservation {}", cid, r.toString());
        //reservationService.store(r);
      }
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the autonomous NSI CS DataPlaneStateChange message.
   *
   * @param dataPlaneStateChange
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType dataPlaneStateChange(
          DataPlaneStateChangeRequestType dataPlaneStateChange,
          Holder<CommonHeaderType> header) throws ServiceException {

    String providerNSA = header.value.getProviderNSA();
    String connectionId = dataPlaneStateChange.getConnectionId();
    DataPlaneStatusType dataPlaneStatus = dataPlaneStateChange.getDataPlaneStatus();

    try {
      log.info("[ConnectionService] dataPlaneStateChange for providerNSA = {}, correlationId = {}, dataPlaneStateChange:\n{}",
              providerNSA,
              header.value.getCorrelationId(),
              CsParser.getInstance().dataPlaneStateChange2xml(dataPlaneStateChange));
    } catch (JAXBException ex) {
      log.error("[ConnectionService] dataPlaneStateChange could not encode log message.", ex);
    }

    // This state change is in the context of the local providerNSA so we must
    // assume we are directly connect to a uPA in order for us to map this
    // incoming event to the associated connection.  If we are connected to an
    // aggregator then the connectionId we want is actually a child connection.
    // Find the associated connection.
    //Reservation r = reservationService.get(providerNSA, connectionId);

    Collection<Reservation> reservations = reservationService.getByAnyConnectionId(providerNSA, connectionId);
    if (reservations == null || reservations.isEmpty()) {
      // We have not seen this reservation before so ignore it.
      log.error("[ConnectionService] dataPlaneStateChange could not find connectionId = {}", connectionId);
    } else {
      for (Reservation r : reservations) {
        log.info("[ConnectionService] updating connectionId = {}, dataPlaneStatus = {}",
            connectionId, dataPlaneStatus.isActive());
        if (reservationService.setDataPlaneActive(r.getId(), dataPlaneStatus.isActive(),
                System.currentTimeMillis()) != 1) {
          log.error("[ConnectionService] failed to update connectionId = {}, id = {}", connectionId, r.getId());
        }

        // We just did something successfully so invoke a model audit to
        // generate an updated version.
        log.info("[ConnectionService] dataPlaneStateChange: issuing audit for topologyId={}", r.getTopologyId());
        AuditRequest req = new AuditRequest("ConnectionService:dataPlaneStateChange", r.getTopologyId());
        raController.getModelAuditActor().tell(req, ActorRef.noSender());
      }
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the autonomous NSI CS ReserveTimeout message.
   *
   * @param reserveTimeout
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType reserveTimeout(ReserveTimeoutRequestType reserveTimeout,
          Holder<CommonHeaderType> header) throws ServiceException {

    // Need these to the look up the reservation in our database.
    String providerNSA = header.value.getProviderNSA();
    String connectionId = reserveTimeout.getConnectionId();
    String xml = reserveTimeoutRequest2xml(reserveTimeout);

    log.error("[ConnectionService] reserveTimeout for providerNSA = {}, correlationId = {}, reserveTimeout:\n{}",
              providerNSA,
              header.value.getCorrelationId(),
              xml);

    Collection<Reservation> reservations = reservationService.getByAnyConnectionId(providerNSA, connectionId);
    if (reservations == null || reservations.isEmpty()) {
      log.error("[ConnectionService] reserveTimeout could not find cid = {}", connectionId);
    } else {
      for (Reservation r : reservations) {
        log.info("[ConnectionService] reserveTimeout cid = {}, reservation {}", connectionId, r.toString());

        // Transition this reservation to reserve timeout.
        if (reservationService.setFailedState(r.getId(), ReservationStateEnumType.RESERVE_TIMEOUT,
            LifecycleStateEnumType.FAILED, Reservation.ErrorState.NSIRESERVETIMEOUT,
            String.format("reserveTimeout on cid = %s, %s", connectionId, xml), System.currentTimeMillis()) != 1) {
          log.info("[ConnectionService] error updating cid = {}", connectionId);
        }

        // TODO: When OpenNSA fixes their timeout notification state machine issue this
        // failed hack can be removed.
        //r.setReservationState(ReservationStateEnumType.RESERVE_TIMEOUT);
        //r.setLifecycleState(LifecycleStateEnumType.FAILED); <--- HACK
        //r.setErrorState(Reservation.ErrorState.NSIRESERVETIMEOUT);
        //r.setErrorMessage(String.format("reserveTimeout on cid = %s, %s", connectionId, xml));
        //r.setDiscovered(System.currentTimeMillis());
        //log.info("[ConnectionService] writing updated connectionId = {}, reservation {}", connectionId, r.toString());
        //reservationService.store(r);
      }
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   * Endpoint receiving the autonomous NSI CS MessageDeliveryTimeout message.
   *
   * @param messageDeliveryTimeout
   * @param header
   * @return
   * @throws ServiceException
   */
  public GenericAcknowledgmentType messageDeliveryTimeout(
          MessageDeliveryTimeoutRequestType messageDeliveryTimeout,
          Holder<CommonHeaderType> header) throws ServiceException {

    CommonHeaderType value = header.value;
    String cid = messageDeliveryTimeout.getConnectionId();

    log.info("[ConnectionService] messageDeliveryTimeout recieved for correlationId = {}, connectionId: {}",
            value.getCorrelationId(), cid);

    Operation op = operationMap.get(value.getCorrelationId());
    if (op == null) {
      log.error("[ConnectionService] messageDeliveryTimeout can't find outstanding operation for correlationId = {}",
              value.getCorrelationId());
    } else {
      op.setState(StateType.failed);
      ServiceExceptionType sex = new ServiceExceptionType();
      sex.setNsaId(value.getProviderNSA());
      sex.setText("messageDeliveryTimeout received");
      sex.setConnectionId(cid);
      op.setException(sex);
      op.getCompleted().release();
    }

    Collection<Reservation> reservations = reservationService.getByAnyConnectionId(value.getProviderNSA(), cid);

    if (reservations == null || reservations.isEmpty()) {
      log.error("[ConnectionService] messageDeliveryTimeout could not find cid = {}", cid);
    } else {
      for (Reservation r : reservations) {
        log.info("[ConnectionService] messageDeliveryTimeout on cid = {}, reservation {}", cid, r.toString());

        // Transition this reservation to reserve timeout.
        if (reservationService.setFailedState(r.getId(), ReservationStateEnumType.RESERVE_TIMEOUT,
            LifecycleStateEnumType.FAILED, Reservation.ErrorState.NSIMESSAGEDELIVERYTIMEOUT,
            String.format("messageDeliveryTimeout on cid = %s, correlationId = %s", cid, value.getCorrelationId()),
            System.currentTimeMillis()) != 1) {
          log.info("[ConnectionService] error updating cid = {}", cid);
        }

        // TODO: When OpenNSA fixes their timeout notification state machine issue this
        // failed hack can be removed.
        //r.setReservationState(ReservationStateEnumType.RESERVE_TIMEOUT);
        //r.setLifecycleState(LifecycleStateEnumType.FAILED);
        //r.setErrorState(Reservation.ErrorState.NSIMESSAGEDELIVERYTIMEOUT);
        //r.setErrorMessage(String.format("messageDeliveryTimeout on cid = %s, correlationId = %s",
        //        cid, value.getCorrelationId()));
        //r.setDiscovered(System.currentTimeMillis());
        //
        //log.info("[ConnectionService] messageDeliveryTimeout writing updated connectionId = {}, reservation {}",
        //        cid, r.toString());
        //reservationService.store(r);
      }
    }

    return FACTORY.createGenericAcknowledgmentType();
  }

  /**
   *
   * @param error
   * @return
   */
  public String errorEvent2xml(ErrorEventType error) {
    try {
      return CsParser.getInstance().errorEvent2xml(error);
    } catch (JAXBException ex) {
      log.error("[getErrorEventType] exception formatting ErrorEventType", ex);
      return "<exception/>";
    }
  }

  /**
   *
   * @param reserveTimeout
   * @return
   */
  public String reserveTimeoutRequest2xml(ReserveTimeoutRequestType reserveTimeout) {
    try {
      return CsParser.getInstance().reserveTimeoutRequest2xml(reserveTimeout);
    } catch (JAXBException ex) {
      log.error("[reserveTimeoutRequest2xml] exception formatting ReserveTimeoutRequestType", ex);
      return "<exception/>";
    }
  }
}
