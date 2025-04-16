package net.es.sense.rm.driver.nsi.mrml;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.nsi.db.ModelService;
import net.es.sense.rm.driver.schema.Mrs;
import net.es.sense.rm.driver.schema.Nml;
import net.es.sense.rm.model.DeltaRequest;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class ModelAdditionTest {

  private static final String MODEL_FILE = "src/test/resources/original-6.ttl";
  private static final String ADDITION_FILE = "src/test/resources/addition-6.ttl";
  private static final String REDUCTION_FILE = "src/test/resources/reduction-6.ttl";

  @Test
  public void readTtl() throws Exception {
    // Get the models for this delta operation.
    Path model = Path.of(MODEL_FILE);
    OntModel originalModel = ModelUtil.unmarshalOntModelTurtle(Files.readString(model));
    OntModel updatedModel = ModelUtil.unmarshalOntModelTurtle(Files.readString(model));

    Optional<OntModel> reduction = Optional.of(ModelUtil.unmarshalOntModelTurtle(Files.readString(Path.of(REDUCTION_FILE))));
    reduction.ifPresent(r -> ModelUtil.applyDeltaReduction(updatedModel, r));

    Optional<OntModel> addition = Optional.of(ModelUtil.unmarshalOntModelTurtle(Files.readString(Path.of(ADDITION_FILE))));
    addition.ifPresent(a -> ModelUtil.applyDeltaAddition(updatedModel, a));

    assertTrue(originalModel.validate().isValid());
    assertTrue(updatedModel.validate().isValid());

    List<Resource> switchingService =  ModelUtil.getResourcesOfType(originalModel, Nml.SwitchingService);
    assertEquals(1, switchingService.size());
    assertEquals("urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE",
        switchingService.get(0).getURI());

    List<Resource> switchingSubnet = ModelUtil.getResourcesOfType(updatedModel, Mrs.SwitchingSubnet);
    assertEquals(1, switchingSubnet.size());
    assertEquals("urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+48afebf5-d42f-4d04-a178-4d2cac603552:vt+l2-policy-Connection_1:vlan+2541",
        switchingSubnet.get(0).getURI());

    Resource resourceOfSubjectAndType = ModelUtil.getResourceOfSubjectAndType(updatedModel, Mrs.SwitchingSubnet,
        "urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+48afebf5-d42f-4d04-a178-4d2cac603552:vt+l2-policy-Connection_1:vlan+2541");
    assertNotNull(resourceOfSubjectAndType);

    List<String> creates = new ArrayList<>();
    addition.ifPresent(a -> {
      for (Resource subject : ModelUtil.getSubjects(a)) {
        log.debug("[ModelAdditionTest::readTtl] subject {}", subject.getURI());

        // Verify that the subject of the addition is not in the original model but is in the
        // updated model.
        if (ModelUtil.getResourceOfSubjectAndType(originalModel, Mrs.SwitchingSubnet, subject) == null) {
          // Looks like this is a new subject so check to see if it is a mrs:SwitchingSubnet.
          if (ModelUtil.getResourceOfSubjectAndType(updatedModel, Mrs.SwitchingSubnet, subject) != null) {
            log.debug("[ModelAdditionTest::readTtl] SwitchingSubnet {} is in updated model.", subject.getURI());
            // The mrs:SwitchingSubnet is present in the new model so add it for creation.
            creates.add(subject.getURI());
          }
        }
      }
    });

    assertEquals(1, creates.size());
  }
}
