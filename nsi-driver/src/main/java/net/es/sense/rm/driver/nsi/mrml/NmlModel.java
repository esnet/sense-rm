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

import com.twitter.common.base.Either;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.lib.jaxb.Relationships;
import net.es.nsi.dds.lib.jaxb.nml.*;
import net.es.sense.rm.driver.nsi.dds.api.DocumentReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class loads all discovered NML documents from the DDS and tracks lastModified
 * times to allow for change detection.  This class also consolidates unidirectional
 * ports into bidirectional port entries with resolved isAlias relationships.
 *
 * @author hacksaw
 */
@Slf4j
public class NmlModel {
  // Globals.
  private final static ObjectFactory FACTORY = new ObjectFactory();
  private final DocumentReader documentReader;
  private final Map<String, NmlPort> ports = new HashMap<>();
  private long lastDiscovered = 0;

  // These are Ethernet service parameter defaults.
  private String defaultServiceType;
  private MrsBandwidthType defaultType = MrsBandwidthType.undefined;
  private String defaultUnits;
  private long defaultGranularity;

  /**
   * Class constructor.
   *
   * @param documentReader A reader for topology documents from the DDS.
   */
  public NmlModel(DocumentReader documentReader) {
    this.documentReader = documentReader;
  }

  /**
   * This method will load every NML port from all discovered network
   * topologies, resolve the unidirection port isAlias entries, and assign
   * this adjacency information to the bidirectional port pair.
   */
  private void load() {
    // Resolve all ports across all networks for later access.
    lastDiscovered = documentReader.getLastDiscovered();
    Collection<NmlTopologyType> topologies = documentReader.getNmlTopologyAll();
    for (NmlTopologyType nml : topologies) {
      log.debug("[NmlModel] processing NML model {}", nml.getId());
      ports.putAll(getNmlPorts(nml));
    }

    // Consolidate isAlias entries in the bidirectional ports.
    ports.values().stream()
             .filter(p -> p.getOrientation() == Orientation.bidirectional)
             .forEach(p -> {
                try {
                  // We need the member in and out unidirectional ports to look up isAlias entries.
                  NmlPort inboundPort = ports.get(p.getInboundPort().get());
                  NmlPort outboundPort = ports.get(p.getOutboundPort().get());

                  // If these unidirectional ports have isAlias entries we will try to consolidate.
                  if (inboundPort.getIsAlias().isPresent() && outboundPort.getIsAlias().isPresent()) {
                    // Look to see if the referenced isAlias ports are in the topology.
                    Optional<NmlPort> remoteOut = Optional.ofNullable(ports.get(inboundPort.getIsAlias().get()));
                    Optional<NmlPort> remoteIn = Optional.ofNullable(ports.get(outboundPort.getIsAlias().get()));

                    // If the remote port's isAlias points back to our member ports...
                    if (remoteOut.isPresent() && remoteOut.get().getIsAlias().isPresent() &&
                            inboundPort.getId().equalsIgnoreCase(remoteOut.get().getIsAlias().get()) &&
                        remoteIn.isPresent() && remoteIn.get().getIsAlias().isPresent() &&
                            outboundPort.getId().equalsIgnoreCase(remoteIn.get().getIsAlias().get())) {
                      // We consolidated the isAlias to a port in topology so now we save it.
                      p.setIsAlias(remoteOut.get().getParentPort());
                    }
                  }
                } catch (NoSuchElementException nse) {
                  // Something was missing from our model for this port.
                  log.error("[NmlModel] Failed to load {}", p.getId());
                }
             });
  }

  /**
   * @return the lastDiscovered
   */
  public long getLastDiscovered() {
    return lastDiscovered;
  }

  /**
   * Get the default service definition type used when creating
   * SwitchingService elements.
   *
   * @return
   */
  public String getDefaultServiceType() {
    return defaultServiceType;
  }

  /**
   * Set the default service definition type used when creating
   * SwitchingService elements.
   *
   * @param defaultServiceType
   */
  public void setDefaultServiceType(String defaultServiceType) {
    this.defaultServiceType = defaultServiceType;
  }

