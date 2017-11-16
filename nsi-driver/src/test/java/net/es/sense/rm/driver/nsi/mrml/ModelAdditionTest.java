package net.es.sense.rm.driver.nsi.mrml;

import net.es.sense.rm.driver.nsi.db.ModelService;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class ModelAdditionTest {

  private static final String MODEL_FILE = "src/test/resources/model-1.ttl";
  private static final String ADDITION_FILE = "src/test/resources/addition-1.ttl";
  private static final String MODEL_ID = "urn:uuid:c87c0ac1-e02b-484c-b595-e58d807a7661";

  private final ModelService modelService = Mockito.mock(ModelService.class);
  private static final Logger LOG = LoggerFactory.getLogger(ModelAdditionTest.class);

  @Test
  public void readTtl() throws Exception {
    // Get the models for this delta operation.
    /*Model m = ModelFactory.createDefaultModel();
    m.read(MODEL_FILE, "TURTLE");

    net.es.sense.rm.driver.nsi.db.Model model = new net.es.sense.rm.driver.nsi.db.Model();
    model.setModelId(MODEL_ID);
    model.setVersion(0);
    model.setTopologyId("urn:ogf:network:es.net:2013:");
    model.setBase(ModelUtil.marshalModel(m));

    Mockito.when(modelService.get(MODEL_ID)).thenReturn(model);

    // Get the models for this delta operation.
    Model addition = ModelFactory.createDefaultModel();
    addition.read(ADDITION_FILE, "TURTLE");

    DeltaRequest request = new DeltaRequest();
    request.setModelId("urn:uuid:c87c0ac1-e02b-484c-b595-e58d807a7661");
    request.setAddition(ModelUtil.marshalModel(addition));

    processDeltaAddition(request); */
  }
}
