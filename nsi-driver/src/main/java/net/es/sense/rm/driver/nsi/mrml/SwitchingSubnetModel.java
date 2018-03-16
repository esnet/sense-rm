package net.es.sense.rm.driver.nsi.mrml;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
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
import org.apache.jena.ext.com.google.common.base.Strings;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class SwitchingSubnetModel {

  private final ReservationService reservationService;
  private final ConnectionMapService connectionMapService;
  private final NmlModel nml;
  private final String topologyId;
  private final Map<String, ServiceDefinitionType> serviceDefinitions;

  // Will contain a map of serviceHolder indexed by SwitchingService identifier.
  Map<String, ServiceHolder> serviceHolder = new HashMap<>();

  private long version = 0;

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

  public long getVersion() {
    return version;
  }

  public Map<String, ServiceHolder> getServiceHolder() {
    return serviceHolder;
  }

  public String getTopologyId() {
    return topologyId;
  }

  public NmlModel getNmlModel() {
    return nml;
  }

  private void load() {
    log.debug("[SwitchingSubnetModel] loading...");

    // Pull out ServiceDefinitions from SwtichingServices and link Bidirectional ports.
    processSwitchingService();

    // Convert reservations to SwitchingSubnets.
    processReservations();
  }

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
                log.info("[SwitchingSubnetModel] processing serviceDefinition {}", servdef.getId());
                // We have a ServiceDefinition against this SwitchingService.
                ServiceHolder sh = new ServiceHolder.ServiceHolderBuilder()
                        .serviceType(servdef.getServiceType())
                        .serviceDefinition(servdef)
                        .switchingService(ss)
                        .build();
                serviceHolder.put(ss.getId(), sh);
              });

      // Now link all the bidirection port members back to their containing SwitchingService.
      nml.getBidirectionalPortIdFromSwitchingService(ss).forEach((p) -> {
        log.info("[SwitchingSubnetModel] linking port {} to switchingService {}", p, ss.getId());
        Optional<NmlPort> ofNullable = Optional.ofNullable(nml.getPort(p));
        ofNullable.ifPresent(np -> np.getSwitchingServices().add(ss));
      });
    });
  }

  private void processReservations() {
    // Process NSI connections adding SwitchingSubnets and associated child
    // ports associated with the parent bidirectional port.
    for (Reservation reservation : reservationService.getByTopologyId(topologyId)) {
      log.info("[SwitchingSubnetModel] processing reservation cid = {}, gid = {}, description={}",
              reservation.getConnectionId(), reservation.getGlobalReservationId(), reservation.getDescription());

      // If we had a model change then make sure to update our version.
      if (version < reservation.getDiscovered()) {
        version = reservation.getDiscovered();
      }

      // Skip generating model for reservation if it is expired.
      switch (reservation.getReservationState()) {
        case RESERVE_FAILED:
        case RESERVE_ABORTING:
        case RESERVE_TIMEOUT:
          log.info("[SwitchingSubnetModel] skipping reservation cid = {}, reservationState = {}",
                  reservation.getConnectionId(), reservation.getReservationState());
          continue;
        default:
          break;
      }

      // We only need to model those reservations in the "created" state.
      if (reservation.getLifecycleState() != LifecycleStateEnumType.CREATED) {
        log.info("[SwitchingSubnetModel] skipping reservation cid = {}, lifecycleState = {}",
                reservation.getConnectionId(), reservation.getLifecycleState());
        continue;
      }

      // We will need to map any reservations made by SENSE back into the original  naming scheme.
      Collection<ConnectionMap> get = connectionMapService.get();
      log.info("connectionMapService contents:...");
      for (ConnectionMap c : get) {
        log.info("{}, {}, {}", c.getDescription(), c.getDeltaId(), c.getSwitchingSubnetId());
        for (StpMapping m : c.getMap()) {
          log.info(m.toString());
        }
      }

      // The GlobalReservationId hold the original SwitchingSubnet name, while
      // the description holds a unique identifier for the connection created by
      // us before the connectionId is assigned by the PA.
      log.info("Testing for stored connection, gid = {}, decription = {}",
              reservation.getGlobalReservationId(), reservation.getDescription());

      Optional<ConnectionMap> connMap = Optional.empty();
      if (!Strings.isNullOrEmpty(reservation.getDescription())) {
        connMap = Optional.ofNullable(connectionMapService.getByDescription(reservation.getDescription()));
      }

      log.info("[SwitchingSubnetModel] connMap = {}", connMap);

      // We only know about the EVTS service at this point so ignore anything
      // else.
      if (Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(reservation.getServiceType().trim())) {
        log.info("[SwitchingSubnetModel] processing EVTS service");
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
          if (!srcChildPort.isPresent()) {
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
          if (!dstChildPort.isPresent()) {
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
          Optional<String> srcSSid = createSwitchingSubnet(srcChildPort.get().getParentPort().get(),
                  reservation.getServiceType());

          Optional<String> dstSSid = createSwitchingSubnet(dstChildPort.get().getParentPort().get(),
                  reservation.getServiceType());

          log.info("[SwitchingSubnetModel] srcSSid = {}, dstSSid = {}", srcSSid, dstSSid);

          if (!srcSSid.orElse("srcSSid").equalsIgnoreCase(dstSSid.orElse("dstSSid"))) {
            log.error("[SwitchingSubnetModel] Reservation using STP from two different SwitchingService src = {}, dst = {}",
                    p2ps.getSourceSTP(), p2ps.getDestSTP());
            continue;
          }

          ServiceHolder holder = serviceHolder.get(srcSSid.get());

          // If this is a SwtichingSubnet created by an MRML delta request
          // we will have a specific mapping name for this.
          String nssId;
          if (connMap.isPresent()) {
            nssId = connMap.get().getSwitchingSubnetId();
          } else {
            nssId = NmlSwitchingSubnet.id(reservation.getTopologyId(), ConnectionId.strip(reservation.getConnectionId()));
          }

          NmlSwitchingSubnet nss = new NmlSwitchingSubnet();
          nss.setSwitchingService(holder.getSwitchingService());
          nss.getPorts().add(srcChildPort.get());
          nss.getPorts().add(dstChildPort.get());
          nss.setConnectionId(reservation.getConnectionId());
          nss.setStartTime(startTime);
          nss.setEndTime(endTime);
          nss.setTopologyId(reservation.getTopologyId());
          nss.setId(nssId);
          nss.setDiscovered(reservation.getDiscovered());
          nss.setTag("connectionId=" + topologyId + ":cid+" + ConnectionId.strip(reservation.getConnectionId()));
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
    if (stpMapping.isPresent()) {
      childPortId = stpMapping.get().getMrsPortId();
      childPortBwId = Optional.ofNullable(stpMapping.get().getMrsBandwidthId());
      childPortLabelId = Optional.ofNullable(stpMapping.get().getMrsLabelId());
    } else {
      childPortId = simpleStp.getMrmlId() + ":cid+" + ConnectionId.strip(connectionId);
    }

    NmlPort childPort = NmlPort.builder()
            .id(childPortId)
            .topologyId(topologyId)
            .mrsBandwidthId(childPortBwId)
            .mrsLabelId(childPortLabelId)
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

    // Adjust the parent port avaialble capacity to remove this port.
    // TODO: this is only temorary.  Adjustment for schedule handling will
    // change this behavior.
    if (stpParent.getAvailableCapacity().isPresent() && capacity.isPresent()) {
      long available = stpParent.getAvailableCapacity().get() - capacity.get();
      stpParent.setAvailableCapacity(Optional.of(available));
    }

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
