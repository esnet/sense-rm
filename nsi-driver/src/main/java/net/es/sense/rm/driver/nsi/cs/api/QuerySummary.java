/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.sense.rm.driver.nsi.cs.api;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.ChildSummaryListType;
import org.ogf.schemas.nsi._2013._12.connection.types.ChildSummaryType;
import org.ogf.schemas.nsi._2013._12.connection.types.DataPlaneStatusType;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ProvisionStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryResultCriteriaType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryResultType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;
import org.ogf.schemas.nsi._2013._12.framework.headers.CommonHeaderType;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class QuerySummary {

  private final ReservationService reservationService;

  public QuerySummary(ReservationService reservationService) {
    this.reservationService = reservationService;
  }

  public void process(QuerySummaryConfirmedType querySummaryConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    // Get the providerNSA identifier.
    String providerNsa = header.value.getProviderNSA();

    // Extract the uPA connection segments associated with individual networks.
    List<QuerySummaryResultType> reservations = querySummaryConfirmed.getReservation();
    log.info("[QuerySummary] providerNSA = {}, # of reservations = {}",
            providerNsa, reservations.size());

    // Process each reservation returned.
    List<Reservation> results = new ArrayList<>();
    for (QuerySummaryResultType reservation : reservations) {
      // Get the parent reservation information to apply to child connections.
      ReservationStateEnumType reservationState = reservation.getConnectionStates().getReservationState();
      ProvisionStateEnumType provisionState = reservation.getConnectionStates().getProvisionState();
      LifecycleStateEnumType lifecycleState = reservation.getConnectionStates().getLifecycleState();
      DataPlaneStatusType dataPlaneStatus = reservation.getConnectionStates().getDataPlaneStatus();

      log.info("[QuerySummary] cid = {}, gid = {}, decription = {}, rstate = {}, pstate = {}, "
              + "lstate = {}, active = {}, providerNSA = {}",
              reservation.getConnectionId(), reservation.getGlobalReservationId(), reservation.getDescription(),
              reservationState, provisionState, lifecycleState, dataPlaneStatus.isActive(), providerNsa);

      if (reservationState == null) {
        reservationState = ReservationStateEnumType.RESERVE_CHECKING;
      }

      if (provisionState == null) {
        provisionState = ProvisionStateEnumType.RELEASED;
      }

      if (lifecycleState == null) {
        lifecycleState = LifecycleStateEnumType.INITIAL;
      }

      // If this reservation is in the process of being created, or failed
      // creation, then there will be no associated criteria.
      if (reservation.getCriteria().isEmpty()) {
        results.add(processReservationNoCriteria(
                providerNsa,
                reservation.getGlobalReservationId(),
                reservation.getDescription(),
                reservation.getConnectionId(),
                reservationState,
                provisionState,
                lifecycleState,
                dataPlaneStatus));
      } else {
        results.addAll(processSummaryCriteria(
                providerNsa,
                reservation.getGlobalReservationId(),
                reservation.getDescription(),
                reservation.getConnectionId(),
                reservationState,
                provisionState,
                lifecycleState,
                dataPlaneStatus,
                reservation.getCriteria()));
      }
    }

    // Determine if we need to update each reservation in the database.
    for (Reservation reservation : results) {
      Reservation r = reservationService.get(reservation.getProviderNsa(), reservation.getConnectionId());
      if (r == null) {
        // We have not seen this reservation before so store it.
        log.info("[QuerySummary] storing new reservation, cid = {}",
                reservation.getConnectionId());
        reservationService.store(reservation);
      } else if (r.diff(reservation)) {
        // We have to determine if the stored reservation needs to be updated.
        log.info("[QuerySummary] storing reservation update, cid = {}",
                reservation.getConnectionId());
        reservation.setId(r.getId());
        reservationService.store(reservation);
      } else {
        log.info("[QuerySummary] reservation no change, cid = {}",
                reservation.getConnectionId());
      }
    }
  }

  private Reservation processReservationNoCriteria(
          String providerNsa,
          String gid,
          String description,
          String cid,
          ReservationStateEnumType reservationState,
          ProvisionStateEnumType provisionState,
          LifecycleStateEnumType lifecycleState,
          DataPlaneStatusType dataPlaneStatus) {

    log.debug("[QuerySummary] processReservationNoCriteria: cid = {}, providerNSA = {}", cid, providerNsa);

    // We have had a state change so update the reservation.
    Reservation reservation = new Reservation();
    reservation.setGlobalReservationId(gid);
    reservation.setDescription(description);
    reservation.setProviderNsa(providerNsa);
    reservation.setConnectionId(cid);
    reservation.setVersion(0);
    reservation.setReservationState(reservationState);
    reservation.setProvisionState(provisionState);
    reservation.setLifecycleState(lifecycleState);
    reservation.setDataPlaneActive(dataPlaneStatus.isActive());
    reservation.setDiscovered(System.currentTimeMillis());

    return reservation;
  }

  private List<Reservation> processSummaryCriteria(
          String providerNsa,
          String gid,
          String description,
          String cid,
          ReservationStateEnumType reservationState,
          ProvisionStateEnumType provisionState,
          LifecycleStateEnumType lifecycleState,
          DataPlaneStatusType dataPlaneStatus,
          List<QuerySummaryResultCriteriaType> criteriaList) {

    log.info("[QuerySummary] processSummaryCriteria: connectionId = {}, providerNsa = {}", cid, providerNsa);

    List<Reservation> results = new ArrayList<>();

    // There will be one criteria for each version of this reservation. We
    // will check to see if there are any new versions than what is already
    // stored.
    for (QuerySummaryResultCriteriaType criteria : criteriaList) {
      log.info("[QuerySummary] processSummaryCriteria: cid = {}, version = {}, serviceType = {}",
              cid, criteria.getVersion(), criteria.getServiceType());
      ChildSummaryListType children = criteria.getChildren();
      if (children == null || children.getChild().isEmpty()) {
        // We are at a leaf child so check to see if we need to store this reservation information.
        Reservation reservation = new Reservation();
        reservation.setGlobalReservationId(gid);
        reservation.setDescription(description);
        reservation.setDiscovered(System.currentTimeMillis());
        reservation.setProviderNsa(providerNsa);
        reservation.setConnectionId(cid);
        reservation.setReservationState(reservationState);
        reservation.setProvisionState(provisionState);
        reservation.setLifecycleState(lifecycleState);
        reservation.setDataPlaneActive(dataPlaneStatus.isActive());
        reservation.setVersion(criteria.getVersion());
        reservation.setServiceType(criteria.getServiceType());
        reservation.setStartTime(CsUtils.getStartTime(criteria.getSchedule().getStartTime()));
        reservation.setEndTime(CsUtils.getEndTime(criteria.getSchedule().getEndTime()));

        // Now we need to determine the network based on the STP used in the service.
        try {
          CsUtils.serializeP2PS(criteria.getServiceType(), criteria.getAny(), reservation);
        } catch (JAXBException ex) {
          log.error("[QuerySummary] serializeP2PS failed for cid = {}", cid, ex);
          continue;
        }

        results.add(reservation);
      } else {
        // We still have children so this must be an aggregator.
        for (ChildSummaryType child : children.getChild()) {
          log.info("[QuerySummary] processSummaryCriteria: child cid = {}, gid = {}, decription = {}, rstate = {}, lstate = {}",
                  child.getConnectionId(), gid, description, reservationState, lifecycleState);

          Reservation reservation = new Reservation();
          reservation.setDiscovered(System.currentTimeMillis());
          reservation.setGlobalReservationId(gid);
          reservation.setDescription(description);
          reservation.setProviderNsa(child.getProviderNSA());
          reservation.setConnectionId(child.getConnectionId());
          reservation.setVersion(criteria.getVersion());
          reservation.setServiceType(child.getServiceType());
          reservation.setReservationState(reservationState);
          reservation.setProvisionState(provisionState);
          reservation.setLifecycleState(lifecycleState);
          reservation.setDataPlaneActive(dataPlaneStatus.isActive());
          reservation.setStartTime(CsUtils.getStartTime(criteria.getSchedule().getStartTime()));
          reservation.setEndTime(CsUtils.getEndTime(criteria.getSchedule().getEndTime()));

          // Now we need to determine the network based on the STP used in the service.
          try {
            CsUtils.serializeP2PS(child.getServiceType(), child.getAny(), reservation);
          } catch (JAXBException ex) {
            log.error("[ConnectionService] processSummaryCriteria: failed for connectionId = {}",
                    reservation.getConnectionId(), ex);
            continue;
          }

          results.add(reservation);
        }
      }
    }

    return results;
  }

}