  /**
   * Get the default bandwidth QoS type.
   *
   * @return the defaultType
   */
  public MrsBandwidthType getDefaultType() {
    return defaultType;
  }

  /**
   * Set the default bandwidth QoS type.
   *
   * @param defaultType the defaultType to set
   */
  public void setDefaultType(MrsBandwidthType defaultType) {
    this.defaultType = defaultType;
  }

  /**
   * Set the default bandwidth QoS type.
   *
   * @param defaultType the defaultType to set
   */
  public void setDefaultType(String defaultType) {
    try {
      this.defaultType = MrsBandwidthType.valueOf(defaultType);
    } catch (IllegalArgumentException ex) {
      log.error("[NmlModel] could not convert defaultType " + defaultType, ex);
    }
  }

  /**
   * Get the default bandwidth metric unit.
   *
   * @return the defaultUnits
   */
  public String getDefaultUnits() {
    return defaultUnits;
  }

  /**
   * Set the default bandwidth metric unit.
   *
   * @param defaultUnits the defaultUnits to set
   */
  public void setDefaultUnits(String defaultUnits) {
    this.defaultUnits = defaultUnits;
  }

  /**
   * Get the default bandwidth granularity value.
   *
   * @return the defaultGranularity
   */
  public long getDefaultGranularity() {
    return defaultGranularity;
  }

  /**
   * Set the default bandwidth granularity value.
   *
   * @param defaultGranularity the defaultGranularity to set
   */
  public void setDefaultGranularity(long defaultGranularity) {
    this.defaultGranularity = defaultGranularity;
  }

  /**
   * Get a bidirectional NmlPort matching the specified identifier.
   *
   * @param id
   * @return
   */
  public NmlPort getPort(String id) {
    if (ports.isEmpty()) {
      load();
    }
    return ports.get(id);
  }

  /**
   * Get a map of bidirectional NmlPort indexed by port identifier.
   *
   * @return
   */
  public Map<String, NmlPort> getPorts() {
    if (ports.isEmpty()) {
      load();
    }
    return ports;
  }

  /**
   * Add an NmlPort to to the port map indexed by the port identifier.
   *
   * @param port the NmlPort to add to the port map.
   *
   * @return
   */
  public NmlPort addPort(NmlPort port) {
    return ports.put(port.getId(), port);
  }

  /**
   * Get a new map of NmlPort entries indexed by port identifier for the
   * specified topologyId.
   *
   * @param topologyId The topology identifier of the network for which to build the port map.
   *
   * @return
   */
  public Map<String, NmlPort> getPorts(String topologyId) {
    if (ports.isEmpty()) {
      load();
    }
    return ports.values().stream()
            .filter(p -> p.getTopologyId().equalsIgnoreCase(topologyId))
            .collect(Collectors.toMap(p -> p.getId(), p -> p));
  }

  /**
   * Get a new map of NmlPort entries indexed by port identifier and matching
   * the provided orientation for the specified topologyId.
   *
   * @param topologyId The topology identifier of the network for which to build the port map.
   * @param orientation The orientation of the ports to include in the map.
   *
   * @return
   */
  public Map<String, NmlPort> getPorts(String topologyId, Orientation orientation) {
    if (ports.isEmpty()) {
      load();
    }
    return ports.values().stream()
            .filter(p -> p.getTopologyId().equalsIgnoreCase(topologyId) &&
                    p.getOrientation().compareTo(orientation) == 0)
            .collect(Collectors.toMap(p -> p.getId(), p -> p));
  }

  /**
   * Get an NmlTopology matching the provided topology identifier.
   *
   * @param topologyId The topology identifier to retrieve.
   *
   * @return
   */
  public Optional<NmlTopologyType> getTopology(String topologyId) {
    return documentReader.getTopologyById(topologyId).stream().findFirst();
  }

  /**
   * Get a list of available topology identifiers.
   *
   * @return
   */
  public Collection<String> getTopologyIds() {
    Collection<String> results = new ArrayList<>();
    for (NmlTopologyType topology : documentReader.getNmlTopologyAll()) {
      results.add(topology.getId());
    }
    return results;
  }

