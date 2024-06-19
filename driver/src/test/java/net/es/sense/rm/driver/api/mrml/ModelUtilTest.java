package net.es.sense.rm.driver.api.mrml;

import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.schema.Mrs;
import net.es.sense.rm.driver.schema.Nml;
import net.es.sense.rm.driver.schema.Sd;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ModelUtilTest {
  private final static String MRML_MODEL_TTL = "src/test/resources/mrml.ttl";
  private final static String MRML_MODEL_JSON = "src/test/resources/mrml.json";
  private final static String MRML_MODEL_XML = "src/test/resources/mrml.xml";
  private final static String MRML_MODEL_1 = "src/test/resources/model-1.ttl";
  private final static String MRML_MODEL_ADDITION_1 = "src/test/resources/addition-1.ttl";
  private final static String MRML_MODEL_REDUCTION_1 = "src/test/resources/reduction-1.ttl";
  private final static String MRML_MODEL_4 = "src/test/resources/model-4.ttl";
  private final static String MRML_MODEL_ADDITION_4 = "src/test/resources/addition-4.ttl";
  private final static String MRML_MODEL_REDUCTION_4 = "src/test/resources/reduction-4.ttl";
  private final static String MRML_MODEL_5 = "src/test/resources/model-5.ttl";
  private final static String MRML_MODEL_ADDITION_5 = "src/test/resources/addition-5.ttl";
  private final static String MRML_MODEL_REDUCTION_5 = "src/test/resources/reduction-5.ttl";

  private final static List<String> SWITCHINGSERVICES =
      List.of("urn:ogf:network:es.net:2013::ServiceDomain:EVTS.A-GOLE");
  private final static List<String> BIDIRECTIONALPORTS = List.of(
      "urn:ogf:network:es.net:2013::anl-mr2:xe-1_2_0:+",
      "urn:ogf:network:es.net:2013::pnwg-cr5:2_1_1:+",
      "urn:ogf:network:es.net:2013::fnal-mr2:xe-0_2_0:+",
      "urn:ogf:network:es.net:2013::sunn-cr5:8_1_1:pacwave",
      "urn:ogf:network:es.net:2013::star-cr5:10_1_8:+",
      "urn:ogf:network:es.net:2013::pnwg-cr5:10_1_2:+",
      "urn:ogf:network:es.net:2013::wash-cr5:6_1_1:wix",
      "urn:ogf:network:es.net:2013::aofa-cr5:to_bnl-rt3_ip-a_v4:+",
      "urn:ogf:network:es.net:2013::atla-cr5:10_1_1:+",
      "urn:ogf:network:es.net:2013::nersc-mr2:xe-7_3_0:xo-osf-rt1",
      "urn:ogf:network:es.net:2013::aofa-cr5:to_manlan_canet_toronto:+",
      "urn:ogf:network:es.net:2013::sunn-cr5:10_1_6:+",
      "urn:ogf:network:es.net:2013::star-cr5:6_1_1:star-tb1",
      "urn:ogf:network:es.net:2013::star-cr5:to_star-tb1_ip-a:+",
      "urn:ogf:network:es.net:2013::star-cr5:10_1_7:+",
      "urn:ogf:network:es.net:2013::chic-cr5:5_1_1:al2s",
      "urn:ogf:network:es.net:2013::fnal-mr2:xe-8_0_0:+",
      "urn:ogf:network:es.net:2013::chic-cr5:3_2_1:+",
      "urn:ogf:network:es.net:2013::newy-cr5:to_bnl-rt3_ip-a_v4:+",
      "urn:ogf:network:es.net:2013::fnal-mr2:xe-7_0_0:+",
      "urn:ogf:network:es.net:2013::aofa-cr5:to_manlan_pilot:+",
      "urn:ogf:network:es.net:2013::fnal-mr2:xe-2_2_0:+",
      "urn:ogf:network:es.net:2013::aofa-cr5:2_1_1:manlan",
      "urn:ogf:network:es.net:2013::lbl-mr2:xe-0_1_0:+",
      "urn:ogf:network:es.net:2013::pnwg-cr5:10_1_7:+",
      "urn:ogf:network:es.net:2013::sacr-cr5:10_1_4:+",
      "urn:ogf:network:es.net:2013::lbl-mr2:xe-9_3_0:+",
      "urn:ogf:network:es.net:2013::atla-cr5:10_1_10:+",
      "urn:ogf:network:es.net:2013::atla-cr5:10_1_11:+",
      "urn:ogf:network:es.net:2013::star-cr5:6_1_1:+",
      "urn:ogf:network:es.net:2013::aofa-cr5:to_manlan_internet2:+",
      "urn:ogf:network:es.net:2013::fnal-mr2:xe-8_3_0:+",
      "urn:ogf:network:es.net:2013::star-cr5:3_1_1:+",
      "urn:ogf:network:es.net:2013::cern-272-cr5:10_1_3:+",
      "urn:ogf:network:es.net:2013::aofa-cr5:to_manlan-mgmt_v4:+",
      "urn:ogf:network:es.net:2013::sunn-cr5:8_1_1:caltech",
      "urn:ogf:network:es.net:2013::aofa-cr5:to_manlan_canet_montreal:+",
      "urn:ogf:network:es.net:2013::atla-cr5:10_1_12:+",
      "urn:ogf:network:es.net:2013::aofa-cr5:5_1_1:star-tb1",
      "urn:ogf:network:es.net:2013::denv-cr5:10_1_11:+",
      "urn:ogf:network:es.net:2013::fnal-mr2:xe-1_3_0:+",
      "urn:ogf:network:es.net:2013::amst-cr5:3_1_1:+",
      "urn:ogf:network:es.net:2013::atla-cr5:4_1_1:+",
      "urn:ogf:network:es.net:2013::star-cr5:6_2_1:umich",
      "urn:ogf:network:es.net:2013::lbl-mr2:xe-8_2_0:+"
  );

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
  }

  @org.junit.jupiter.api.AfterEach
  void tearDown() {
  }

  /**
   * Test for correct model creation.
   *
   * @throws IOException
   */
  @org.junit.jupiter.api.Test
  void newMrmlModel() throws IOException {
    // Build the default model with imported schema.
    OntModel model = ModelUtil.newMrmlModel();
    model.read(new ByteArrayInputStream(Files.readAllBytes(Paths.get(MRML_MODEL_TTL))), null, Lang.TURTLE.getName());

    // Validate the ontology.
    ValidityReport validate = model.validate();
    assertTrue(validate.isValid());

    // Make sure all the models we imported are in the document manager.
    OntDocumentManager documentManager = model.getDocumentManager();
    for (Schema s : ModelUtil.SCHEMA_IMPORTS) {
      Model m = documentManager.getModel(s.getUri());
      assertNotNull(m);
      assertFalse(m.isEmpty());
    }
  }

  /**
   * Test to verify supported encoding types.
   */
  @org.junit.jupiter.api.Test
  void isSupported() {
    assertTrue(ModelUtil.isSupported(Lang.RDFJSON.getName()));
    assertTrue(ModelUtil.isSupported(Lang.TURTLE.getName()));
    assertTrue(ModelUtil.isSupported(Lang.RDFXML.getName()));
    assertFalse(ModelUtil.isSupported(Lang.JSONLD.getName()));
  }

  /**
   * Test to unmarshal MRML ontology.
   */
  @org.junit.jupiter.api.Test
  void unmarshalOntModel() throws IOException, RiotException {
    // Success case.
    Path fileName = Path.of(MRML_MODEL_TTL);
    String str = Files.readString(fileName);
    OntModel m1 = ModelUtil.unmarshalOntModel(str, Lang.TURTLE.getName());
    assertFalse(m1.isEmpty());
    ValidityReport validate = m1.validate();
    assertTrue(validate.isValid());

    // Empty model case.
    OntModel m2 = ModelUtil.unmarshalOntModel("", Lang.TURTLE.getName());
    assertNotNull(m2);
    assertTrue(m2.isEmpty());
    assertTrue(m2.validate().isValid());

    // Invalid model case.
    assertThrows(
        RiotException.class,
        () -> ModelUtil.unmarshalOntModel("broken", Lang.TURTLE.getName()),
        "Expected unmarshalOntModel() to throw, but it didn't"
    );
  }

  @org.junit.jupiter.api.Test
  void unmarshalOntModelTurtle() {

  }

  @org.junit.jupiter.api.Test
  void unmarshalOntModelJson() {
  }

  @org.junit.jupiter.api.Test
  void unmarshalOntModelXml() {
  }

  /**
   * Test to verify loading ontology from a file.
   *
   * @throws IOException
   */
  @org.junit.jupiter.api.Test
  void unmarshalOntModelFromFile() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_TTL, Lang.TURTLE.getName());
    assertFalse(model.isEmpty());
    ValidityReport validate = model.validate();
    assertTrue(validate.isValid());

    model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_JSON, Lang.JSONLD.getName());
    assertFalse(model.isEmpty());
    validate = model.validate();
    assertTrue(validate.isValid());

    model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_XML, Lang.RDFXML.getName());
    assertFalse(model.isEmpty());
    validate = model.validate();
    assertTrue(validate.isValid());

    // Invalid model file.
    assertThrows(IOException.class,
        () -> ModelUtil.unmarshalOntModelFromFile("", Lang.TURTLE.getName()),
        "Expected unmarshalOntModelFromFile() to throw, but it didn't"
    );

    // Invalid encoding.
    assertThrows(RiotException.class,
        () -> ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_TTL, Lang.RDFXML.getName()),
        "Expected unmarshalOntModelFromFile() to throw, but it didn't"
    );
  }

  @org.junit.jupiter.api.Test
  void marshalOntModel() {
  }

  @org.junit.jupiter.api.Test
  void testMarshalOntModel() {
  }

  @org.junit.jupiter.api.Test
  void marshalOntModelXml() {
  }

  @org.junit.jupiter.api.Test
  void marshalOntModelJson() {
  }

  @org.junit.jupiter.api.Test
  void marshalOntModelTurtle() {
  }

  @org.junit.jupiter.api.Test
  void cloneOntModel() {
  }

  @org.junit.jupiter.api.Test
  void isEmptyModel() throws IOException {
    // Build the default model with imported schema.
    OntModel model = ModelUtil.newMrmlModel();

    // Validate the ontology.
    ValidityReport validate = model.validate();
    assertTrue(validate.isValid());

    assertTrue(ModelUtil.isEmptyModel(model));

    model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_TTL, Lang.TURTLE.getName());
    validate = model.validate();
    assertTrue(validate.isValid());

    assertFalse(ModelUtil.isEmptyModel(model));

    model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_1, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(model));

    model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_4, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(model));

    model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_5, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(model));

    model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_1, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(model));

    model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_4, Lang.TURTLE.getName());
    assertTrue(ModelUtil.isEmptyModel(model));

    model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_5, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(model));

  }

  /**
   * Tests to query resources based on id and type.
   *
   * @throws IOException If the ontology fails to load.
   */
  @org.junit.jupiter.api.Test
  void isResourceOfType() throws IOException {
    // Load model containing resources to test.
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_TTL, Lang.TURTLE.getName());

    Resource bandwidthService = model.createResource("urn:ogf:network:es.net:2013::aofa-cr5:to_manlan_canet_toronto:+:BandwidthService");
    assertTrue(ModelUtil.isResourceOfType(model, Mrs.BandwidthService, bandwidthService));

    Resource bidirectionalPort = model.createResource("urn:ogf:network:es.net:2013::aofa-cr5:2_1_1:manlan");
    assertTrue(ModelUtil.isResourceOfType(model, Nml.BidirectionalPort, bidirectionalPort));

    Resource serviceDefinition = model.createResource("urn:ogf:network:es.net:2013::ServiceDefinition:EVTS.A-GOLE");
    assertTrue(ModelUtil.isResourceOfType(model, Sd.ServiceDefinition, serviceDefinition));

    Resource vlan = model.createResource("urn:ogf:network:es.net:2013::star-cr5:6_1_1:+:vlan");
    assertFalse(ModelUtil.isResourceOfType(model, Sd.ServiceDefinition, vlan));
    assertFalse(ModelUtil.isResourceOfType(model, Nml.Label, vlan));
    assertTrue(ModelUtil.isResourceOfType(model, Nml.LabelGroup, vlan));
  }

  @org.junit.jupiter.api.Test
  void getResourcesOfType() throws IOException {
    // Load model containing resources to test.
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_TTL, Lang.TURTLE.getName());

    List<Resource> switchingSubnet = ModelUtil.getResourcesOfType(model, Mrs.SwitchingSubnet);
    assertTrue(switchingSubnet.isEmpty());

    List<Resource> switchingService = ModelUtil.getResourcesOfType(model, Nml.SwitchingService);
    assertFalse(switchingService.isEmpty());
    assertLinesMatch(SWITCHINGSERVICES, switchingService.stream().map(Resource::getURI).toList());

    List<Resource> bidirectionalPort = ModelUtil.getResourcesOfType(model, Nml.BidirectionalPort);
    assertFalse(bidirectionalPort.isEmpty());
    assertLinesMatch(BIDIRECTIONALPORTS, bidirectionalPort.stream().map(Resource::getURI).toList());
  }

  @org.junit.jupiter.api.Test
  void getResourceOfIdAndType() throws IOException {
    // Load model containing resources to test.
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_TTL, Lang.TURTLE.getName());

    // Success cases.
    SWITCHINGSERVICES.forEach(s -> {
      Resource r = ModelUtil.getResourceOfSubjectAndType(model, Nml.SwitchingService, s);
      assertNotNull(r);
      assertEquals(s, r.getURI());
    });

    BIDIRECTIONALPORTS.forEach(s -> {
      Resource r = ModelUtil.getResourceOfSubjectAndType(model, Nml.BidirectionalPort, s);
      assertNotNull(r);
      assertEquals(s, r.getURI());
    });

    SWITCHINGSERVICES.forEach(s -> {
      Resource ss = model.createResource(s);
      Resource r = ModelUtil.getResourceOfSubjectAndType(model, Nml.SwitchingService, ss);
      assertNotNull(r);
      assertEquals(s, r.getURI());
    });

    BIDIRECTIONALPORTS.forEach(s -> {
      Resource bp = model.createResource(s);
      Resource r = ModelUtil.getResourceOfSubjectAndType(model, Nml.BidirectionalPort, bp);
      assertNotNull(r);
      assertEquals(s, r.getURI());
    });

    // Failure cases.
    assertNull(ModelUtil.getResourceOfSubjectAndType(model, Nml.BidirectionalPort,
        "urn:ogf:network:es.net:2013::aofa-cr5:to_manlan_canet_toronto:+:BandwidthService"));
  }

  @org.junit.jupiter.api.Test
  void getSubjectsOfPredicateRelationship() throws IOException {
    // Load model containing resources to test.
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_TTL, Lang.TURTLE.getName());

    Resource target = model.createResource("urn:ogf:network:es.net:2013::sunn-cr5:8_1_1:caltech");

    List<Resource> list = ModelUtil.getSubjectsOfPredicateRelationship(model, Nml.hasBidirectionalPort, target);
    assertLinesMatch(
        Stream.of("urn:ogf:network:es.net:2013:",
            "urn:ogf:network:es.net:2013::ServiceDomain:EVTS.A-GOLE").sorted().toList(),
        list.stream().map(Resource::getURI).sorted().toList());

    OntModel addition = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_1, Lang.TURTLE.getName());
    OntModel aModel = ModelUtil.mergeModels(model, addition);

    list = ModelUtil.getSubjectsOfPredicateRelationship(aModel, Nml.hasBidirectionalPort, target);
    assertLinesMatch(
        Stream.of("urn:ogf:network:es.net:2013:",
                "urn:ogf:network:es.net:2013::ServiceDomain:EVTS.A-GOLE").sorted().toList(),
        list.stream().map(Resource::getURI).sorted().toList());

    target = model.createResource("urn:ogf:network:es.net:2013::sunn-cr5:8_1_1:caltech:vlanport+3615");
    list = ModelUtil.getSubjectsOfPredicateRelationship(aModel, Nml.hasBidirectionalPort, target);
    assertLinesMatch(
        Stream.of("urn:ogf:network:es.net:2013::sunn-cr5:8_1_1:caltech",
            "urn:ogf:network:es.net:2013::ServiceDomain:EVTS.A-GOLE:vlan+3615").sorted().toList(),
        list.stream().map(Resource::getURI).sorted().toList());
  }

  @org.junit.jupiter.api.Test
  void getParentBidirectionalPort() throws IOException {
    // Load model containing resources to test.
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_TTL, Lang.TURTLE.getName());
    OntModel addition = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_1, Lang.TURTLE.getName());
    ModelUtil.applyDeltaAddition(model, addition);
    Resource parent = model.createResource("urn:ogf:network:es.net:2013::sunn-cr5:8_1_1:caltech");
    Resource child = model.createResource("urn:ogf:network:es.net:2013::sunn-cr5:8_1_1:caltech:vlanport+3615");

    // Success case.
    Resource result = ModelUtil.getParentBidirectionalPort(model, child);
    assertNotNull(result);
    assertTrue(ModelUtil.isResourceOfType(model, Nml.BidirectionalPort, result));
    assertEquals(parent.getURI(), result.getURI());

    // Failure case.
    result = ModelUtil.getParentBidirectionalPort(model,
        model.createResource("urn:ogf:network:es.net:2013::anl-mr2:xe-1_2_0:+"));
    assertNull(result);
  }

  /**
   * Test model addition where a model diff is applied to an existing model.
   *
   * @throws IOException
   */
  @org.junit.jupiter.api.Test
  void applyDeltaAddition() throws IOException {
    // Load the base model.
    OntModel original = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_1, Lang.TURTLE.getName());
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_1, Lang.TURTLE.getName());
    OntModel addition = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_1, Lang.TURTLE.getName());

    // Validate the ontology.
    assertTrue(original.validate().isValid());
    assertTrue(model.validate().isValid());
    assertTrue(addition.validate().isValid());

    // Apply the addition to the base model.
    ModelUtil.applyDeltaAddition(model, addition);

    // Verify root topology.
    Resource topology = model.createResource("urn:ogf:network:es.net:2013:");
    topology = ModelUtil.getResourceOfSubjectAndType(model, Nml.Topology, topology);
    assertNotNull(topology);

    // Get the list of BiDirectional ports rooted in this modified topology.
    List<Resource> biPorts = ModelUtil.getObjectsOfPredicateRelationship(model, topology, Nml.hasBidirectionalPort);

    // Do the same for the original topology.
    Resource oTopology = original.createResource("urn:ogf:network:es.net:2013:");
    oTopology = ModelUtil.getResourceOfSubjectAndType(model, Nml.Topology, oTopology);
    assertNotNull(oTopology);
    List<Resource> oBiPorts = ModelUtil.getObjectsOfPredicateRelationship(original, oTopology, Nml.hasBidirectionalPort);

    // Verify the list is identical after the addition.
    assertLinesMatch(oBiPorts.stream().map(Resource::getURI).sorted().toList(),
        biPorts.stream().map(Resource::getURI).sorted().toList());

    // Now compare the list off all biDirectional ports.  It should be different.
    oBiPorts = ModelUtil.getResourcesOfType(original, Nml.BidirectionalPort);
    biPorts = ModelUtil.getResourcesOfType(model, Nml.BidirectionalPort);
    assertNotEquals(oBiPorts.stream().map(Resource::getURI).sorted().toList(),
        biPorts.stream().map(Resource::getURI).sorted().toList());

    // Check to see if a newly added Bidirectional port has been applied.
    Resource port = model.createResource("urn:ogf:network:es.net:2013::sunn-cr5:8_1_1:caltech:vlanport+3615");
    assertNull(ModelUtil.getBidirectionalPort(original, port));
    assertNotNull(ModelUtil.getBidirectionalPort(model, port));
  }

  /**
   * Test model reduction where a model diff is applied to an existing model.
   */
  @org.junit.jupiter.api.Test
  void applyDeltaReduction() {
  }

  @org.junit.jupiter.api.Test
  void getSubjectsModel() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_1, Lang.TURTLE.getName());
    List<Resource> results = ModelUtil.getSubjects(model);
    assertNotNull(results);
    assertFalse(results.isEmpty());

    List<String> list = results.stream().map(Resource::getURI).toList();
    assertFalse(list.isEmpty());
    list.forEach(uri -> log.debug("[getSubjectsModel] {}", uri));

    assertTrue(list.contains("urn:ogf:network:es.net:2013:"));
    assertTrue(list.contains("urn:ogf:network:es.net:2013::chic-cr5:5_1_1:al2s"));
    assertFalse(list.contains("urn:ogf:network:es.net:2013::sunn-cr5:8_1_1:caltech:vlanport+3615"));
  }

  @org.junit.jupiter.api.Test
  void getMrsTag() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_1, Lang.TURTLE.getName());
    List<Resource> results = ModelUtil.getSubjects(model);
    assertNotNull(results);
    assertFalse(results.isEmpty());

    Resource switchingSubnet = ModelUtil.getResourceOfSubjectAndType(model, Mrs.SwitchingSubnet,
        "urn:ogf:network:es.net:2013::switchingSubnet:62dca40f-929b-40fe-8aa1-c6a4ea62cdcd");
    assertNotNull(switchingSubnet);

    String tag = ModelUtil.getMrsTag(switchingSubnet);
    assertNotNull(tag);
    assertEquals("serviceId=urn:uuid:62dca40f-929b-40fe-8aa1-c6a4ea62cdcd", tag);
  }
  
  @org.junit.jupiter.api.Test
  void subjectsReduction_1() throws IOException {
    OntModel reduction = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_1, Lang.TURTLE.getName());
    List<Resource> results = ModelUtil.getSubjects(reduction);
    assertNotNull(results);
    assertFalse(results.isEmpty());

    List<String> list = results.stream().map(Resource::getURI).toList();
    assertFalse(list.isEmpty());
    list.forEach(uri -> log.debug("[subjectsReduction_1] {}", uri));

    assertTrue(list.contains("urn:ogf:network:es.net:2013::chic-cr5:3_2_1:+"));
    assertTrue(list.contains("urn:ogf:network:es.net:2013::chic-cr5:3_2_1:+:vlanport+3608"));
    assertTrue(list.contains("urn:ogf:network:es.net:2013::ServiceDomain:EVTS.A-GOLE:vlan+3608"));
    assertFalse(list.contains("http://www.w3.org/2002/07/owl#Nothing"));
  }

  @org.junit.jupiter.api.Test
  void subjectsReduction_4() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_4, Lang.TURTLE.getName());
    List<Resource> results = ModelUtil.getSubjects(model);
    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  @org.junit.jupiter.api.Test
  void subjectsAddition_4() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_4, Lang.TURTLE.getName());
    List<Resource> results = ModelUtil.getSubjects(model);
    assertNotNull(results);
    results.forEach(uri -> log.debug("[subjectsAddition_4] {}", uri));
    assertEquals(10, results.size());
  }

  @org.junit.jupiter.api.Test
  void subjectsReduction_5() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_5, Lang.TURTLE.getName());
    List<Resource> results = ModelUtil.getSubjects(model);
    assertNotNull(results);
    assertFalse(results.isEmpty());

    List<String> list = results.stream().map(Resource::getURI).toList();
    assertFalse(list.isEmpty());
    list.forEach(uri -> log.debug("[subjectsReduction_5] {}", uri));

    assertTrue(list.contains("urn:ogf:network:es.net:2013::lbnl59-cr6:2_1_c6_1:+:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlanport+1717:service+bw"));
    assertTrue(list.contains("urn:ogf:network:es.net:2013::sunn-cr6:1_1_c3_1:+:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlanport+1717:service+bw"));
    assertFalse(list.contains("http://www.w3.org/2002/07/owl#TransitiveProperty"));
    assertFalse(list.contains("http://www.w3.org/2002/07/owl#Ontology"));
  }

  @org.junit.jupiter.api.Test
  void getResourceTypeUri() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_1, Lang.TURTLE.getName());
    List<Resource> biPorts = ModelUtil.getResourcesOfType(model, Nml.BidirectionalPort);
    biPorts.forEach(b -> {
      String resourceTypeUri = ModelUtil.getResourceTypeUri(b);
      assertEquals(Nml.BidirectionalPort.getURI(), resourceTypeUri);
    });

    List<Resource> ss = ModelUtil.getResourcesOfType(model, Nml.SwitchingService);
    ss.forEach(s -> {
      String resourceTypeUri = ModelUtil.getResourceTypeUri(s);
      assertEquals(Nml.SwitchingService.getURI(), resourceTypeUri);
    });
  }

  @org.junit.jupiter.api.Test
  void isXxxTest() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_1, Lang.TURTLE.getName());
    List<Resource> biPorts = ModelUtil.getResourcesOfType(model, Nml.BidirectionalPort);
    biPorts.forEach(r -> {
      assertTrue(ModelUtil.isBidirectionalPort(r));
    });

    List<Resource> ss = ModelUtil.getResourcesOfType(model, Nml.SwitchingService);
    ss.forEach(r -> {
      assertTrue(ModelUtil.isSwitchingService(r));
    });

    List<Resource> sws = ModelUtil.getResourcesOfType(model, Mrs.SwitchingSubnet);
    sws.forEach(r -> {
      assertTrue(ModelUtil.isSwitchingSubnet(r));
    });

    List<Resource> bs = ModelUtil.getResourcesOfType(model, Mrs.BandwidthService);
    bs.forEach(r -> {
      assertTrue(ModelUtil.isBandwidthService(r));
    });

  }

  private final static List<String> EXISTSDURING = Stream.of(
    "urn:ogf:network:es.net:2013::sunn-cr6:1_1_c3_1:+:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlanport+1717:label+1717",
    "urn:ogf:network:es.net:2013::lbnl59-cr6:2_1_c6_1:+:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlanport+1717",
    "urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlan+1717",
    "urn:ogf:network:es.net:2013::lbnl59-cr6:2_1_c6_1:+:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlanport+1717:label+1717",
    "urn:ogf:network:es.net:2013::sunn-cr6:1_1_c3_1:+:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlanport+1717:service+bw",
    "urn:ogf:network:es.net:2013::sunn-cr6:1_1_c3_1:+:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlanport+1717",
    "urn:ogf:network:es.net:2013::lbnl59-cr6:2_1_c6_1:+:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlanport+1717:service+bw"
  ).sorted().toList();

  @org.junit.jupiter.api.Test
  void getAllTests() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_5, Lang.TURTLE.getName());

    // Verify we get the correct set of resources containing the specified existsDuring element.
    Resource existsDuring = model.getResource("urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlan+1717:existsDuring");
    List<Resource> exists = ModelUtil.getAllWithExistsDuring(model, existsDuring);
    assertEquals(7, exists.size());
    assertEquals(EXISTSDURING, exists.stream().map(Resource::getURI).sorted().toList());

    List<Resource> ss = ModelUtil.getAllSwitchingSubnetWithExistsDuring(model, existsDuring);
    assertEquals(1, ss.size());
    assertEquals("urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlan+1717", ss.stream().findFirst().get().getURI());
  }
}