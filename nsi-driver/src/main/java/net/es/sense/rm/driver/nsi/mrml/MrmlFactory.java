package net.es.sense.rm.driver.nsi.mrml;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.google.common.base.Strings;
import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlDate;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.dds.lib.jaxb.nml.NmlLocationType;
import net.es.nsi.dds.lib.jaxb.nml.NmlLocationType.NmlAddress;
import net.es.nsi.dds.lib.jaxb.nml.NmlSwitchingServiceType;
import net.es.nsi.dds.lib.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.lib.jaxb.nml.ServiceDefinitionType;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import net.es.sense.rm.driver.schema.Mrs;
import net.es.sense.rm.driver.schema.Nml;
import net.es.sense.rm.driver.schema.Rdf;
import net.es.sense.rm.driver.schema.Sd;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;

/**
 * This class handles creation of an MRML model representing the network.
 *
 * @author hacksaw
 */
@Slf4j
public class MrmlFactory {

  private final NmlModel nml; // The NML topology model as discovered from the providerNSA (via DDS).
  private final SwitchingSubnetModel ssm; // The MRS switching subnet model as discovered from providerNSA and locally stored data.
  private final String topologyId; // The topology identifier for this network (networkId in NSI-CS speak).
  private final NmlTopologyType topology; // The NML topology JAXB document that will be populated with discovered document.

  /**
   * Initialize this MRML factory class.
   *
   * @param nml The NML document cache.
   * @param ssm The switching subnet model cache.
   * @param topologyId The unique identifier for the topology managed by this MRML factory.
   * @throws IllegalArgumentException When a DDS topology document cannot be found for the specified topologyId.
   */
  public MrmlFactory(NmlModel nml, SwitchingSubnetModel ssm, String topologyId) throws IllegalArgumentException {
    log.debug("[MrmlFactory] creating topologyId = {}", topologyId);
    this.nml = nml;
    this.ssm = ssm;
    this.topologyId = topologyId;
    this.topology = nml.getTopology(topologyId)
            .orElseThrow(new IllegalArgumentExceptionSupplier(
                    "[MrmlFactory] Could not find NML document for topologyId = " + topologyId));
    log.debug("[MrmlFactory] created topologyId = {}", topologyId);
  }

  /**
   * MRML model version is a compound identifier composed of the lastDiscovered
   * time from the DDS for most recent document change and the connection
   * version (based on time we discovered the connection).
   *
   * @return
   */
  public String getVersion() {
    log.debug("[MrmlFactory] version query dds version = {}, connection version = {}",
            nml.getLastDiscovered(), ssm.getVersion());
    return nml.getLastDiscovered() + ":" + ssm.getVersion();
  }


  /**
   * Serialize the base Ontology model to a string.
   *
   * @param encoding The output encoding for the serializer.
   * @return The model serialized as a string.
   * @throws IOException If the model cannot be serialized.
   */
  public String getModelAsString(Lang encoding) throws IOException {
    log.debug("[MrmlFactory] getModelAsString for modelType {}", encoding);
    return ModelUtil.marshalModel(getOntologyModel().getBaseModel(), encoding.getName());
  }

  /**
   * Returns an MRML Ontology containing topologyResource for the specified topologyResource.
   *
   * @return MRML Ontology model.
   */
  public OntModel getOntologyModel() throws IOException {

    log.debug("[MrmlFactory] getOntologyModel for topologyId = {}", topologyId);

    // Create the empty model in which to place the content.
    OntModel model = ModelUtil.newMrmlModel();

    // Populate the root Topology resource.
    createTopolgyResource(model);

    // Add bidirectional ports using common attributes from unidirectional members.
    createBidirectionalPorts(model);

    // Process the ServiceDefinitions.
    createServiceDefinition(model);

    // Now we need to process the ports.
    createSwitchingService(model);

    createBidirectionalPortsFromConnections(model);

    createSwitchingSubnet(model);

    return model;
  }

  private static Resource createResource(OntModel model, String uri, Resource type) {
    Resource res = model.createResource(uri);
    model.add(model.createStatement(res, Rdf.type, type));
    return res;
  }

