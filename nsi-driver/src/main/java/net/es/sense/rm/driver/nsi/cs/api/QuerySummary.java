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
import net.es.nsi.cs.lib.CsParser;
import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import net.es.sense.rm.driver.nsi.cs.db.ReservationAudit;
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
 * This class provides logic for creating and storing reservation objects
 * from the QuerySummaryConfirmedType.
 *
 * @author hacksaw
 */
@Slf4j
public class QuerySummary {
  private final String networkId;
  private final ReservationService reservationService;

  /**
   * Constructor for class.
   *
   * @param networkId The network identifier for the topology this SENSE-NSI-RM is managing.
   * @param reservationService The reservation database.
   */
  public QuerySummary(String networkId, ReservationService reservationService) {
    this.networkId = networkId;
    this.reservationService = reservationService;
  }

  /**
   *
   * @param querySummaryConfirmed
   * @param header
   * @throws ServiceException
   */
  public void process(QuerySummaryConfirmedType querySummaryConfirmed,
          Holder<CommonHeaderType> header) throws ServiceException {

    // Get the providerNSA identifier.
    String providerNsa = header.value.getProviderNSA();

    // Extract the uPA reservation (connection) segments associated with individual networks.
    List<QuerySummaryResultType> reservations = querySummaryConfirmed.getReservation();

    try {
      log.debug("[QuerySummary] providerNSA = {}, # of reservations = {}, querySummaryConfirmed:\n{}",
              providerNsa, reservations.size(),
              CsParser.getInstance().querySummaryConfirmed2xml(querySummaryConfirmed));
    } catch (JAXBException ex) {
      log.error("[QuerySummary] providerNSA = {} failed to encode QuerySummaryConfirmedType", providerNsa, ex);
    }

    // Process each reservation returned from the NSA.
    List<Reservation> results = new ArrayList<>();
    for (QuerySummaryResultType reservation : reservations) {
      // Get the parent reservation information to apply to child connections.
      ReservationStateEnumType reservationState = reservation.getConnectionStates().getReservationState();
      ProvisionStateEnumType provisionState = reservation.getConnectionStates().getProvisionState();
      LifecycleStateEnumType lifecycleState = reservation.getConnectionStates().getLifecycleState();
      DataPlaneStatusType dataPlaneStatus = reservation.getConnectionStates().getDataPlaneStatus();

      log.debug("[QuerySummary] incoming providerNSA = {}, {}", providerNsa, getQuerySummaryResultType(reservation));

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

    // TODO: Remove this once we fix the OpenNSA reservation issue.
    log.debug("[QuerySummary] pre-add reservation database contents...");
    for (Reservation reservation : reservationService.get()) {
      log.debug("[QuerySummary] {}", reservation);
    }

    // Determine if we need to update each reservation in the database.
    ReservationAudit rAudit = new ReservationAudit();
    for (Reservation reservation : results) {
      // Save this reservation for the later database contents audit.
      rAudit.add(reservation.getProviderNsa(), reservation.getConnectionId());

      // Look for this returned reservation in the existing reservation database.
      Reservation r = reservationService.get(reservation.getProviderNsa(), reservation.getConnectionId());
      if (r == null) {
        // We have not seen this reservation before so store it.
        log.debug("[QuerySummary] storing new reservation, cid = {}, discovered = {}",
                reservation.getConnectionId(), reservation.getDiscovered());
        reservationService.store(reservation);
      } else if (r.getLifecycleState() == LifecycleStateEnumType.TERMINATED) {
        // We want to make sure we do not undo a terminated/failed state (OpenNSA bug).
        log.debug("[QuerySummary] skipping update for a terminated " +
                "reservation, cid = {}, discovered = {},\n    update = {},\n    existing = {}",
                reservation.getConnectionId(), reservation.getDiscovered(), reservation, r);
      } else if (r.getLifecycleState() == LifecycleStateEnumType.FAILED) {
        log.debug("[QuerySummary] skipping update for a failed " +
                "reservation, cid = {}, discovered = {},\n    update = {},\n    existing = {}",
                reservation.getConnectionId(), reservation.getDiscovered(), reservation, r);
      } else if (r.diff(reservation)) {
        // We have to determine if the stored reservation needs to be updated.
        log.debug("[QuerySummary] storing updated reservation, " +
                "cid = {}, discovered = {},\n    update = {},\n    existing = {}",
                reservation.getConnectionId(), reservation.getDiscovered(), reservation, r);
        reservation.setId(r.getId());
        reservationService.store(reservation);
      }
      else {
        log.debug("[QuerySummary] reservation no change, cid = {}", reservation.getConnectionId());
      }
    }

    // TODO: Remove this once we fix the OpenNSA reservation issue.
    log.debug("[QuerySummary] before audit reservation database contents...");
    for (Reservation reservation : reservationService.get()) {
      log.debug("[QuerySummary] {}", reservation);
    }

    // TODO: Remove this once we fix the OpenNSA reservation issue.
    log.debug("[QuerySummary] audit connection contents {}", rAudit.toString());

    // We now know all the reservation we can see on this providerNSA.  Go
    // through our reservation database a deleted any that are no longer
    // present on the NSA.
    rAudit.audit(reservationService);

    // TODO: Remove this once we fix the OpenNSA reservation issue.
    log.debug("[QuerySummary] after audit reservation database contents...");
    for (Reservation reservation : reservationService.get()) {
      log.debug("[QuerySummary] {}", reservation);
    }
  }

  /**
   * Build a reservation object when no criteria was specified.
   *
   * @param providerNsa
   * @param gid
   * @param description
   * @param cid
   * @param reservationState
   * @param provisionState
   * @param lifecycleState
   * @param dataPlaneStatus
   * @return
   */
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

  /**
   * Build one or more reservation objects based on NSI reservation criteria.
   * An aggregator may have many child reservations that we would like to
   * track if it matches the topology we are interested in modeling.
   *
   * @param providerNsa
   * @param gid
   * @param description
   * @param cid
   * @param reservationState
   * @param provisionState
   * @param lifecycleState
   * @param dataPlaneStatus
   * @param criteriaList The list of zero or more child reservations.
   * @return
   */
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

    log.info("[QuerySummary] processSummaryCriteria: gid = {}, cid = {}, description = {}. providerNsa = {}",
            gid, cid, description, providerNsa);

    List<Reservation> results = new ArrayList<>();

    // There will be one criteria for each version of this reservation. We
    // will check to see if there are any more recent versions than what has
    // already been stored.
    for (QuerySummaryResultCriteriaType criteria : criteriaList) {
      log.info("[QuerySummary] processSummaryCriteria: cid = {}, version = {}, serviceType = {}",
              cid, criteria.getVersion(), criteria.getServiceType());

      // If this reservation was created by us then we will have this stored in
      // the database so we will need to update.  This could create two entries
      // one for the parent and one for the child reservation.
      Reservation parent = buildReservation(
              providerNsa,
              gid,
              description,
              cid,
              reservationState,
              provisionState,
              lifecycleState,
              dataPlaneStatus,
              criteria.getServiceType(),
              criteria);

      if (parent != null) {
        results.add(parent);
      }

      ChildSummaryListType children = criteria.getChildren();
      if (children != null && !children.getChild().isEmpty()) {
        // We still have children so this must be an aggregator.  Build a list of
        // child reservations matching our topology identifier.
        log.info("[QuerySummary] processSummaryCriteria: processing child criteria.");

        for (ChildSummaryType child : children.getChild()) {
          log.info("[QuerySummary] processSummaryCriteria: child cid = {}, gid = {}, decription = {}, " +
                          "rstate = {}, lstate = {}",
                  child.getConnectionId(), gid, description, reservationState, lifecycleState);

          Reservation reservation = buildReservation(
                  providerNsa, // We will mark this child as if came from the aggregator.
                  gid,
                  description,
                  child.getConnectionId(),
                  reservationState,
                  provisionState,
                  lifecycleState,
                  dataPlaneStatus,
                  child.getServiceType(),
                  criteria);

          if (reservation != null) {
            reservation.setParentConnectionId(cid); // We are a child connection so add the parent connectionId.
            results.add(reservation);
          }
        }
      }
    }

    return results;
  }

