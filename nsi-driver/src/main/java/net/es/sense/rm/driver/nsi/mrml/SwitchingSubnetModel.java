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
package net.es.sense.rm.driver.nsi.mrml;

import static java.util.Comparator.comparing;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.google.common.base.Strings;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.cs.lib.CsParser;
import net.es.nsi.cs.lib.SimpleStp;
import net.es.nsi.dds.lib.jaxb.nml.NmlSwitchingServiceType;
import net.es.nsi.dds.lib.jaxb.nml.ServiceDefinitionType;
import net.es.sense.rm.driver.nsi.cs.db.ConnectionMap;
import net.es.sense.rm.driver.nsi.cs.db.ConnectionMapService;
import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import net.es.sense.rm.driver.nsi.cs.db.StpMapping;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;
import org.ogf.schemas.nsi._2013._12.services.types.TypeValueType;

/**
 * Create MRML SwitchingSubnet model from stored NSI reservations and connection maps.
 *
 * @author hacksaw
 */
@Slf4j
public class SwitchingSubnetModel {

  private final ReservationService reservationService; // The reservation database.
  private final ConnectionMapService connectionMapService; // The connection map database.
  private final NmlModel nml; // The currently discovered NML models from the DDS.
  private final String topologyId; // The topologyId for network we are modelling.
  private final Map<String, ServiceDefinitionType> serviceDefinitions; // Map from URI to service definition.

  // Will contain a map of serviceHolder indexed by SwitchingService identifier.
  Map<String, ServiceHolder> serviceHolder = new HashMap<>();

  private long version = 0; // Will hold the version of this model once computed.

  /**
   * Constructor for the SwitchingSubnetModel initializes with needed database services
   * and source models.  Also, initial processing of models is performed.
   *
   * @param reservationService The reservation database.
   * @param connectionMapService The connection map database.
   * @param nml The currently discovered NML models from the DDS.
   * @param topologyId The topologyId for network we are modelling.
   * @throws IllegalArgumentException If an invalid topologyId is provided.
   */
  public SwitchingSubnetModel(ReservationService reservationService,
          ConnectionMapService connectionMapService, NmlModel nml,
          String topologyId) throws IllegalArgumentException {
    this.reservationService = reservationService;
    this.connectionMapService = connectionMapService;
    this.nml = nml;
    this.topologyId = topologyId;
    this.serviceDefinitions = nml.getServiceDefinitionsIndexed(topologyId);

    load();
  }

  /**
   * Get the version of the current computed MRML model.
   *
   * @return The version of the computed MRML model.
   */
  public long getVersion() {
    return version;
  }

  /**
   * Get the map of serviceHolder (containing switchingService related information)
   * indexed by SwitchingService identifier for this topology.
   *
   * @return Map of serviceHolder.
   */
  public Map<String, ServiceHolder> getServiceHolder() {
    return serviceHolder;
  }

  /**
   * Get the topology identifier for the SwitchingSubnet modelled by this class.
   *
   * @return The string topology identifier for the SwitchingSubnet modelled by this class.
   */
  public String getTopologyId() {
    return topologyId;
  }

  /**
   * Get the NML model instance holding the currently discovered NML models from the DDS.
   *
   * @return The NML model instance.
   */
  public NmlModel getNmlModel() {
    return nml;
  }

  /**
   * During construction this method loads all dependent data models.  This implies that
   * the version of model generated is versioned at construction time and not when the
   * created model is accessed.
   */
  private void load() {
    log.debug("[SwitchingSubnetModel] loading...");

    // Pull out ServiceDefinitions from SwitchingServices and link Bidirectional ports.
    processSwitchingService();

    // Convert NSI-CS reservations to SwitchingSubnets.
    processReservations();

    log.debug("[SwitchingSubnetModel] completed loading.");
  }

