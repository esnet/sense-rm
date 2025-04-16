package net.es.sense.rm.driver.nsi.cs;

import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class CsProviderTest {
  private final static String MRML_MODEL_2 = "src/test/resources/model-2.ttl";
  private final static String MRML_MODEL_ADDITION_2 = "src/test/resources/addition-2.ttl";
  private final static String MRML_MODEL_REDUCTION_2 = "src/test/resources/reduction-2.ttl";
  private final static String MRML_MODEL_3 = "src/test/resources/model-3.ttl";
  private final static String MRML_MODEL_ADDITION_3 = "src/test/resources/addition-3.ttl";
  private final static String MRML_MODEL_REDUCTION_3 = "src/test/resources/reduction-3.ttl";
  private final static String MRML_MODEL_4 = "src/test/resources/model-4.ttl";
  private final static String MRML_MODEL_ADDITION_4 = "src/test/resources/addition-4.ttl";
  private final static String MRML_MODEL_REDUCTION_4 = "src/test/resources/reduction-4.ttl";
  private final static String MRML_MODEL_5 = "src/test/resources/model-5.ttl";
  private final static String MRML_MODEL_ADDITION_5 = "src/test/resources/addition-5.ttl";
  private final static String MRML_MODEL_REDUCTION_5 = "src/test/resources/reduction-5.ttl";

  @org.junit.jupiter.api.Test
  void getReductionTerminates() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_2, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(model));

    OntModel updated = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_2, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(updated));

    OntModel reduction = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_2, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(reduction));

    OntModel addition = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_2, Lang.TURTLE.getName());
    assertTrue(ModelUtil.isEmptyModel(addition));

    ModelUtil.applyDeltaReduction(updated, reduction);
    ModelUtil.applyDeltaAddition(updated, addition);

    List<String> terminates = CsProvider.getReductionTerminates(model, updated, reduction);
    terminates.forEach(t -> log.debug("[getReductionTerminates] uri = {}", t));
    assertEquals(2, terminates.size());
    assertTrue(terminates.contains("urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db10:vt+l2-policy-Connection_1:vlan+3989"));
    assertTrue(terminates.contains("urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db10:vt+l2-policy-Connection_1:vlan+2012"));
  }

  @org.junit.jupiter.api.Test
  void getAdditionCreatesNegative() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_2, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(model));

    OntModel updated = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_2, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(updated));

    OntModel reduction = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_2, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(reduction));

    OntModel addition = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_2, Lang.TURTLE.getName());
    assertTrue(ModelUtil.isEmptyModel(addition));

    ModelUtil.applyDeltaReduction(updated, reduction);
    ModelUtil.applyDeltaAddition(updated, addition);

    List<String> creates = CsProvider.getAdditionCreates(model, updated, reduction);
    creates.forEach(t -> log.debug("[getAdditionCreatesNegative] uri = {}", t));
    assertEquals(0, creates.size());
  }

  @org.junit.jupiter.api.Test
  void getAdditionCreatesPositive() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_3, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(model));

    OntModel updated = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_3, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(updated));

    OntModel reduction = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_3, Lang.TURTLE.getName());
    assertTrue(ModelUtil.isEmptyModel(reduction));

    OntModel addition = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_3, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(addition));

    ModelUtil.applyDeltaReduction(updated, reduction);
    ModelUtil.applyDeltaAddition(updated, addition);

    List<String> creates = CsProvider.getAdditionCreates(model, updated, addition);
    creates.forEach(c -> log.debug("[getAdditionCreatesNegative] uri = {}", c));
    assertEquals(2, creates.size());
    assertTrue(creates.contains("urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db10:vt+l2-policy-Connection_1:vlan+3989"));
    assertTrue(creates.contains("urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db10:vt+l2-policy-Connection_1:vlan+2012"));
  }

  @org.junit.jupiter.api.Test
  void getModifiedSwitchingSubnetsNegative() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_3, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(model));

    OntModel updated = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_3, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(updated));

    OntModel reduction = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_3, Lang.TURTLE.getName());
    assertTrue(ModelUtil.isEmptyModel(reduction));

    OntModel addition = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_3, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(addition));

    ModelUtil.applyDeltaReduction(updated, reduction);
    ModelUtil.applyDeltaAddition(updated, addition);

    // This test should have no modifications since it is all new resources.
    Set<Resource> modifications = CsProvider.getModifiedSwitchingSubnets(model, updated, addition);
    modifications.forEach(c -> log.debug("[getModifiedSwitchingSubnetsNegative] uri = {}", c.getURI()));
    assertEquals(0, modifications.size());
  }

  @org.junit.jupiter.api.Test
  void getModifiedBandwidthServicesPositive() throws IOException {
    OntModel model = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_5, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(model));

    OntModel updated = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_5, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(updated));

    OntModel reduction = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_REDUCTION_5, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(reduction));

    OntModel addition = ModelUtil.unmarshalOntModelFromFile(MRML_MODEL_ADDITION_5, Lang.TURTLE.getName());
    assertFalse(ModelUtil.isEmptyModel(addition));

    ModelUtil.applyDeltaReduction(updated, reduction);
    ModelUtil.applyDeltaAddition(updated, addition);

    log.debug("[getModifiedBandwidthServicesPositive] testing");

    // Find modified SwitchingSubnet.
    Set<Resource> modifiedSwitchingSubnets = CsProvider.getModifiedSwitchingSubnets(model, updated, addition);

    modifiedSwitchingSubnets.forEach(c -> log.debug("[getModifiedBandwidthServicesPositive] uri = {}", c.getURI()));
    assertEquals(1, modifiedSwitchingSubnets.size());

    assertTrue(modifiedSwitchingSubnets.stream().map(Resource::getURI).toList()
        .contains("urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+6af3a5f3-a3ec-4fee-ad6b-dc5fb9e7cb7b:vt+l2-policy-Connection_1:vlan+1717"));
  }
}