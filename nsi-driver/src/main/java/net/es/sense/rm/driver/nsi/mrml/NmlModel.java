package net.es.sense.rm.driver.nsi.mrml;

import com.twitter.common.base.Either;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.lib.jaxb.Relationships;
import net.es.nsi.dds.lib.jaxb.nml.NmlBidirectionalPortType;
import net.es.nsi.dds.lib.jaxb.nml.NmlLabelGroupType;
import net.es.nsi.dds.lib.jaxb.nml.NmlLabelType;
import net.es.nsi.dds.lib.jaxb.nml.NmlNetworkObject;
import net.es.nsi.dds.lib.jaxb.nml.NmlPortGroupRelationType;
import net.es.nsi.dds.lib.jaxb.nml.NmlPortGroupType;
import net.es.nsi.dds.lib.jaxb.nml.NmlPortRelationType;
import net.es.nsi.dds.lib.jaxb.nml.NmlPortType;
import net.es.nsi.dds.lib.jaxb.nml.NmlSwitchingServiceRelationType;
import net.es.nsi.dds.lib.jaxb.nml.NmlSwitchingServiceType;
import net.es.nsi.dds.lib.jaxb.nml.NmlTopologyRelationType;
import net.es.nsi.dds.lib.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.lib.jaxb.nml.ObjectFactory;
import net.es.nsi.dds.lib.jaxb.nml.ServiceDefinitionType;
import net.es.sense.rm.driver.nsi.dds.api.DocumentReader;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class NmlModel {

  private final static ObjectFactory FACTORY = new ObjectFactory();
  private final DocumentReader documentReader;
  private String defaultServiceType = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";
  private String defaultType = "guaranteedCapped";
  private String defaultUnits = "bps";
  private long defaultGranularity = 1L;
  private final Map<String, NmlPort> ports = new HashMap<>();

  public NmlModel(DocumentReader documentReader) {
    this.documentReader = documentReader;

    // Resolve all ports across all networks for later access.
    Collection<NmlTopologyType> topologies = documentReader.getNmlTopologyAll();
    for (NmlTopologyType nml : topologies) {
      log.info("[NmlModel] processing NML model {}", nml.getId());
      ports.putAll(getNmlPorts(nml));
    }

    // Consolidate isAlias entries in the bidirectional ports.
    ports.values().stream()
            .filter(p -> p.getOrientation() == Orientation.bidirectional)
            .forEach(p -> {
              NmlPort inboundPort = ports.get(p.getInboundPort().get());
              NmlPort outboundPort = ports.get(p.getOutboundPort().get());

              if (inboundPort.getIsAlias().isPresent() && outboundPort.getIsAlias().isPresent()) {
                String in = inboundPort.getIsAlias().get();
                String out = outboundPort.getIsAlias().get();

                Optional<NmlPort> remoteOut = Optional.ofNullable(ports.get(in));
                Optional<NmlPort> remoteIn = Optional.ofNullable(ports.get(out));

                // If the remote port's isAlias points back to our member ports...
                if (remoteOut.isPresent() && inboundPort.getId().equalsIgnoreCase(remoteOut.get().getIsAlias().get()) &&
                        remoteIn.isPresent() && outboundPort.getId().equalsIgnoreCase(remoteIn.get().getIsAlias().get())) {
                  p.setIsAlias(remoteOut.get().getParentPort());
                }
              }
            });
  }

  public void setDefaultServiceType(String defaultServiceType) {
    this.defaultServiceType = defaultServiceType;
  }

  public String getDefaultServiceType() {
    return defaultServiceType;
  }

  /**
   * @return the defaultType
   */
  public String getDefaultType() {
    return defaultType;
  }

  /**
   * @param defaultType the defaultType to set
   */
  public void setDefaultType(String defaultType) {
    this.defaultType = defaultType;
  }

  /**
   * @return the defaultUnits
   */
  public String getDefaultUnits() {
    return defaultUnits;
  }

  /**
   * @param defaultUnits the defaultUnits to set
   */
  public void setDefaultUnits(String defaultUnits) {
    this.defaultUnits = defaultUnits;
  }

  /**
   * @return the defaultGranularity
   */
  public long getDefaultGranularity() {
    return defaultGranularity;
  }

  public Collection<String> getTopologyIds() {
    Collection<String> results = new ArrayList<>();
    for (NmlTopologyType topology : documentReader.getNmlTopologyAll()) {
      results.add(topology.getId());
    }
    return results;
  }

  /**
   * @param defaultGranularity the defaultGranularity to set
   */
  public void setDefaultGranularity(long defaultGranularity) {
    this.defaultGranularity = defaultGranularity;
  }

  public Map<String, NmlPort> getPorts() {
    return ports;
  }

  public Map<String, NmlPort> getPorts(String topologyId) {
    return ports.values().stream()
            .filter(p -> p.getTopologyId().equalsIgnoreCase(topologyId))
            .collect(Collectors.toMap(p -> p.getId(), p -> p));
  }

  public Map<String, NmlPort> getPorts(String topologyId, Orientation orientation) {
    return ports.values().stream()
            .filter(p -> p.getTopologyId().equalsIgnoreCase(topologyId) && p.getOrientation() == orientation)
            .collect(Collectors.toMap(p -> p.getId(), p -> p));
  }

  public Optional<NmlTopologyType> getTopology(String topologyId) {
    return documentReader.getTopologyById(topologyId).stream().findFirst();
  }

  private Map<String, NmlPort> getNmlPorts(NmlTopologyType nmlTopology) {
    Map<String, NmlPort> portMap = new HashMap<>();

    // Unidirectional ports and SwitchingService are modelled using Relations.
    nmlTopology.getRelation().forEach((relation) -> {
      String type = relation.getType();
      if (type.equalsIgnoreCase(Relationships.HAS_OUTBOUND_PORT)) {
        // Some topologies use PortGroup to model a list of
        // unidirectional ports.
        relation.getPortGroup().forEach((portGroup) -> {
          portMap.put(portGroup.getId(), convertPortGroup(portGroup, Orientation.outbound, nmlTopology.getId()));
        });

        // Some topologies use Port to model unidirectional ports.
        relation.getPort().forEach((port) -> {
          portMap.put(port.getId(), convertPort(port, Orientation.outbound, nmlTopology.getId()));
        });
      } else if (type.equalsIgnoreCase(Relationships.HAS_INBOUND_PORT)) {
        // Some topologies use PortGroup to model a list of
        // unidirectional ports.
        relation.getPortGroup().forEach((portGroup) -> {
          portMap.put(portGroup.getId(), convertPortGroup(portGroup, Orientation.inbound, nmlTopology.getId()));
        });

        // Some topologies use Port to model unidirectional ports.
        relation.getPort().forEach((port) -> {
          portMap.put(port.getId(), convertPort(port, Orientation.inbound, nmlTopology.getId()));
        });
      }
    });

    // Bidirectional ports are stored in separate group element and
    // reference individual undirection Ports or PortGroups.
    List<NmlNetworkObject> groups = nmlTopology.getGroup();
    groups.stream()
            .filter((group) -> (group instanceof NmlBidirectionalPortType))
            .map((group) -> convertBidirectionalPort(NmlBidirectionalPortType.class.cast(group), nmlTopology.getId(), portMap))
            .filter((biPort) -> (biPort.isPresent()))
            .forEachOrdered((biPort) -> {
              portMap.put(biPort.get().getId(), biPort.get());
            });

    return portMap;
  }

  private NmlPort convertPortGroup(NmlPortGroupType portGroup, Orientation orientation, String topologyId) {

    log.debug("[NmlModel] processing portGroup {}", portGroup.getId());

    // Extract the labels associated with this port.  We
    // currently expect a single labelType with a range of
    // values.  VLAN labels is all we currently know about.
    Set<NmlLabelGroupType> labels = new LinkedHashSet<>();
    for (NmlLabelGroupType labelGroup : portGroup.getLabelGroup()) {
      // We break out the vlan specific label.
      //labelType = Optional.ofNullable(labelGroup.getLabeltype());
      //if (NmlEthernet.isVlanLabel(labelType)) {
      //  labels.addAll(NmlEthernet.labelGroupToLabels(labelGroup));
      //}
      labels.add(labelGroup);
    }

    // PortGroup relationship has isAlias connection information.
    Optional<String> isAlias = Optional.empty();
    int count = 0;
    for (NmlPortGroupRelationType pgRelation : portGroup.getRelation()) {
      if (Relationships.isAlias(pgRelation.getType())) {
        for (NmlPortGroupType alias : pgRelation.getPortGroup()) {
          isAlias = Optional.ofNullable(alias.getId());
          count++;
        }
      }
    }

    // Log an error if we have more than one isAlias
    // relationship but continue.
    if (count > 1) {
      log.error("[MrmlFactory] PortGroup found with multiple isAlias entries, id = {}, count = {}.", portGroup.getId(), Integer.toString(count));
    }

    // We need to pull out the individual Ethernet bandwidth attributes.
    NmlEthernet params = new NmlEthernet(portGroup.getAny());

    // Store this port information in our scratch pad.
    NmlPort.NmlPortBuilder builder = NmlPort.builder();
    builder.id(portGroup.getId())
            .topologyId(topologyId)
            .name(Optional.ofNullable(portGroup.getName()))
            .orientation(orientation)
            .encoding(Optional.ofNullable(portGroup.getEncoding()))
            .labels(labels)
            .isAlias(isAlias)
            .capacity(params.getCapacity())
            .granularity(params.getGranularity())
            .interfaceMTU(params.getInterfaceMTU())
            .maximumReservableCapacity(params.getMaximumReservableCapacity())
            .minimumReservableCapacity(params.getMinimumReservableCapacity())
            .port(Either.left(portGroup));
    return builder.build();
  }

  private NmlPort convertPort(NmlPortType port, Orientation orientation, String topologyId) {

    log.debug("[NmlModel] processing port {}", port.getId());

    // Port relationship has isAlias connection information.
    String isAlias = null;
    int count = 0;
    for (NmlPortRelationType pgRelation : port.getRelation()) {
      if (Relationships.isAlias(pgRelation.getType())) {
        for (NmlPortType alias : pgRelation.getPort()) {
          isAlias = alias.getId();
          count++;
        }
      }
    }

    // Log an error if we have more than one isAlias
    // relationship but continue.
    if (count > 1) {
      log.error("[MrmlFactory] Port found with multiple isAlias entries, id = {}, count = {}.", port.getId(), Integer.toString(count));
    }

    // Convert the port Label to a PortGroup of one.
    NmlLabelGroupType labelGroup = new NmlLabelGroupType();
    labelGroup.setLabeltype(port.getLabel().getLabeltype());
    labelGroup.setValue(port.getLabel().getValue());
    LinkedHashSet<NmlLabelGroupType> labels = new LinkedHashSet<>();
    labels.add(labelGroup);

    // We need to pull out the individual Ethernet bandwidth attributes.
    NmlEthernet params = new NmlEthernet(port.getAny());

    // Store this port information in our scratch pad.
    NmlPort.NmlPortBuilder builder = NmlPort.builder();
    builder.id(port.getId())
            .topologyId(topologyId)
            .name(Optional.ofNullable(port.getName()))
            .orientation(orientation)
            .encoding(Optional.ofNullable(port.getEncoding()))
            .labels(labels)
            .isAlias(Optional.ofNullable(isAlias))
            .capacity(params.getCapacity())
            .granularity(params.getGranularity())
            .interfaceMTU(params.getInterfaceMTU())
            .maximumReservableCapacity(params.getMaximumReservableCapacity())
            .minimumReservableCapacity(params.getMinimumReservableCapacity())
            .port(Either.right(port));

    return builder.build();
  }

  private Optional<NmlPort> convertBidirectionalPort(NmlBidirectionalPortType port, String topologyId, Map<String, NmlPort> portMap) {

    log.debug("[NmlModel] processing bidirectional port {}", port.getId());

    // Process port groups containing the unidirectional references.
    NmlPort inbound = null;
    NmlPort outbound = null;
    List<Object> rest = port.getRest();
    for (Object obj : rest) {
      if (obj instanceof JAXBElement) {
        JAXBElement<?> element = (JAXBElement<?>) obj;
        if (element.getValue() instanceof NmlPortGroupType) {
          NmlPortGroupType pg = (NmlPortGroupType) element.getValue();
          NmlPort tmp = portMap.get(pg.getId());
          if (tmp != null) {
            if (null == tmp.getOrientation()) {
              log.error("[MrmlFactory] BidirectionalPort has invalid member value, id = {} has invalid undirectional port group reference, pgId = {}, orientation = {}.", port.getId(), pg.getId(), tmp.getOrientation().toString());
            } else {
              switch (tmp.getOrientation()) {
                case inbound:
                  inbound = tmp;
                  break;
                case outbound:
                  outbound = tmp;
                  break;
                default:
                  log.error("[MrmlFactory] BidirectionalPort has invalid member value, id = {} has invalid undirectional port group reference, pgId = {}, orientation = {}.", port.getId(), pg.getId(), tmp.getOrientation().toString());
                  break;
              }
            }
          }
        } else if (element.getValue() instanceof NmlPortType) {
          NmlPortType p = (NmlPortType) element.getValue();
          NmlPort tmp = portMap.get(p.getId());
          if (tmp != null) {
            if (null == tmp.getOrientation()) {
              log.error("[MrmlFactory] BidirectionalPort has invalid member value, id = {} has invalid undirectional port group reference, pgId = {}, orientation = {}.", port.getId(), p.getId(), tmp.getOrientation().toString());
            } else {
              switch (tmp.getOrientation()) {
                case inbound:
                  inbound = tmp;
                  break;
                case outbound:
                  outbound = tmp;
                  break;
                default:
                  log.error("[MrmlFactory] BidirectionalPort has invalid member value, id = {} has invalid undirectional port group reference, pgId = {}, orientation = {}.", port.getId(), p.getId(), tmp.getOrientation().toString());
                  break;
              }
            }
          }
        }
      }
    }

    if (inbound == null) {
      log.error("[MrmlFactory] BidirectionalPort missing inbound port, id = {}.", port.getId());
      return Optional.empty();
    } else if (outbound == null) {
      log.error("[MrmlFactory] BidirectionalPort missing outbound port, id = {}.", port.getId());
      return Optional.empty();
    }

    // Merge the lables uni ports into bidirectional port.
    Set<NmlLabelGroupType> inLabels = inbound.getLabels();
    Set<NmlLabelGroupType> outLabels = outbound.getLabels();

    if (inLabels == null && outLabels == null) {
      // No labels is a valid case.
      log.info("[MrmlFactory] Bidirectional port {} has no labels for members {} and {} ", port.getId(), inbound.getId(), outbound.getId());
    } else if (inLabels != null && outLabels != null) {
      // Determine if the labels defined in the member unidirectional ports match.
      boolean consistent = true;
      for (NmlLabelGroupType inLabel : inLabels) {
        boolean found = false;
        for (NmlLabelGroupType outLabel : outLabels) {
          if (labelEquals(inLabel, outLabel)) {
            found = true;
            break;
          }
        }

        // We fail based on a simple string compare.  We could be less strict
        // and explode the label ranges then intersect the common labels for
        // use in this bidirectional port.  Perhaps in the future if this
        // becomes an issue.
        if (!found) {
          log.error("[MrmlFactory] Bidirectional port {} has a label range mismatch, type = {}, value = {}.", port.getId(), inLabel.getLabeltype(), inLabel.getValue());
          consistent = false;
          break;
        }
      }

      if (!consistent) {
        return Optional.empty();
      }
    } else if (inLabels == null) {
      log.error("[MrmlFactory] Bidirectional port {} has no inbound port labels.", port.getId());
      return Optional.empty();
    } else if (outLabels == null) {
      log.error("[MrmlFactory] Bidirectional port {} has no outbound port labels.", port.getId());
      return Optional.empty();
    }

    // Build the bidirection Port using unidirectional Labels
    // and store this port information in our scratch pad.
    NmlPort.NmlPortBuilder builder = NmlPort.builder();
    builder.id(port.getId())
            .topologyId(topologyId)
            .name(Optional.ofNullable(port.getName()))
            .encoding(inbound.getEncoding())
            .orientation(Orientation.bidirectional)
            .labels(inLabels)
            .capacity(outbound.getCapacity())
            .granularity(outbound.getGranularity())
            .interfaceMTU(outbound.getInterfaceMTU())
            .maximumReservableCapacity(outbound.getMaximumReservableCapacity())
            .minimumReservableCapacity(outbound.getMinimumReservableCapacity())
            .inboundPort(Optional.ofNullable(inbound.getId()))
            .outboundPort(Optional.ofNullable(outbound.getId()));

    // Update member ports.
    inbound.setParentPort(Optional.ofNullable(port.getId()));
    outbound.setParentPort(Optional.ofNullable(port.getId()));

    return Optional.ofNullable(builder.build());
  }

  /**
   *
   * @param a
   * @param b
   * @return
   */
  public static boolean labelEquals(NmlLabelType a, NmlLabelType b) {
    if (a == null && b == null) {
      return true;
    } else if (a == null) {
      return false;
    } else if (b == null) {
      return false;
    } else if (!a.getLabeltype().equalsIgnoreCase(b.getLabeltype())) {
      return false;
    }

    if (a.getValue() == null && b.getValue() == null) {
      return true;
    } else if (b.getValue() == null) {
      return false;
    } else if (a.getValue() == null) {
      return false;
    } else if (!a.getValue().equalsIgnoreCase(b.getValue())) {
      return false;
    }

    return true;
  }

  /**
   *
   * @param a
   * @param b
   * @return
   */
  public static boolean labelEquals(NmlLabelGroupType a, NmlLabelGroupType b) {
    if (a == null && b == null) {
      return true;
    } else if (a == null) {
      return false;
    } else if (b == null) {
      return false;
    } else if (!a.getLabeltype().equalsIgnoreCase(b.getLabeltype())) {
      return false;
    }

    if (a.getValue() == null && b.getValue() == null) {
      return true;
    } else if (b.getValue() == null) {
      return false;
    } else if (a.getValue() == null) {
      return false;
    } else if (!a.getValue().equalsIgnoreCase(b.getValue())) {
      return false;
    }

    return true;
  }

  /**
   * Get all the ServiceDefinitions associated with the specified topology. Will return a default ServiceDefinition if
   * one is not found.
   *
   * @param topologyId
   * @return
   */
  public Collection<ServiceDefinitionType> getServiceDefinitions(String topologyId) throws IllegalArgumentException {

    NmlTopologyType topology = getTopology(topologyId)
            .orElseThrow(new IllegalArgumentExceptionSupplier(
                    "[getServiceDefinition] Could not find NML document for networkId = " + topologyId));

    // Pull out the ServiceDefinition elements which are stored in ANY.
    Collection<ServiceDefinitionType> results = new ArrayList<>();
    for (Object object : topology.getAny()) {
      if (object instanceof JAXBElement) {
        JAXBElement<?> jaxb = (JAXBElement) object;
        if (jaxb.getValue() instanceof ServiceDefinitionType) {
          results.add(ServiceDefinitionType.class.cast(jaxb.getValue()));
        }
      }
    }

    // If no service definition was found create a default one.
    if (results.isEmpty()) {
      ServiceDefinitionType serviceDefinition = FACTORY.createServiceDefinitionType();
      serviceDefinition.setId(topology.getId() + ":ServiceDefinition:default");
      serviceDefinition.setServiceType(defaultServiceType);
      results.add(serviceDefinition);
    }

    return results;
  }

  private NmlSwitchingServiceType newNmlSwitchingService(String topologyId) throws IllegalArgumentException {
    NmlTopologyType topology = getTopology(topologyId)
            .orElseThrow(new IllegalArgumentExceptionSupplier(
                    "[getServiceDefinition] Could not find NML document for networkId = " + topologyId));

    NmlSwitchingServiceType switchingService = new NmlSwitchingServiceType();
    switchingService.setId(topology.getId() + ":SwitchingService:default");
    switchingService.setName("Default Switching Service");
    switchingService.setLabelSwapping(Boolean.FALSE);

    // Add the unidirectional port references.
    NmlSwitchingServiceRelationType hasInboundPort = new NmlSwitchingServiceRelationType();
    hasInboundPort.setType(Relationships.HAS_INBOUND_PORT);

    NmlSwitchingServiceRelationType hasOutboundPort = new NmlSwitchingServiceRelationType();
    hasOutboundPort.setType(Relationships.HAS_OUTBOUND_PORT);

    ports.values().stream().filter(p -> topologyId.equalsIgnoreCase(p.getTopologyId())).forEach(port -> {
      // Ignore the bidirectional ports.
      if (port.getOrientation() == Orientation.inbound) {
        NmlPortGroupType pg = new NmlPortGroupType();
        pg.setId(port.getId());
        hasInboundPort.getPortGroup().add(pg);
      } else if (port.getOrientation() == Orientation.outbound) {
        NmlPortGroupType pg = new NmlPortGroupType();
        pg.setId(port.getId());
        hasOutboundPort.getPortGroup().add(pg);
      }
    });

    switchingService.getRelation().add(hasInboundPort);
    switchingService.getRelation().add(hasOutboundPort);

    // Add a default service definition.
    ServiceDefinitionType serviceDefinition = FACTORY.createServiceDefinitionType();
    serviceDefinition.setId(topologyId + ":ServiceDefinition:default");
    serviceDefinition.setServiceType(defaultServiceType);
    switchingService.getAny().add(FACTORY.createServiceDefinition(serviceDefinition));
    return switchingService;
  }

  public Collection<NmlSwitchingServiceType> getSwitchingServices(String topologyId) throws IllegalArgumentException {
    NmlTopologyType topology = getTopology(topologyId)
            .orElseThrow(new IllegalArgumentExceptionSupplier(
                    "[getServiceDefinition] Could not find NML document for networkId = " + topologyId));

    Collection<NmlSwitchingServiceType> newSwitchingServices = new ArrayList<>();

    // The SwitchingService is modelled as a "hasService" relation.
    for (NmlTopologyRelationType relation : topology.getRelation()) {
      if (relation.getType().equalsIgnoreCase(Relationships.HAS_SERVICE)) {
        for (NmlNetworkObject service : relation.getService()) {
          // We want the SwitchingService.
          if (service instanceof NmlSwitchingServiceType) {
            newSwitchingServices.add((NmlSwitchingServiceType) service);
          }
        }
      }
    }

    // NML Default Behavior #1: Check to see if we have a SwitchingService
    // defined, and if not, create the default one with all ports and
    // labelSwapping set to false.
    if (newSwitchingServices.isEmpty()) {
      NmlSwitchingServiceType switchingService = newNmlSwitchingService(topologyId);
      newSwitchingServices.add(switchingService);

    } else {
      // NML Default Behavior #2: If we have a SwitchingService with no port
      // members then we must add all ports of matching label and encoding
      // type.  We use a boolean here to tell us if the SwitchingService
      // held a port.
      for (NmlSwitchingServiceType switchingService : newSwitchingServices) {
        boolean foundPort = false;
        for (NmlSwitchingServiceRelationType relation : switchingService.getRelation()) {
          if (Relationships.HAS_INBOUND_PORT.equalsIgnoreCase(relation.getType())) {
            foundPort = true;
            break;
          } else if (Relationships.HAS_OUTBOUND_PORT.equalsIgnoreCase(relation.getType())) {
            foundPort = true;
            break;
          }
        }

        if (!foundPort) {
          // Treat this as a wildcard SwitchingService buy adding all
          // unidirectional ports with maching attributes.
          populateWildcardSwitchingService(switchingService, topologyId);
        }
      }
    }

    return newSwitchingServices;
  }

  private NmlSwitchingServiceType populateWildcardSwitchingService(NmlSwitchingServiceType switchingService, String topologyId) {
    Optional<String> encoding = Optional.ofNullable(switchingService.getEncoding());
    Optional<String> labelType = Optional.ofNullable(switchingService.getLabelType());

    log.info("Found empty SwitchingService id= {} so populating based on wildcard rules, encoding = {}, labelType = {}.",
            switchingService.getId(), encoding, labelType);

    // Add the unidirectional port references.
    NmlSwitchingServiceRelationType hasInboundPort = new NmlSwitchingServiceRelationType();
    hasInboundPort.setType(Relationships.HAS_INBOUND_PORT);

    NmlSwitchingServiceRelationType hasOutboundPort = new NmlSwitchingServiceRelationType();
    hasOutboundPort.setType(Relationships.HAS_OUTBOUND_PORT);

    ports.values().stream()
            .filter(p -> topologyId.equalsIgnoreCase(p.getTopologyId()))
            .filter(p -> encoding.equals(p.getEncoding()))
            .filter(p -> p.getLabels().stream().anyMatch(label -> Optional.ofNullable(label.getLabeltype()).equals(labelType)))
            .forEach(p -> {
              // Ignore the bidirectional ports.
              if (p.getOrientation() == Orientation.inbound) {
                NmlPortGroupType pg = new NmlPortGroupType();
                pg.setId(p.getId());
                hasInboundPort.getPortGroup().add(pg);
              } else if (p.getOrientation() == Orientation.outbound) {
                NmlPortGroupType pg = new NmlPortGroupType();
                pg.setId(p.getId());
                hasOutboundPort.getPortGroup().add(pg);
              }
            });

    switchingService.getRelation().add(hasInboundPort);
    switchingService.getRelation().add(hasOutboundPort);

    return switchingService;
  }

  public Collection<String> getBidirectionalPortIdFromSwitchingService(NmlSwitchingServiceType switchingService) {
    Set<String> inbound = new HashSet<>();
    Set<String> outbound = new HashSet<>();

    // Collect all the unidirectional ports and generate bidireaction ports
    // if both inbound and outbound ports exist.
    for (NmlSwitchingServiceRelationType relation : switchingService.getRelation()) {
      if (Relationships.HAS_INBOUND_PORT.equalsIgnoreCase(relation.getType())) {
        for (NmlPortGroupType portGroup : relation.getPortGroup()) {
          Optional<NmlPort> in = Optional.ofNullable(ports.get(portGroup.getId()));
          in.ifPresent(p -> inbound.add(p.getParentPort().get()));
        }

        for (NmlPortType port : relation.getPort()) {
          Optional<NmlPort> o = Optional.ofNullable(ports.get(port.getId()));
          o.ifPresent(p -> inbound.add(p.getParentPort().get()));
        }
      } else if (Relationships.HAS_OUTBOUND_PORT.equalsIgnoreCase(relation.getType())) {
        for (NmlPortGroupType portGroup : relation.getPortGroup()) {
          Optional<NmlPort> in = Optional.ofNullable(ports.get(portGroup.getId()));
          in.ifPresent(p -> outbound.add(p.getParentPort().get()));
        }

        for (NmlPortType port : relation.getPort()) {
          Optional<NmlPort> o = Optional.ofNullable(ports.get(port.getId()));
          o.ifPresent(p -> outbound.add(p.getParentPort().get()));
        }
      }
    }

    inbound.retainAll(outbound);
    return inbound;
  }
}