  /**
   * Process the JAXB representation of the NML SwitchingService elements populating
   * the serviceHolder map for easy access.
   */
  private void processSwitchingService() {
    // Pull out ServiceDefinitions from the SwitchingServices.
    nml.getSwitchingServices(topologyId).forEach((ss) -> {
      log.info("[SwitchingSubnetModel] processing SwitchingService {}", ss.getId());

      ss.getAny().stream()
              .filter((object) -> (object instanceof JAXBElement))
              .map((object) -> (JAXBElement) object)
              .filter((jaxb) -> (jaxb.getValue() instanceof ServiceDefinitionType))
              .map((jaxb) -> (ServiceDefinitionType) jaxb.getValue())
              .forEachOrdered((sd) -> {
                ServiceDefinitionType servdef = serviceDefinitions.get(sd.getId());
                // We have a ServiceDefinition against this SwitchingService.
                ServiceHolder sh = new ServiceHolder.ServiceHolderBuilder()
                        .serviceType(servdef.getServiceType())
                        .serviceDefinition(servdef)
                        .switchingService(ss)
                        .build();
                serviceHolder.put(ss.getId(), sh);
              });

      // Now link all the bidirectional port members back to their containing SwitchingService.
      nml.getBidirectionalPortIdFromSwitchingService(ss).forEach((p) -> {
        Optional<NmlPort> ofNullable = Optional.ofNullable(nml.getPort(p));
        ofNullable.ifPresent(np -> np.getSwitchingServices().add(ss));
      });
    });
  }

