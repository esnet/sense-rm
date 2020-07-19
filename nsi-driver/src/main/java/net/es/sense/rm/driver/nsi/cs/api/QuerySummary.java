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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import org.apache.jena.ext.com.google.common.base.Strings;
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
  private final String networkId;
  private final ReservationService reservationService;

  public QuerySummary(String networkId, ReservationService reservationService) {
    this.networkId = networkId;
    this.reservationService = reservationService;
  }

  public void process(QuerySummaryConfirmedType querySummaryConfirmed, Holder<CommonHeaderType> header) throws ServiceException {
    // Get the providerNSA identifier.
    String providerNsa = header.value.getProviderNSA();

    // Extract the uPA connection segments associated with individual networks.
    List<QuerySummaryResultType> reservations = querySummaryConfirmed.getReservation();
    log.debug("[QuerySummary] providerNSA = {}, # of reservations = {}",
            providerNsa, reservations.size());

    // Process each reservation returned.
    List<Reservation> results = new ArrayList<>();
    for (QuerySummaryResultType reservation : reservations) {
      // Get the parent reservation information to apply to child connections.
      ReservationStateEnumType reservationState = reservation.getConnectionStates().getReservationState();
      ProvisionStateEnumType provisionState = reservation.getConnectionStates().getProvisionState();
      LifecycleStateEnumType lifecycleState = reservation.getConnectionStates().getLifecycleState();
      DataPlaneStatusType dataPlaneStatus = reservation.getConnectionStates().getDataPlaneStatus();

      log.debug("[QuerySummary] cid = {}, gid = {}, decription = {}, rstate = {}, pstate = {}, "
              + "lstate = {}, active = {}, providerNSA = {}",
              reservation.getConnectionId(), reservation.getGlobalReservationId(), reservation.getDescription(),
              reservationState, provisionState, lifecycleState, dataPlaneStatus.isActive(), providerNsa);

      if (reservationState == null) {
        reservationState = ReservationStateEnumType.RESERVE_START;
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
        log.error("[QuerySummary] criteria is empty for cid = {}", reservation.getConnectionId());

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
        log.debug("[QuerySummary] storing new reservation, cid = {}, discovered = {}",
                reservation.getConnectionId(), reservation.getDiscovered());
        reservationService.store(reservation);
      } else if (r.diff(reservation)) {
        // We have to determine if the stored reservation needs to be updated.
        log.debug("[QuerySummary] storing updated reservation, cid = {}, discovered = {}",
                reservation.getConnectionId(), reservation.getDiscovered());
        reservation.setId(r.getId());
        reservationService.store(reservation);
      }
      else {
        log.debug("[QuerySummary] reservation no change, cid = {}", reservation.getConnectionId());
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
    reservation.setTopologyId(networkId);
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
    // will check to see if there are any more recent versions than what has
    // already been stored.
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
        reservation.setStartTime(CsUtils.getStartTime(criteria.getSchedule().getStartTime()));
        reservation.setEndTime(CsUtils.getEndTime(criteria.getSchedule().getEndTime()));

        // There is a history of incorrect EVTS URN so we need to cover all of them.
        if (Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_1.equalsIgnoreCase(criteria.getServiceType()) ||
                Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_2.equalsIgnoreCase(criteria.getServiceType()) ||
                Nsi.NSI_SERVICETYPE_EVTS_OSCARS.equalsIgnoreCase(criteria.getServiceType())) {
          reservation.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
        } else {
          reservation.setServiceType(criteria.getServiceType());
        }

        // Now we need to get the P2PS structure (add other services here when defined).
        try {
          CsUtils.ResultEnum result = CsUtils.serializeP2PS(reservation.getServiceType(), criteria.getAny(), reservation);

          if (CsUtils.ResultEnum.NOTFOUND == result) {
            // We could not find a P2PS structure so
            log.info("[QuerySummary] unable to locate P2PS structure, cid = {}, serviceType = {}",
                    cid, reservation.getServiceType());
            continue;
          } else if (CsUtils.ResultEnum.MISMATCH == result) {
            log.info("[QuerySummary] STP with networkId mismatch in P2PS structure, cid = {}, serviceType = {}",
                    cid, reservation.getServiceType());
            continue;
          }
        } catch (JAXBException ex) {
          log.error("[QuerySummary] serializeP2PS failed for cid = {}", cid, ex);
          continue;
        }

        // Before we add this reservation to the results check to see if it is
        // in the network we are managing.
        if (!networkId.equalsIgnoreCase(reservation.getTopologyId())) {
          log.info("[QuerySummary] processSummaryCriteria: rejecting reservation cid = {}, for topologyId = {}",
                  reservation.getConnectionId(), reservation.getTopologyId());
          continue;
        }

        results.add(reservation);
      } else {
        // We still have children so this must be an aggregator.  Build a list of
        // child reservations matching our topology identifier.
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

          if (Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_1.equalsIgnoreCase(child.getServiceType()) ||
                  Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_2.equalsIgnoreCase(child.getServiceType()) ||
                  Nsi.NSI_SERVICETYPE_EVTS_OSCARS.equalsIgnoreCase(child.getServiceType())) {
            reservation.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
          } else {
            reservation.setServiceType(child.getServiceType());
          }

          reservation.setReservationState(reservationState);
          reservation.setProvisionState(provisionState);
          reservation.setLifecycleState(lifecycleState);
          reservation.setDataPlaneActive(dataPlaneStatus.isActive());
          reservation.setStartTime(CsUtils.getStartTime(criteria.getSchedule().getStartTime()));
          reservation.setEndTime(CsUtils.getEndTime(criteria.getSchedule().getEndTime()));

          // Now we need to determine the network based on the STP used in the service.
          try {
            CsUtils.ResultEnum result = CsUtils.serializeP2PS(child.getServiceType(), child.getAny(), reservation);
            if (CsUtils.ResultEnum.NOTFOUND == result) {
              log.info("[QuerySummary] processSummaryCriteria: serializeP2PS failed for reservation cid = {}",
                      reservation.getConnectionId());
              continue;
            } else if (CsUtils.ResultEnum.MISMATCH == result) {
              log.info("[QuerySummary] processSummaryCriteria: STP with networkId mismatch in P2PS structure, cid = {}, serviceType = {}",
                      cid, reservation.getServiceType());
              continue;
            }

            if (Strings.isNullOrEmpty(reservation.getTopologyId())) {
              reservation.setTopologyId(networkId);
            } else if (!networkId.equalsIgnoreCase(reservation.getTopologyId())) {
              log.info("[QuerySummary] processSummaryCriteria: rejecting reservation cid = {}, for topologyId = {}",
                      reservation.getConnectionId(), reservation.getTopologyId());
              continue;
            }
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