  private Resource createTopolgyResource(OntModel model) {
    // Add the root topologyResource element.
    Resource nmlTopology = createResource(model, topology.getId(), Nml.Topology);
    model.add(model.createStatement(nmlTopology, Nml.name, getName()));
    model.add(model.createStatement(nmlTopology, Nml.existsDuring, createLifetime(model)));
    Resource location = createLocation(model, topology.getLocation());
    if (location != null) {
      model.add(model.createStatement(nmlTopology, Nml.locatedAt, location));
    }

    // Instead of using the NML document version we make our own based on the most recent of the topology
    // document or connection discovery.
    try {
      long version = Math.max(nml.getLastDiscovered(), ssm.getVersion());
      model.add(model.createStatement(nmlTopology, Nml.version, XmlDate.longToXMLGregorianCalendar(version).toXMLFormat()));
    } catch (DatatypeConfigurationException ex) {
      log.error("createTopolgyResource: failed to convert internal version to XML.", ex);
      model.add(model.createStatement(nmlTopology, Nml.version, topology.getVersion().toXMLFormat()));
    }

    return nmlTopology;
  }

  private String getName() {
    return Strings.isNullOrEmpty(topology.getName()) ? topology.getId() : topology.getName();
  }

  private Resource createLifetime(OntModel model) {
    Resource res = model.createResource(Nml.Lifetime);
    if (topology.getLifetime() == null || topology.getLifetime().getStart() == null) {
      res.addProperty(Nml.start, topology.getVersion().toXMLFormat());
    } else {
      XMLGregorianCalendar start = topology.getLifetime().getStart();
      res.addProperty(Nml.start, start.toXMLFormat());
    }

    if (topology.getLifetime() != null && topology.getLifetime().getEnd() != null) {
      XMLGregorianCalendar end = topology.getLifetime().getEnd();
      res.addProperty(Nml.end, end.toXMLFormat());
    }

    return res;
  }

  private Resource createLocation(OntModel model, NmlLocationType location) {
    if (location == null) {
      return null;
    }

    Resource res = model.createResource(Nml.Location);
    if (location.getAddress() != null) {
      res.addProperty(Nml.address, XmlUtilities.jaxbToXml(NmlAddress.class, location.getAddress()));
    }

    if (location.getAlt() != null) {
      res.addLiteral(Nml.alt, location.getAlt());
    }

    if (location.getLat() != null) {
      res.addLiteral(Nml.lat, location.getLat());
    }

    if (location.getLat() != null) {
      res.addLiteral(Nml.long_, location.getLong());
    }

    if (location.getName() != null) {
      res.addLiteral(Nml.name, location.getName());
    }

    if (location.getUnlocode() != null) {
      res.addLiteral(Nml.unlocode, location.getUnlocode());
    }

    return res;
  }

  /**
   * Create the parent bidirectional ports and annotate with static attributes.
   * Creation of the child ports representing the utilized VLANS and consumed
   * bandwidth will occur later.
   *
   * @param model
   * @return
   * @throws IllegalArgumentException
   */
  private Map<String, Resource> createBidirectionalPorts(OntModel model) throws IllegalArgumentException {
    Map<String, Resource> biPorts = new HashMap<>();

    Resource topologyResource = model.getResource(topologyId);

    nml.getPorts(topologyId, Orientation.bidirectional).values()
            .forEach(p -> {
              Resource bi = createResource(model, p.getId(), Nml.BidirectionalPort);
              bi.addProperty(Nml.belongsTo, topologyResource);
              topologyResource.addProperty(Nml.hasBidirectionalPort, bi);

              addCommonAttributes(model, p, bi);

              // Make a label relationship.
              p.getLabels().forEach(l -> {
                String postfix = l.getLabeltype().substring(l.getLabeltype().lastIndexOf("#") + 1);
                Resource label = createResource(model, p.getId() + ":" + postfix, Nml.LabelGroup);
                Resource labelType = model.createResource(l.getLabeltype());
                label.addProperty(Nml.labeltype, labelType);
                label.addLiteral(Nml.values, l.getValue());
                bi.addProperty(Nml.hasLabelGroup, label);
                label.addProperty(Nml.belongsTo, bi);
              });

              // Make the bandwidth service.
              Resource bw = createResource(model, p.getId() + ":BandwidthService", Mrs.BandwidthService);

              bi.addProperty(Nml.hasService, bw);
              addBwAttributes(p, bw);
              //p.getUsedCapacity().ifPresent(c -> bw.addLiteral(Mrs.usedCapacity, c));
              //p.getAvailableCapacity().ifPresent(c -> bw.addLiteral(Mrs.availableCapacity, c));
              p.getReservableCapacity().ifPresent(c -> bw.addLiteral(Mrs.reservableCapacity, c));
              p.getIndividualCapacity().ifPresent(c -> bw.addLiteral(Mrs.individualCapacity, c));
              bw.addProperty(Nml.belongsTo, bi);
              biPorts.put(p.getId(), bi);
            });

    return biPorts;
  }