  /**
   * Create a new map of NmlPorts for the given nmlTopology indexed by port
   * identifier.  This map will contain both unidirectional and bidirectional NML ports.
   *
   * @param nmlTopology NML topology to parse for a list of ports.
   *
   * @return
   */
  private Map<String, NmlPort> getNmlPorts(NmlTopologyType nmlTopology) {
    Map<String, NmlPort> portMap = new HashMap<>();

    // Unidirectional ports and SwitchingService are modeled using Relations.
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

  /**
   * Convert the NmlPortGroupType element into an NmlPort.
   *
   * @param portGroup The PortGroup to convert.
   * @param orientation The orientation of the port.
   * @param topologyId The topology identifier for this port.
   *
   * @return
   */
  private NmlPort convertPortGroup(NmlPortGroupType portGroup, Orientation orientation, String topologyId) {
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
            .type(defaultType)
            .granularity(params.getGranularity())
            .maximumCapacity(params.getCapacity())
            .minimumCapacity(params.getMinimumReservableCapacity())
            .reservableCapacity(params.getCapacity())
            .individualCapacity(params.getMaximumReservableCapacity())
            .availableCapacity(params.getCapacity())
            .interfaceMTU(params.getInterfaceMTU())
            .port(Either.left(portGroup));
    return builder.build();
  }

  /**
   * Convert the NmlPortType element into an NmlPort.
   *
   * @param port The Port to convert.
   * @param orientation The orientation of the port.
   * @param topologyId The topology identifier for this port.

   * @return
   */
  private NmlPort convertPort(NmlPortType port, Orientation orientation, String topologyId) {
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
            .type(defaultType)
            .granularity(params.getGranularity())
            .maximumCapacity(params.getCapacity())
            .minimumCapacity(params.getMinimumReservableCapacity())
            .reservableCapacity(params.getCapacity())
            .individualCapacity(params.getMaximumReservableCapacity())
            .availableCapacity(params.getCapacity())
            .interfaceMTU(params.getInterfaceMTU())
            .port(Either.right(port));

    return builder.build();
  }

  private Optional<NmlPort> convertBidirectionalPort(NmlBidirectionalPortType port, String topologyId, Map<String, NmlPort> portMap) {
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
      log.debug("[MrmlFactory] Bidirectional port {} has no labels for members {} and {} ", port.getId(), inbound.getId(), outbound.getId());
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
            .type(defaultType)
            .granularity(inbound.getGranularity())
            .maximumCapacity(inbound.getMaximumCapacity())
            .minimumCapacity(inbound.getMinimumCapacity())
            .reservableCapacity(inbound.getReservableCapacity())
            .individualCapacity(inbound.getIndividualCapacity())
            .availableCapacity(inbound.getAvailableCapacity())
            .interfaceMTU(inbound.getInterfaceMTU())
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
  public Map<String, ServiceDefinitionType> getServiceDefinitionsIndexed(String topologyId) throws IllegalArgumentException {

    NmlTopologyType topology = getTopology(topologyId)
            .orElseThrow(new IllegalArgumentExceptionSupplier(
                    "[getServiceDefinition] Could not find NML document for networkId = " + topologyId));

    // Pull out the ServiceDefinition elements which are stored in ANY.
    HashMap<String, ServiceDefinitionType> results = new HashMap<>();
    for (Object object : topology.getAny()) {
      if (object instanceof JAXBElement) {
        JAXBElement<?> jaxb = (JAXBElement) object;
        if (jaxb.getValue() instanceof ServiceDefinitionType) {
          ServiceDefinitionType cast = ServiceDefinitionType.class.cast(jaxb.getValue());
          results.put(cast.getId(), cast);
        }
      }
    }

    // If no service definition was found create a default one.
    if (results.isEmpty()) {
      ServiceDefinitionType serviceDefinition = FACTORY.createServiceDefinitionType();
      serviceDefinition.setId(topology.getId() + ":ServiceDefinition:default");
      serviceDefinition.setServiceType(defaultServiceType.trim());
      results.put(serviceDefinition.getId(), serviceDefinition);
    }

    return results;
  }

  public Collection<ServiceDefinitionType> getServiceDefinitions(String topologyId) throws IllegalArgumentException {
    return this.getServiceDefinitionsIndexed(topologyId).values();
  }

  private NmlSwitchingServiceType newNmlSwitchingService(String topologyId) throws IllegalArgumentException {
    if (ports.isEmpty()) {
      load();
    }
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
    serviceDefinition.setServiceType(defaultServiceType.trim());
    switchingService.getAny().add(FACTORY.createServiceDefinition(serviceDefinition));
    return switchingService;
  }

  public Collection<NmlSwitchingServiceType> getSwitchingServices(String topologyId) throws IllegalArgumentException {
    return getSwitchingServicesIndexed(topologyId).values();
  }

  public Map<String, NmlSwitchingServiceType> getSwitchingServicesIndexed(String topologyId) throws IllegalArgumentException {
    NmlTopologyType topology = getTopology(topologyId)
            .orElseThrow(new IllegalArgumentExceptionSupplier(
                    "[getSwitchingServicesIndexed] Could not find NML document for networkId = " + topologyId));

    log.debug("[getSwitchingServicesIndexed] entering.");

    Map<String, NmlSwitchingServiceType> newSwitchingServices = new HashMap<>();

    // The SwitchingService is modelled as a "hasService" relation.
    for (NmlTopologyRelationType relation : topology.getRelation()) {
      log.debug("[getSwitchingServicesIndexed] relation = {}", relation.getType());
      if (relation.getType().equalsIgnoreCase(Relationships.HAS_SERVICE)) {
        for (NmlNetworkObject service : relation.getService()) {
          // We want the SwitchingService.
          log.debug("[getSwitchingServicesIndexed] SwitchingService instance {}", service.getClass().getCanonicalName());
          if (service instanceof NmlSwitchingServiceType) {
            NmlSwitchingServiceType s = (NmlSwitchingServiceType) service;
            log.debug("found SwitchingService {}", service.getId());
            newSwitchingServices.put(s.getId(), s);
          }
        }
      }
    }

    // NML Default Behavior #1: Check to see if we have a SwitchingService
    // defined, and if not, create the default one with all ports and
    // labelSwapping set to false.
    if (newSwitchingServices.isEmpty()) {
      log.debug("No SwitchingService found so creating a default, topologyId = {}", topologyId);
      NmlSwitchingServiceType switchingService = newNmlSwitchingService(topologyId);
      newSwitchingServices.put(switchingService.getId(), switchingService);

    } else {
      log.debug("SwitchingService found but no port members so adding matching, topologyId = {}", topologyId);
      // NML Default Behavior #2: If we have a SwitchingService with no port
      // members then we must add all ports of matching label and encoding
      // type.  We use a boolean here to tell us if the SwitchingService
      // held a port.
      for (NmlSwitchingServiceType switchingService : newSwitchingServices.values()) {
        log.debug("Adding ports to SwitchingService = {}", switchingService.getId());
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
          log.debug("[getSwitchingServicesIndexed] no ports defined so populating wildcard.");
          // Treat this as a wildcard SwitchingService buy adding all
          // unidirectional ports with maching attributes.
          populateWildcardSwitchingService(switchingService, topologyId);
        }
      }
    }

    return newSwitchingServices;
  }

  private NmlSwitchingServiceType populateWildcardSwitchingService(NmlSwitchingServiceType switchingService, String topologyId) {
    if (ports.isEmpty()) {
      load();
    }
    Optional<String> encoding = Optional.ofNullable(switchingService.getEncoding());
    Optional<String> labelType = Optional.ofNullable(switchingService.getLabelType());

    log.debug("Found empty SwitchingService id= {} so populating based on wildcard rules, encoding = {}, labelType = {}.",
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
    if (ports.isEmpty()) {
      load();
    }

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
