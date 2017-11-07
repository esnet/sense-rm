package net.es.sense.rm.driver.nsi.mrml;

import com.google.common.base.Strings;
import java.io.StringWriter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.dds.lib.jaxb.nml.NmlLocationType;
import net.es.nsi.dds.lib.jaxb.nml.NmlLocationType.NmlAddress;
import net.es.nsi.dds.lib.jaxb.nml.NmlSwitchingServiceType;
import net.es.nsi.dds.lib.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.lib.jaxb.nml.ServiceDefinitionType;
import net.es.sense.rm.driver.schema.Mrs;
import net.es.sense.rm.driver.schema.Nml;
import net.es.sense.rm.driver.schema.Owl;
import net.es.sense.rm.driver.schema.Rdf;
import net.es.sense.rm.driver.schema.Rdfs;
import net.es.sense.rm.driver.schema.Sd;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class MrmlFactory {

  private final NmlModel nml;
  private final SwitchingSubnetModel ssm;
  private final String topologyId;
  private final NmlTopologyType topology;

  public MrmlFactory(NmlModel nml, SwitchingSubnetModel ssm, String topologyId) throws IllegalArgumentException {
    this.nml = nml;
    this.ssm = ssm;
    this.topologyId = topologyId;
    this.topology = nml.getTopology(topologyId)
            .orElseThrow(new IllegalArgumentExceptionSupplier(
                    "[MrmlFactory] Could not find NML document for topologyId = " + topologyId));
  }


  public long getVersion() {
    XMLGregorianCalendar version = topology.getVersion();
    GregorianCalendar cal = version.toGregorianCalendar();
    return cal.getTimeInMillis() > ssm.getVersion() ? cal.getTimeInMillis() : ssm.getVersion() ;
  }

  public String getModelAsString(String modelType) {
    Lang encoding;
    switch(modelType.toLowerCase()) {
      case "jasonld":
        encoding = Lang.JSONLD;
        break;
      case "turtle":
      default:
        encoding = Lang.TURTLE;
        break;

    }
    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, getOntologyModel().getBaseModel(), encoding);
    return sw.toString();
  }

  public String getModelAsString(Lang encoding) {
    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, getOntologyModel().getBaseModel(), encoding);
    return sw.toString();
  }

  /**
   * Returns an MRML Ontology containing topologyResource for the specified topologyResource.
   *
   * @return MRML Ontology model.
   */
  public OntModel getOntologyModel() {

    log.info("[getOntologyModel] topologyId = {}", topologyId);

    // Create the empty model in which to place the content.
    OntModel model = createEmptyModel();

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

  public OntModel createEmptyModel() {
    OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
    model.setNsPrefix("rdfs", Rdfs.getURI());
    model.setNsPrefix("rdf", Rdf.getURI());
    model.setNsPrefix("sd", Sd.getURI());
    model.setNsPrefix("owl", Owl.getURI());
    model.setNsPrefix("nml", Nml.getURI());
    model.setNsPrefix("mrs", Mrs.getURI());
    //model.setNsPrefix("spa", Spa.getURI());
    return model;
  }

  private Resource createResource(OntModel model, String uri, Resource type) {
    Resource res = model.createResource(uri);
    model.add(model.createStatement(res, Rdf.type, type));
    return res;
  }

  private Resource createTopolgyResource(OntModel model) {
    // Add the root topologyResource element.
    Resource nmlTopology = createResource(model, topology.getId(), Nml.Topology);
    model.add(model.createStatement(nmlTopology, Nml.name, getName()));
    model.add(model.createStatement(nmlTopology, Nml.version, topology.getVersion().toXMLFormat()));
    model.add(model.createStatement(nmlTopology, Nml.existsDuring, createLifetime(model)));
    Resource location = createLocation(model, topology.getLocation());
    if (location != null) {
      model.add(model.createStatement(nmlTopology, Nml.locatedAt, location));
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
      res.addProperty(Nml.address, XmlUtilities.jaxbToString(NmlAddress.class, location.getAddress()));
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

  private Map<String, Resource> createBidirectionalPorts(OntModel model) throws IllegalArgumentException {
    Map<String, Resource> biPorts = new HashMap<>();

    Resource topologyResource = model.getResource(topologyId);

    nml.getPorts(topologyId, Orientation.bidirectional).values().stream()
            .forEach(p -> {
              Resource bi = createResource(model, p.getId(), Nml.BidirectionalPort);
              bi.addProperty(Nml.belongsTo, topologyResource);
              topologyResource.addProperty(Nml.hasBidirectionalPort, bi);

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

              // Make a label relationship.
              p.getLabels().stream().forEach(l -> {
                String postfix = l.getLabeltype().substring(l.getLabeltype().lastIndexOf("#") + 1);
                Resource label = createResource(model, p.getId() + ":" + postfix, Nml.LabelGroup);
                Resource labelType = model.createResource(l.getLabeltype());
                label.addProperty(Nml.labeltype, labelType);
                label.addLiteral(Nml.values, l.getValue());
                bi.addProperty(Nml.hasLabelGroup, label);
                label.addProperty(Nml.belongsTo, bi);
              });

              // Make the bandwidth service - NSI supports guaranteedCapped only.
              Resource bw = createResource(model, p.getId() + ":BandwidthService", Mrs.BandwidthService);
              bi.addProperty(Nml.hasService, bw);
              bw.addLiteral(Mrs.type, nml.getDefaultType());
              bw.addLiteral(Mrs.unit, nml.getDefaultUnits());
              bw.addLiteral(Mrs.granularity, p.getGranularity().orElse(nml.getDefaultGranularity()));
              bw.addLiteral(Mrs.minimumCapacity, p.getMinimumReservableCapacity().orElse(1L));
              if (p.getMaximumReservableCapacity().isPresent()) {
                long c = p.getMaximumReservableCapacity().get();
                bw.addLiteral(Mrs.maximumCapacity, c);
                bw.addLiteral(Mrs.reservableCapacity, c);
                bw.addLiteral(Mrs.individualCapacity, c);

                // Calculate used capacity.
                // availableCapacity = reservableCapacity - usedCapacity;
                long usedCapacity = 0;
                for (String child : p.getChildren()) {
                  Optional<NmlPort> childPort = Optional.ofNullable(nml.getPort(child));
                  if (childPort.isPresent() && childPort.get().getCapacity().isPresent()) {
                    usedCapacity =  usedCapacity + childPort.get().getCapacity().get();
                  }
                }
                bw.addLiteral(Mrs.usedCapacity, usedCapacity);
                bw.addLiteral(Mrs.availableCapacity, c - usedCapacity);
              }
              bw.addProperty(Nml.belongsTo, bi);
              biPorts.put(p.getId(), bi);
            });

    return biPorts;
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

      if (!Strings.isNullOrEmpty(ss.getEncoding())) {
        Resource encoding = model.createResource(ss.getEncoding());
        ssr.addProperty(Nml.encoding, encoding);
      }

      ssr.addLiteral(Nml.labelSwapping, ss.isLabelSwapping());

      if (!Strings.isNullOrEmpty(ss.getLabelType())) {
        Resource encoding = model.createResource(ss.getLabelType());
        ssr.addProperty(Nml.labeltype, encoding);
      }

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

  private Map<String, Resource> createBidirectionalPortsFromConnections(OntModel model) throws IllegalArgumentException {
    Map<String, Resource> biPorts = new HashMap<>();

    log.info("[MrmlFactory] createBidirectionalPortsFromConnections for topologyId {}", topologyId);

    nml.getPorts(topologyId, Orientation.child).values().stream()
            .forEach(p -> {
              log.info("[MrmlFactory] creating child port {}", p.getId());

              Resource parentPort = model.getResource(p.getParentPort().get());

              log.info("[MrmlFactory] parentPort {}, resource {}", p.getParentPort().get(), parentPort);

              Resource bi = createResource(model, p.getId(), Nml.BidirectionalPort);
              bi.addProperty(Nml.belongsTo, parentPort);
              parentPort.addProperty(Nml.hasBidirectionalPort, bi);

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

              model.add(model.createStatement(bi, Nml.existsDuring,
                      createLifetime(model, p.getStartTime(), p.getEndTime())));

              // Make a label relationship.
              p.getLabels().stream().forEach(l -> {
                Resource label = createResource(model, p.getId() + ":label", Nml.Label);
                Resource labelType = model.createResource(l.getLabeltype());
                label.addProperty(Nml.labeltype, labelType);
                label.addLiteral(Nml.value, l.getValue());
                bi.addProperty(Nml.hasLabel, label);
                label.addProperty(Nml.belongsTo, bi);
              });

              // Make the bandwidth service - NSI supports guaranteedCapped only.
              Resource bw = createResource(model, p.getId() + ":BandwidthService", Mrs.BandwidthService);
              bi.addProperty(Nml.hasService, bw);
              bw.addLiteral(Mrs.type, nml.getDefaultType());
              bw.addLiteral(Mrs.unit, nml.getDefaultUnits());
              bw.addLiteral(Mrs.granularity, p.getGranularity().orElse(nml.getDefaultGranularity()));
              bw.addLiteral(Mrs.minimumCapacity, p.getMinimumReservableCapacity().orElse(1L));

              long usedCapacity = p.getCapacity().orElse(0L);
              bw.addLiteral(Mrs.usedCapacity, usedCapacity);

              if (p.getMaximumReservableCapacity().isPresent()) {
                long c = p.getMaximumReservableCapacity().get();

                bw.addLiteral(Mrs.maximumCapacity, c);
                bw.addLiteral(Mrs.reservableCapacity, c);
                bw.addLiteral(Mrs.availableCapacity, c - usedCapacity);
                bw.addLiteral(Mrs.individualCapacity, c);

              }

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

        // This belongs to the parent topologyResource.
        ssr.addProperty(Nml.belongsTo, swResource);

        // We provide the subnet.
        swResource.addProperty(Mrs.providesSubnet, ssr);

        if (!Strings.isNullOrEmpty(switchingService.getEncoding())) {
          Resource encoding = model.createResource(switchingService.getEncoding());
          ssr.addProperty(Nml.encoding, encoding);
        }

        ssr.addLiteral(Nml.labelSwapping, switchingService.isLabelSwapping());

        if (!Strings.isNullOrEmpty(switchingService.getLabelType())) {
          Resource encoding = model.createResource(switchingService.getLabelType());
          ssr.addProperty(Nml.labeltype, encoding);
        }

        model.add(model.createStatement(ssr, Nml.existsDuring,
                createLifetime(model, switchingSubnet.getStartTime(), switchingSubnet.getEndTime())));

        // Add all the bidirectional port identifiers.
        switchingSubnet.getPorts().forEach(bi -> {
          ssr.addProperty(Nml.hasBidirectionalPort, model.getResource(bi.getId()));
        });
      }
    }

    return ssCollection;
  }

  private Resource createLifetime(OntModel model, Optional<Long> startTime, Optional<Long> endTime) {
    Resource res = model.createResource(Nml.Lifetime);
    startTime.ifPresent(s -> {
      try {
        XMLGregorianCalendar start = XmlUtilities.xmlGregorianCalendar(new Date(s));
        res.addProperty(Nml.start, start.toXMLFormat());
      } catch (DatatypeConfigurationException ex) {
        log.error("[MrmlFactory] failed to create startTime xmlGregorianCalendar, {}", ex);
      }
    });

    endTime.ifPresent(s -> {
      try {
        XMLGregorianCalendar end = XmlUtilities.xmlGregorianCalendar(new Date(s));
        res.addProperty(Nml.end, end.toXMLFormat());
      } catch (DatatypeConfigurationException ex) {
        log.error("[MrmlFactory] failed to create endTime xmlGregorianCalendar, {}", ex);
      }
    });

    return res;
  }
}