  private void addBwAttributes(NmlPort p, Resource bw) {
    bw.addLiteral(Mrs.type, p.getType().name());
    bw.addLiteral(Mrs.unit, nml.getDefaultUnits());
    bw.addLiteral(Mrs.granularity, p.getGranularity().orElse(nml.getDefaultGranularity()));
    p.getMaximumCapacity().ifPresent(c -> bw.addLiteral(Mrs.maximumCapacity, c));
    bw.addLiteral(Mrs.minimumCapacity, p.getMinimumCapacity().orElse(1L));
  }

  private void addCommonAttributes(OntModel model, NmlPort p, Resource bi) {
    p.getName().ifPresent(n -> bi.addLiteral(Nml.name, n));
    p.getEncoding().ifPresent(e -> {
      Resource encoding = model.createResource(e);
      bi.addProperty(Nml.encoding, encoding);
    });
    p.getIsAlias().ifPresent(i -> {
      Resource res = model.createResource(i);
      bi.addProperty(Nml.isAlias, res);
    });

    p.getServiceId().ifPresent(sid -> {
      Resource tag = model.createResource(sid);
      bi.addProperty(Mrs.tag, tag);
    });
  }

  private Map<String, Resource> createServiceDefinition(OntModel model) throws IllegalArgumentException {
    Map<String, Resource> sdCollection = new HashMap<>();

    Resource topologyResource = model.getResource(topologyId);

    nml.getServiceDefinitions(topologyId).forEach((serviceDefinition) -> {
      Resource sd = createResource(model, serviceDefinition.getId(), Sd.ServiceDefinition);

      sd.addProperty(Nml.belongsTo, topologyResource);
      topologyResource.addProperty(Sd.hasServiceDefinition, sd);

      if (!Strings.isNullOrEmpty(serviceDefinition.getName())) {
        sd.addLiteral(Nml.name, serviceDefinition.getName());
      }

      if (!Strings.isNullOrEmpty(serviceDefinition.getComment())) {
        sd.addLiteral(Sd.comment, serviceDefinition.getComment());
      }

      if (!Strings.isNullOrEmpty(serviceDefinition.getServiceType())) {
        sd.addLiteral(Sd.serviceType, serviceDefinition.getServiceType().trim());
      }

      sdCollection.put(serviceDefinition.getId(), sd);
    });

    return sdCollection;
  }

  private Map<String, Resource> createSwitchingService(OntModel model) throws IllegalArgumentException {
    Map<String, Resource> ssCollection = new HashMap<>();

    // Our parent topologyResource resource.
    Resource topologyResource = model.getResource(topologyId);

    nml.getSwitchingServices(topologyId).forEach((ss) -> {
      // Create the NML switching service.
      Resource ssr = createResource(model, ss.getId(), Nml.SwitchingService);

      // This belongs to the parent topologyResource.
      ssr.addProperty(Nml.belongsTo, topologyResource);

      // The parent topologyResource has this service.
      topologyResource.addProperty(Nml.hasService, ssr);

      // At the NML properties.
      if (!Strings.isNullOrEmpty(ss.getName())) {
        ssr.addLiteral(Nml.name, ss.getName());
      }

      AddSsAttributes(model, ss, ssr);

      // Add the ServiceDefinition.
      ss.getAny().stream()
              .filter((object) -> (object instanceof JAXBElement))
              .map((object) -> (JAXBElement) object)
              .filter((jaxb) -> (jaxb.getValue() instanceof ServiceDefinitionType))
              .map((jaxb) -> (ServiceDefinitionType) jaxb.getValue())
              .forEachOrdered((sd) -> {
                ssr.addProperty(Sd.hasServiceDefinition, model.getResource(sd.getId()));
              }
              );

      // Add all the bidirectional port identifiers associated with the
      // unidirectional ports.
      nml.getBidirectionalPortIdFromSwitchingService(ss).forEach((bi) -> {
        ssr.addProperty(Nml.hasBidirectionalPort, model.getResource(bi));
      });

    });

    return ssCollection;
  }