  /**
   * Get the list of active reservations we will need to model in MRML.
   *
   * @return The list of active reservations.
   */
  private List<Reservation> getActiveReservations() {
    // In this new version supporting modifications we must account for reservations
    // with multiple versions, some of which are not yet valid.  Let us try some
    // pre-filtering magic to get reservations from a specific topology that are in
    // the RESERVE_START state with a lifecycle state of CREATED or TERMINATING.
    // Remove any duplicate reservations (sharing a connectionId) by keeping the
    // reservation of the highest version.
    return reservationService.getByTopologyId(topologyId).stream()
        .filter(r -> r.getReservationState() == ReservationStateEnumType.RESERVE_START && (r.getLifecycleState() == LifecycleStateEnumType.CREATED || r.getLifecycleState() == LifecycleStateEnumType.TERMINATING))
        .collect(Collectors.groupingBy(Reservation::getConnectionId, Collectors.maxBy(Comparator.comparing(Reservation::getVersion))))
        .values().stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  /**
   * Convert the NSI CS reservations into MRML SwitchingSubnet elements and
   * link to parent SwitchingService.
   */
  private void processReservations() {
    // Process NSI connections adding SwitchingSubnets and associated child
    // ports associated with the parent bidirectional port.
    log.debug("processReservations: loading topologyId = {}", topologyId);

    // In this new version supporting modifications we must account for reservations
    // with multiple versions, some of which are not yet valid.  Let us try some
    // pre-filtering magic to get reservations from a specific topology that are in
    // the RESERVE_START state with a lifecycle state of CREATED or TERMINATING.
    // Remove any duplicate reservations (sharing a connectionId) by keeping the
    // reservation of the highest version.
    for (Reservation reservation : getActiveReservations()) {
      log.info("[SwitchingSubnetModel] processing reservation\n{}", reservation.toString());

      // If we had a model change then make sure to update our version.
      if (version < reservation.getDiscovered()) {
        version = reservation.getDiscovered();
        log.info("[SwitchingSubnetModel] new version discovered reservation cid = {}", reservation.getConnectionId());
      }

      // Skip generating model for reservation if it is expired.
      switch (reservation.getReservationState()) {
        case RESERVE_FAILED, RESERVE_ABORTING, RESERVE_TIMEOUT -> {
          log.info("[SwitchingSubnetModel] skipping reservation cid = {}, reservationState = {}",
              reservation.getConnectionId(), reservation.getReservationState());
          continue;
        }
        default -> {
          log.info("[SwitchingSubnetModel] processing reservation cid = {}, reservationState = {}",
              reservation.getConnectionId(), reservation.getReservationState());
        }
      }

      // We only need to model those reservations in the "created" state.
      if (reservation.getLifecycleState() != LifecycleStateEnumType.CREATED &&
              reservation.getLifecycleState() != LifecycleStateEnumType.TERMINATING) {
        log.info("[SwitchingSubnetModel] skipping reservation cid = {}, lifecycleState = {}",
                reservation.getConnectionId(), reservation.getLifecycleState());
        continue;
      }

      // One last test before processing this reservation... if this is a child reservation,
      // and the parent reservation is present in the database, then we skip the child
      // reservation.  The parent reservation will only be present if the src and dst STP
      // are in our topology.  If either of the src or dst were not in out topology then we
      // discarded the reservation.
      if (!Strings.isNullOrEmpty(reservation.getParentConnectionId())) {
        // We have a parent connection id so see if it is in the DB.
        Collection<Reservation> parent = reservationService.getByProviderNsaAndConnectionId(reservation.getProviderNsa(),
            reservation.getParentConnectionId());
        String parentConnectionId = parent.stream().max(comparing(Reservation::getVersion))
            .map(Reservation::getConnectionId).orElse(null);
        if (parentConnectionId != null) {
          log.info("[SwitchingSubnetModel] skipping child reservation cid = {}, lifecycleState = {}, parentId = {}",
                reservation.getConnectionId(), reservation.getLifecycleState(), parentConnectionId);
          continue;
        }
      }

      // The globalReservationId holds the original SwitchingSubnet name, while
      // the description holds a unique identifier for the connection created by
      // us before the connectionId is assigned by the PA.
      log.info("[SwitchingSubnetModel] Testing for stored connection, gid = {}, cid = {}, description = {}",
              reservation.getGlobalReservationId(), reservation.getConnectionId(), reservation.getDescription());

      // Look up the connection map corresponding to this reservation.
      Optional<ConnectionMap> connMap = Optional.ofNullable(connectionMapService.getByUniqueId(reservation.getUniqueId()));
      if (connMap.isEmpty() && !Strings.isNullOrEmpty(reservation.getDescription())) {
        // We probably do not need this special case, but just in case there was a race condition,
        // and we lost the uniqueId in our reservation.
        log.info("[SwitchingSubnetModel] no stored connection map for uniqueId = {}, gid = {}, cid = {}, description = {}",
            reservation.getUniqueId(), reservation.getGlobalReservationId(), reservation.getConnectionId(),
            reservation.getDescription());
        connMap = Optional.ofNullable(connectionMapService.getByUniqueId(reservation.getDescription()));
      }

      // We may have a mapping to a different serviceType so use it if available.
      String serviceType;
      if (connMap.isPresent()) {
        serviceType = connMap.get().getServiceType();
        log.info("[SwitchingSubnetModel] we created this reservation serviceType = {}, connMap = {}",
                serviceType, connMap);
      } else {
        serviceType = reservation.getServiceType();
        log.info("[SwitchingSubnetModel] we did not created this reservation serviceType = {}", serviceType);
      }

      // ServiceType will be missing if the reservation does not contain reservation criteria.
      if (serviceType == null) {
        log.error("[SwitchingSubnetModel] skipping reservation due to missing serviceType cid = {}",
                reservation.getConnectionId());
        continue;
      }

      // We only know about the EVTS service at this point so ignore anything
      // else.
      String st = reservation.getServiceType().trim();
      if (Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(st) ||
              Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_1.equalsIgnoreCase(st) ||
              Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_2.equalsIgnoreCase(st) ||
              Nsi.NSI_SERVICETYPE_EVTS_OSCARS.equalsIgnoreCase(st)) {

        // Normalize to a single service type.
        reservation.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);

        log.info("[SwitchingSubnetModel] processing EVTS service");

        if (reservation.getService() == null) {
          log.error("[SwitchingSubnetModel] skipping reservation due to missing service criteria cid = {}",
                  reservation.getConnectionId());
          continue;
        }

        try {
          // We have two tasks here: 1. We need to create child bidirectional
          // ports to identify the resources used by this reservation. 2. We
          // need to create a SwitchingSubnet linked to the correct
          // SwitchingService with member bidirectional ports.
          P2PServiceBaseType p2ps = CsParser.getInstance().xml2p2ps(reservation.getService());
          Optional<Long> startTime = reservation.getStartTime() == 0 ? Optional.empty() : Optional.of(reservation.getStartTime());
          Optional<Long> endTime = reservation.getEndTime() == Long.MAX_VALUE ? Optional.empty() : Optional.of(reservation.getEndTime());
          Optional<Long> capacity = Optional.of(p2ps.getCapacity() * 1000000);

          Optional<NmlPort> srcChildPort = createChildPort(
                  reservation.getTopologyId(),
                  reservation.getConnectionId(),
                  p2ps.getSourceSTP(),
                  connMap,
                  startTime,
                  endTime,
                  capacity
          );

          // No nml port if consolidation with parent port is not possible.
          if (srcChildPort.isEmpty()) {
            log.error("[SwitchingSubnetModel] Could not find parent port for STP {}", p2ps.getSourceSTP());
            continue;
          }

          Optional<NmlPort> dstChildPort = createChildPort(
                  reservation.getTopologyId(),
                  reservation.getConnectionId(),
                  p2ps.getDestSTP(),
                  connMap,
                  startTime,
                  endTime,
                  capacity
          );

          // No nml port if consolidation with parent port is not possible.
          if (dstChildPort.isEmpty()) {
            log.error("[SwitchingSubnetModel] Could not find parent port for STP {}", p2ps.getDestSTP());
            continue;
          }

          // We can only build a SwitchingSubnet if this service contains two endpoints in the same network.
          if (!srcChildPort.get().getTopologyId().equalsIgnoreCase(dstChildPort.get().getTopologyId())) {
            log.error("[SwitchingSubnetModel] Reservation using STP on two different networks src = {}, dst = {}",
                    srcChildPort, dstChildPort);
            continue;
          }

          // Now build the SwitchingSubnet.
          if (srcChildPort.get().getParentPort().isEmpty()) {
            log.error("[SwitchingSubnetModel] Parent port missing src = {}", srcChildPort);
            continue;
          }
          Optional<String> srcSSid = createSwitchingSubnet(srcChildPort.get().getParentPort().get(), serviceType);

          if (dstChildPort.get().getParentPort().isEmpty()) {
            log.error("[SwitchingSubnetModel] Parent port missing dst = {}", dstChildPort);
            continue;
          }
          Optional<String> dstSSid = createSwitchingSubnet(dstChildPort.get().getParentPort().get(), serviceType);

          log.info("[SwitchingSubnetModel] srcSSid = {}, dstSSid = {}", srcSSid, dstSSid);

          if (!srcSSid.orElse("srcSSid").equalsIgnoreCase(dstSSid.orElse("dstSSid"))) {
            log.error("[SwitchingSubnetModel] Reservation using STP from two different SwitchingService src = {}, dst = {}",
                    p2ps.getSourceSTP(), p2ps.getDestSTP());
            continue;
          }

          // Check to see of there is a custom OSCARS connectionId in the P2P structure.
          String oscarsId = p2ps.getParameter().stream()
              .filter(p -> "oscarsId".equalsIgnoreCase(p.getType()))
              .map(TypeValueType::getValue)
              .findFirst()
              .orElse(null);

          ServiceHolder holder = serviceHolder.get(srcSSid.get());

          // If this is a SwitchingSubnet created by an MRML delta request
          // we will have a specific mapping name for this.
          String nssId;
          String nssExistsDuringId = null;
          if (connMap.isPresent()) {
            nssId = connMap.get().getSwitchingSubnetId();
            nssExistsDuringId = connMap.get().getExistsDuringId();
          } else {
            nssId = NmlSwitchingSubnet.id(reservation.getTopologyId(), ConnectionId.strip(reservation.getConnectionId()));
          }

          if (Strings.isNullOrEmpty(nssExistsDuringId)) {
            nssExistsDuringId = NmlExistsDuring.id(nssId);
          }

          NmlSwitchingSubnet nss = new NmlSwitchingSubnet();
          nss.setSwitchingService(holder.getSwitchingService());
          nss.getPorts().add(srcChildPort.get());
          nss.getPorts().add(dstChildPort.get());
          nss.setConnectionId(reservation.getConnectionId());
          nss.setExistsDuringId(nssExistsDuringId);
          nss.setStartTime(startTime);
          nss.setEndTime(endTime);
          nss.setTopologyId(reservation.getTopologyId());
          nss.setId(nssId);
          nss.setDiscovered(reservation.getDiscovered());
          nss.setTag("connectionId=" + topologyId + ":cid+" + ConnectionId.strip(reservation.getConnectionId()));
          nss.setStatus(NetworkStatus.parse(reservation));
          nss.setOscarsId(oscarsId);
          nss.setErrorState(reservation.getErrorState());
          nss.setErrorMessage(reservation.getErrorMessage());
          holder.getSwitchingSubnets().add(nss);
          log.info("[SwitchingSubnetModel] adding SwitchingSubnet = {}", nss.getId());
        } catch (JAXBException ex) {
          log.error("Could not parse P2PS structure for conenctionId = {}", reservation.getConnectionId(), ex);
        }
      } else {
        log.error("Unknown serviceType for conenctionId = {}, serviceType = {}", reservation.getConnectionId(),
                reservation.getServiceType());
      }
    }
  }

