package net.es.sense.rm.driver.nsi.mrml;

import com.google.common.base.Strings;
import java.io.StringWriter;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.lib.jaxb.nml.NmlLocationType;
import net.es.nsi.dds.lib.jaxb.nml.NmlLocationType.NmlAddress;
import net.es.nsi.dds.lib.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.lib.jaxb.nml.ServiceDefinitionType;
import net.es.nsi.dds.lib.util.XmlUtilities;
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
  private final String networkId;
  private final NmlTopologyType network;

  public MrmlFactory(NmlModel nml, String networkId) throws IllegalArgumentException {
    this.nml = nml;
    this.networkId = networkId;
    this.network = nml.getTopology(networkId)
            .orElseThrow(new IllegalArgumentExceptionSupplier(
                    "[createOntologyModel] Could not find NML document for networkId = " + networkId));
  }


  public long getVersion() {
    XMLGregorianCalendar version = network.getVersion();
    GregorianCalendar cal = version.toGregorianCalendar();
    return cal.getTimeInMillis();
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
   * Returns an MRML Ontology containing topology for the specified network.
   *
   * @return MRML Ontology model.
   */
  public OntModel getOntologyModel() {

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
    // Add the root topology element.
    Resource nmlTopology = createResource(model, network.getId(), Nml.Topology);
    model.add(model.createStatement(nmlTopology, Nml.name, getName()));
    network.getVersion().setTimezone(0);
    model.add(model.createStatement(nmlTopology, Nml.version, network.getVersion().toXMLFormat()));
    model.add(model.createStatement(nmlTopology, Nml.existsDuring, createLifetime(model)));
    Resource location = createLocation(model, network.getLocation());
    if (location != null) {
      model.add(model.createStatement(nmlTopology, Nml.locatedAt, location));
    }

    return nmlTopology;
  }

  private String getName() {
    return Strings.isNullOrEmpty(network.getName()) ? network.getId() : network.getName();
  }

  private Resource createLifetime(OntModel model) {
    Resource res = model.createResource(Nml.Lifetime);
    if (network.getLifetime() == null || network.getLifetime().getStart() == null) {
      res.addProperty(Nml.start, network.getVersion().toXMLFormat());
    } else {
      XMLGregorianCalendar start = network.getLifetime().getStart();
      start.setTimezone(0);
      res.addProperty(Nml.start, start.toXMLFormat());
    }

    if (network.getLifetime() != null && network.getLifetime().getEnd() != null) {
      XMLGregorianCalendar end = network.getLifetime().getEnd();
      end.setTimezone(0);
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

    Resource topology = model.getResource(networkId);

    nml.getPorts(networkId, Orientation.bidirectional).values().stream()
            .forEach(p -> {
              Resource bi = createResource(model, p.getId(), Nml.BidirectionalPort);
              bi.addProperty(Nml.belongsTo, topology);
              topology.addProperty(Nml.hasBidirectionalPort, bi);

              p.getName().ifPresent(n -> bi.addLiteral(Nml.name, n));
              p.getEncoding().ifPresent(e -> {
                Resource encoding = model.createResource(e);
                bi.addProperty(Nml.encoding, encoding);
                      });
              p.getIsAlias().ifPresent(i -> {
                Resource res = model.createResource(i);
                bi.addProperty(Nml.isAlias, res);
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
              p.getMaximumReservableCapacity().ifPresent(c -> bw.addLiteral(Mrs.maximumCapacity, c));
              p.getMaximumReservableCapacity().ifPresent(c -> bw.addLiteral(Mrs.reservableCapacity, c));
              p.getMinimumReservableCapacity().ifPresent(c -> bw.addLiteral(Mrs.minimumCapacity, c));
              bw.addLiteral(Mrs.usedCapacity, 0L);
              p.getMaximumReservableCapacity().ifPresent(c -> bw.addLiteral(Mrs.availableCapacity, c));
              p.getMaximumReservableCapacity().ifPresent(c -> bw.addLiteral(Mrs.individualCapacity, c));
              bw.addProperty(Nml.belongsTo, bi);
            });

    return biPorts;
  }

  private Map<String, Resource> createServiceDefinition(OntModel model) throws IllegalArgumentException {
    Map<String, Resource> sdCollection = new HashMap<>();

    Resource topology = model.getResource(networkId);

    nml.getServiceDefinitions(networkId).forEach((serviceDefinition) -> {
      Resource sd = createResource(model, serviceDefinition.getId(), Sd.ServiceDefinition);

      sd.addProperty(Nml.belongsTo, topology);
      topology.addProperty(Sd.hasServiceDefinition, sd);

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

    // Our parent topology resource.
    Resource topology = model.getResource(networkId);

    nml.getSwitchingServices(networkId).forEach((ss) -> {
      // Create the NML switching service.
      Resource ssr = createResource(model, ss.getId(), Nml.SwitchingService);

      // This belongs to the parent topology.
      ssr.addProperty(Nml.belongsTo, topology);

      // The parent topology has this service.
      topology.addProperty(Nml.hasService, ssr);

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

}