  private void AddSsAttributes(OntModel model, NmlSwitchingServiceType ss, Resource ssr) {
    if (!Strings.isNullOrEmpty(ss.getEncoding())) {
      Resource encoding = model.createResource(ss.getEncoding());
      ssr.addProperty(Nml.encoding, encoding);
    }

    ssr.addLiteral(Nml.labelSwapping, ss.isLabelSwapping());

    if (!Strings.isNullOrEmpty(ss.getLabelType())) {
      Resource encoding = model.createResource(ss.getLabelType());
      ssr.addProperty(Nml.labeltype, encoding);
    }
  }

  /**
   * Create the children ports based off of the discovered NSI connections.
   *
   * @param model
   * @return
   * @throws IllegalArgumentException
   */
  private Map<String, Resource> createBidirectionalPortsFromConnections(OntModel model) throws IllegalArgumentException {
    Map<String, Resource> biPorts = new HashMap<>();

    log.debug("[MrmlFactory] createBidirectionalPortsFromConnections for topologyId {}", topologyId);

    nml.getPorts(topologyId, Orientation.child).values().forEach(p -> {
      log.debug("[MrmlFactory] creating child port {}", p.getId());

      // Create the Bidirectional port resource.
      Resource bi = createResource(model, p.getId(), Nml.BidirectionalPort);

      p.getParentPort().ifPresent(parent -> {
        log.debug("[MrmlFactory] linking child port {} to parentPort {}", p.getId(), parent);
        Resource parentPort = model.getResource(parent);
        bi.addProperty(Nml.belongsTo, parentPort);
        parentPort.addProperty(Nml.hasBidirectionalPort, bi);
      });

      addCommonAttributes(model, p, bi);

      // Special nml:existsDuring handling: If we do not have an existsDuring
      // resource with the same identifier as ours, we will need to create a new
      // one, otherwise, just reference the existing one.
      final Resource existsDuring = createLifetime(model, p.getNmlExistsDuringId().get(),
          p.getStartTime(), p.getEndTime());
      bi.addProperty(Nml.existsDuring, existsDuring);

      // Make a label relationship.
      p.getLabels().forEach(l -> {
        String labelId;
        if (p.getMrsLabelId().isPresent()) {
          labelId = p.getMrsLabelId().get();
        } else {
          labelId = p.getId() + ":label";
        }

        Resource label = createResource(model, labelId, Nml.Label);
        Resource labelType = model.createResource(l.getLabeltype());
        label.addProperty(Nml.existsDuring, existsDuring);
        label.addProperty(Nml.labeltype, labelType);
        label.addLiteral(Nml.value, l.getValue());
        bi.addProperty(Nml.hasLabel, label);
        label.addProperty(Nml.belongsTo, bi);
      });

      // Make the bandwidth service - NSI supports guaranteedCapped only.
      String bwId;
      if (p.getMrsBandwidthId().isPresent()) {
        bwId = p.getMrsBandwidthId().get();
      } else {
        bwId = p.getId() + ":BandwidthService";
      }

      Resource bw = createResource(model, bwId, Mrs.BandwidthService);
      bi.addProperty(Nml.hasService, bw);
      bw.addProperty(Nml.existsDuring, existsDuring);
      addBwAttributes(p, bw);
      p.getUsedCapacity().ifPresent(c -> bw.addLiteral(Mrs.usedCapacity, c));
      p.getAvailableCapacity().ifPresent(c -> bw.addLiteral(Mrs.availableCapacity, c));
      p.getReservableCapacity().ifPresent(c -> bw.addLiteral(Mrs.reservableCapacity, c));
      p.getIndividualCapacity().ifPresent(c -> bw.addLiteral(Mrs.individualCapacity, c));
      bw.addProperty(Nml.belongsTo, bi);
      biPorts.put(p.getId(), bi);
    });

    return biPorts;
  }