  /**
   * Create a child port under a the parent bidirectional port by using NSI connection information.
   *
   * @param topologyId
   * @param connectionId
   * @param stp
   * @param connMap
   * @param startTime
   * @param endTime
   * @param capacity
   * @return
   */
  private Optional<NmlPort> createChildPort(
          String topologyId,
          String connectionId,
          String stp,
          Optional<ConnectionMap> connMap,
          Optional<Long> startTime,
          Optional<Long> endTime,
          Optional<Long> capacity) {

    // Create the source port.
    SimpleStp simpleStp = new SimpleStp(stp);
    NmlPort stpParent = nml.getPort(simpleStp.getId());
    if (stpParent == null) {
      log.error("[SwitchingSubnetModel] Could not find parent port for STP {}", stp);
      return Optional.empty();
    }

    // If we have a specific MRML mapping stored then we need to use its
    // naming scheme.
    Optional<StpMapping> stpMapping = Optional.empty();
    if (connMap.isPresent()) {
      stpMapping = connMap.get().findMapping(simpleStp.getStpId());
    }

    // We will name our port based on the stored mapping, otherwise we
    // make one up based on the NSI connectionId.
    String childPortId;
    Optional<String> childPortBwId = Optional.empty();
    Optional<String> childPortLabelId = Optional.empty();
    Optional<String> childPortExistsDuringId = Optional.empty();
    if (stpMapping.isPresent()) {
      childPortId = stpMapping.get().getMrsPortId();
      childPortBwId = Optional.ofNullable(stpMapping.get().getMrsBandwidthId());
      childPortLabelId = Optional.ofNullable(stpMapping.get().getMrsLabelId());
      childPortExistsDuringId = Optional.ofNullable(stpMapping.get().getNmlExistsDuringId());
    } else {
      childPortId = simpleStp.getMrmlId() + ":cid+" + ConnectionId.strip(connectionId);
    }

    if (!childPortExistsDuringId.isPresent()) {
      childPortExistsDuringId = Optional.of(NmlExistsDuring.id(childPortId));
    }

    NmlPort childPort = NmlPort.builder()
            .id(childPortId)
            .topologyId(topologyId)
            .mrsBandwidthId(childPortBwId)
            .mrsLabelId(childPortLabelId)
            .nmlExistsDuringId(childPortExistsDuringId)
            .name(Optional.of(connectionId))
            .orientation(Orientation.child)
            .parentPort(Optional.of(stpParent.getId()))
            .encoding(stpParent.getEncoding())
            .interfaceMTU(stpParent.getInterfaceMTU())
            .type(stpParent.getType())
            .granularity(stpParent.getGranularity())
            .maximumCapacity(capacity) // This would be maximumCapacity of parent for soft cap service.
            .minimumCapacity(stpParent.getMinimumCapacity()) //
            .reservableCapacity(capacity)
            .availableCapacity(capacity)
            .individualCapacity(stpParent.getIndividualCapacity())
            .startTime(startTime)
            .endTime(endTime)
            .build();

    childPort.setNmlLabels(simpleStp);

    // Add to the parent port.
    stpParent.getChildren().add(childPort.getId());
    nml.addPort(childPort);
    return Optional.of(childPort);
  }

  private Optional<String> createSwitchingSubnet(String portId, String serviceType) {
    // Now build the SwitchingSubnet.
    Optional<String> ssId = Optional.empty();

    NmlPort parent = nml.getPort(portId);
    List<NmlSwitchingServiceType> ss = parent.getSwitchingServices();

    for (NmlSwitchingServiceType t : ss) {
      log.info("[SwitchingSubnetModel] checking source SwitchingService {}", t.getId());
      ServiceHolder sh = serviceHolder.get(t.getId());
      if (serviceType.equalsIgnoreCase(sh.getServiceType())) {
        // We found the SwitchingService we were looking for.
        ssId = Optional.ofNullable(t.getId());
        log.info("[SwitchingSubnetModel] Matched source SwitchingService {}", t.getId());
      }
    }

    return ssId;
  }
}
