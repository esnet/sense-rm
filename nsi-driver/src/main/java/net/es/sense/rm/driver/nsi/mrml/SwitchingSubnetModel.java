package net.es.sense.rm.driver.nsi.mrml;

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
import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class SwitchingSubnetModel {

  private final ReservationService reservationService;
  private final NmlModel nml;
  private final String topologyId;
  private final Map<String, ServiceDefinitionType> serviceDefinitions;

  // Will contain a map of serviceHolder indexed by SwitchingService identifier.
  Map<String, ServiceHolder> serviceHolder = new HashMap<>();

  private long version = 0;

  public SwitchingSubnetModel(ReservationService reservationService, NmlModel nml, String topologyId) throws IllegalArgumentException {
    this.reservationService = reservationService;
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

      ss.getAny().stream().forEach(any -> {
          log.info("[SwitchingSubnetModel] any = {}", any.getClass().getCanonicalName());
      });

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
      log.info("[SwitchingSubnetModel] processing reservation cid = {}", reservation.getConnectionId());

      // We only know about the EVTS service at this point.
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

          // Create the source port.
          SimpleStp src = new SimpleStp(p2ps.getSourceSTP());
          NmlPort srcParent = nml.getPort(src.getId());
          if (srcParent == null) {
            log.error("[SwitchingSubnetModel] Could not find parent port for STP {}", p2ps.getSourceSTP());
            continue;
          }

          NmlPort srcChildPort = NmlPort.builder()
                  .id(src.getMrmlId() + ":cid:" + reservation.getConnectionId())
                  .topologyId(reservation.getTopologyId())
                  .name(Optional.of(reservation.getConnectionId()))
                  .orientation(Orientation.child)
                  .parentPort(Optional.of(srcParent.getId()))
                  .encoding(srcParent.getEncoding())
                  .interfaceMTU(srcParent.getInterfaceMTU())
                  .granularity(srcParent.getGranularity())
                  .maximumReservableCapacity(capacity)
                  .minimumReservableCapacity(srcParent.getMinimumReservableCapacity())
                  .startTime(startTime)
                  .endTime(endTime)
                  .build();

          srcChildPort.setNmlLabels(src);


          // Create the destination port.
          SimpleStp dst = new SimpleStp(p2ps.getDestSTP());
          NmlPort dstParent = nml.getPort(dst.getId());
          if (dstParent == null) {
            log.error("[SwitchingSubnetModel] Could not find parent port for STP {}", p2ps.getDestSTP());
            continue;
          }
          NmlPort dstChildPort = NmlPort.builder()
                  .id(dst.getMrmlId() + ":cid:" + reservation.getConnectionId())
                  .topologyId(reservation.getTopologyId())
                  .name(Optional.of(reservation.getConnectionId()))
                  .orientation(Orientation.child)
                  .parentPort(Optional.of(dstParent.getId()))
                  .encoding(dstParent.getEncoding())
                  .interfaceMTU(dstParent.getInterfaceMTU())
                  .granularity(dstParent.getGranularity())
                  .maximumReservableCapacity(capacity)
                  .minimumReservableCapacity(dstParent.getMinimumReservableCapacity())
                  .startTime(startTime)
                  .endTime(endTime)
                  .build();

          dstChildPort.setNmlLabels(src);

          // We can only build a SwitchingSubnet if this service contains two endpoints in the same network.
          if (!src.getNetworkId().equalsIgnoreCase(dst.getNetworkId())) {
            log.error("[SwitchingSubnetModel] Reservation using STP on two different networks src = {}, dst = {}",
                    src, dst);
            continue;
          }

          // Add to the parent port.
          srcParent.getChildren().add(srcChildPort.getId());
          nml.addPort(srcChildPort);

          log.info("[SwitchingSubnetModel] adding src child port {}", srcChildPort.getId());

          // Add to the parent port.
          dstParent.getChildren().add(dstChildPort.getId());
          nml.addPort(dstChildPort);

          log.info("[SwitchingSubnetModel] adding dst child port {}", dstChildPort.getId());

          // Now build the SwitchingSubnet.
          List<NmlSwitchingServiceType> srcSS = srcParent.getSwitchingServices();
          Optional<String> srcSSid = Optional.empty();
          for (NmlSwitchingServiceType t : srcSS) {
            log.info("[SwitchingSubnetModel] checking source SwitchingService {}", t.getId());
            ServiceHolder sh = serviceHolder.get(t.getId());
            if (reservation.getServiceType().equalsIgnoreCase(sh.getServiceType())) {
              // We found the SwitchingService we were looking for.
              srcSSid = Optional.ofNullable(t.getId());
              log.info("[SwitchingSubnetModel] Matched source SwitchingService {}", t.getId());
            }
          }

          List<NmlSwitchingServiceType> dstSS = dstParent.getSwitchingServices();
          Optional<String> dstSSid = Optional.empty();
          for (NmlSwitchingServiceType t : dstSS) {
            log.info("[SwitchingSubnetModel] checking destination SwitchingService {}", t.getId());
            ServiceHolder sh = serviceHolder.get(t.getId());
            if (reservation.getServiceType().compareToIgnoreCase(sh.getServiceType()) == 0) {
              // We found the SwitchingService we were looking for.
              dstSSid = Optional.ofNullable(t.getId());
              log.info("Matched dest SwitchingService {}", t.getId());
            }
          }

          log.info("[SwitchingSubnetModel] srcSSid = {}, dstSSid = {}", srcSSid, dstSSid);

          if (!srcSSid.orElse("srcSSid").equalsIgnoreCase(dstSSid.orElse("dstSSid"))) {
            log.error("[SwitchingSubnetModel] Reservation using STP from two different SwitchingService src = {}, dst = {}",
                    src, dst);
            continue;
          }

          ServiceHolder holder = serviceHolder.get(srcSSid.get());

          NmlSwitchingSubnet nss = new NmlSwitchingSubnet();
          nss.setSwitchingService(holder.getSwitchingService());
          nss.getPorts().add(srcChildPort);
          nss.getPorts().add(dstChildPort);
          nss.setServiceId(reservation.getConnectionId());
          nss.setStartTime(startTime);
          nss.setEndTime(endTime);
          nss.setTopologyId(reservation.getTopologyId());
          nss.setId(NmlSwitchingSubnet.id(reservation.getTopologyId(), reservation.getConnectionId()));
          nss.setDiscovered(reservation.getDiscovered());
          holder.getSwitchingSubnets().add(nss);
          log.info("[SwitchingSubnetModel] adding SwitchingSubnet = {}", nss.getId());

          if (version < reservation.getDiscovered()) {
            version = reservation.getDiscovered();
          }

        } catch (JAXBException ex) {
          log.error("Could not parse P2PS structure for conenctionId = {}", reservation.getConnectionId());
        }
      } else {
        log.error("Unknown serviceType for conenctionId = {}, serviceType = {}", reservation.getConnectionId(),
                reservation.getServiceType());
      }
    }
  }
}