  private Map<String, Resource> createSwitchingSubnet(OntModel model) throws IllegalArgumentException {
    Map<String, Resource> ssCollection = new HashMap<>();

    for (ServiceHolder sh : ssm.getServiceHolder().values()) {
      // Our associated SwitchingService.
      NmlSwitchingServiceType switchingService = sh.getSwitchingService();
      List<NmlSwitchingSubnet> switchingSubnets = sh.getSwitchingSubnets();

      // Get our holding SwitchingService.
      Resource swResource = model.getResource(switchingService.getId());

      for (NmlSwitchingSubnet switchingSubnet : switchingSubnets) {
        // Create the NML switching service.
        Resource ssr = createResource(model, switchingSubnet.getId(), Mrs.SwitchingSubnet);

        // Place the reservation identifier into the SwitchingSubnet for tracing.
        ssr.addProperty(Mrs.tag, switchingSubnet.getTag());

        // This belongs to the parent topologyResource.
        ssr.addProperty(Nml.belongsTo, swResource);

        // We provide the subnet.
        swResource.addProperty(Mrs.providesSubnet, ssr);

        AddSsAttributes(model, switchingService, ssr);

        Resource existsDuring = createLifetime(model, switchingSubnet.getExistsDuringId(),
                switchingSubnet.getStartTime(), switchingSubnet.getEndTime());
        ssr.addProperty(Nml.existsDuring, existsDuring);

        // Add dataPlane status to the SwitchingSubnet.
        Resource resNetworkStatus = createResource(model, switchingSubnet.getId() + ":status", Mrs.NetworkStatus);
        resNetworkStatus.addProperty(Mrs.type, "dataplane");
        resNetworkStatus.addProperty(Mrs.value, switchingSubnet.getStatus().toString());
        ssr.addProperty(Mrs.hasNetworkStatus, resNetworkStatus);

        // Add the oscarsId SwitchingSubnet if present.
        Optional.ofNullable(switchingSubnet.getOscarsId()).ifPresent(oscarsId -> {
          Resource resNetworkAddress = createResource(model, switchingSubnet.getId() + ":oscarsId", Mrs.NetworkAddress);
          resNetworkAddress.addProperty(Mrs.type, "oscarsId");
          resNetworkAddress.addProperty(Mrs.value, oscarsId);
          ssr.addProperty(Mrs.hasNetworkAddress, resNetworkAddress);
        });

        // Add the errorStatus information if there is an error.
        if (switchingSubnet.getErrorState() != Reservation.ErrorState.NONE) {
          Resource resNetworkError = createResource(model, switchingSubnet.getId() + ":error", Mrs.ErrorStatus);
          resNetworkError.addProperty(Mrs.type, "errorStatus");
          resNetworkError.addProperty(Mrs.value, switchingSubnet.getErrorState().toString());
          if (!Strings.isNullOrEmpty(switchingSubnet.getErrorMessage())) {
            resNetworkError.addProperty(Mrs.errorMessage, switchingSubnet.getErrorMessage());
          }
          ssr.addProperty(Mrs.hasErrorStatus, resNetworkError);
        }

        // Add all the bidirectional port identifiers.
        switchingSubnet.getPorts().forEach(bi -> {
          // Get the resource associated with the bidirectional port.
          Resource biResource = model.getResource(bi.getId());

          // Add the hasBidirectionalPort relationship to the SwitchingSubnet.
          ssr.addProperty(Nml.hasBidirectionalPort, biResource);

          // Now add the NetworkStatus to this BiDirectional port.
          biResource.addProperty(Mrs.hasNetworkStatus, resNetworkStatus);
        });

        ssCollection.put(ssr.getURI(), ssr);
      }
    }

    return ssCollection;
  }

  private Resource createLifetime(OntModel model, String id, Optional<Long> startTime, Optional<Long> endTime) {
    log.debug("[createLifetime] entering");

    // If the lifetime resource already exists then return it (trust there
    // is not a conflicting id being used by a different resource type).
    /**Resource lifeTime = model.getResource(id);
    if (lifeTime != null) {
      log.debug("[createLifetime] found Lifetime resource {}, {}", lifeTime.getURI());
      return lifeTime;
    }**/

    final Resource res = createResource(model, id, Nml.Lifetime);
    startTime.ifPresent(s -> {
      try {
        XMLGregorianCalendar start = XmlUtilities.xmlGregorianCalendar(new Date(s));
        res.addProperty(Nml.start, ResourceFactory.createTypedLiteral(start.toXMLFormat(), XSDDatatype.XSDdateTime));
      } catch (DatatypeConfigurationException ex) {
        log.error("[MrmlFactory] failed to create startTime xmlGregorianCalendar", ex);
      }
    });

    endTime.ifPresent(s -> {
      try {
        XMLGregorianCalendar end = XmlUtilities.xmlGregorianCalendar(new Date(s));
        res.addProperty(Nml.end, ResourceFactory.createTypedLiteral(end.toXMLFormat(), XSDDatatype.XSDdateTime));
      } catch (DatatypeConfigurationException ex) {
        log.error("[MrmlFactory] failed to create endTime xmlGregorianCalendar", ex);
      }
    });

    log.debug("[createLifetime] created new Lifetime resource {}", res.getURI());

    return res;
  }
}