  /**
   * Build the reservation storage object.
   *
   * @param providerNsa
   * @param gid
   * @param description
   * @param cid
   * @param reservationState
   * @param provisionState
   * @param lifecycleState
   * @param dataPlaneStatus
   * @param serviceType
   * @param criteria
   * @return
   */
  private Reservation buildReservation(
          String providerNsa,
          String gid,
          String description,
          String cid,
          ReservationStateEnumType reservationState,
          ProvisionStateEnumType provisionState,
          LifecycleStateEnumType lifecycleState,
          DataPlaneStatusType dataPlaneStatus,
          String serviceType,
          QuerySummaryResultCriteriaType criteria) {

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
      reservation.setStartTime(CsUtils.getStartTime(criteria.getSchedule().getStartTime()));
      reservation.setEndTime(CsUtils.getEndTime(criteria.getSchedule().getEndTime()));
      reservation.setDiscovered(System.currentTimeMillis());


      // There is a history of incorrect EVTS URN so we need to cover all of them.
      if (Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_1.equalsIgnoreCase(serviceType) ||
              Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_2.equalsIgnoreCase(serviceType) ||
              Nsi.NSI_SERVICETYPE_EVTS_OSCARS.equalsIgnoreCase(serviceType)) {
        reservation.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
      } else {
        reservation.setServiceType(serviceType);
      }

      // Now we need to get the P2PS structure (add other services here when defined).
      try {
        CsUtils.ResultEnum result = CsUtils.serializeP2PS(reservation.getServiceType(),
                criteria.getAny(), reservation);

        if (CsUtils.ResultEnum.NOTFOUND == result) {
          // We could not find a P2PS structure so
          log.info("[QuerySummary] unable to locate P2PS structure, cid = {}, serviceType = {}",
                  cid, reservation.getServiceType());
          return null;
        } else if (CsUtils.ResultEnum.MISMATCH == result) {
          log.info("[QuerySummary] STP with networkId mismatch in P2PS structure, cid = {}, serviceType = {}",
                  cid, reservation.getServiceType());
          return null;
        }
      } catch (JAXBException ex) {
        log.error("[QuerySummary] serializeP2PS failed for cid = {}", cid, ex);
        return null;
      }

      // Before we add this reservation to the results check to see if it is
      // in the network we are managing.
      if (!networkId.equalsIgnoreCase(reservation.getTopologyId())) {
        log.info("[QuerySummary] processSummaryCriteria: rejecting reservation cid = {}, for topologyId = {}",
                reservation.getConnectionId(), reservation.getTopologyId());
        return null;
      }

      return reservation;
  }

  /**
   * Encodes the query results into a string for debug.
   *
   * @param query
   * @return
   */
  public String getQuerySummaryResultType(QuerySummaryResultType query) {
    StringBuilder result = new StringBuilder("QuerySummaryResultType: ");
    try {
      result.append(CsParser.getInstance().qsrt2xml(query));
    } catch (JAXBException ex) {
      log.error("[QuerySummary] exception formatting QuerySummaryResultType", ex);
      result.append("<exception>");
    }
    return result.toString();
  }
}
