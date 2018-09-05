package net.es.sense.rm.driver.nsi.db;

import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ModelService.class, ModelServiceBean.class, ModelRepository.class, Model.class, DbUnitTestConfiguration.class, JsonProxy.class, JsonExclusionStrategy.class })
@DataJpaTest
@ActiveProfiles("test")
public class ModelServiceTest {
  private static final String MODEL_FILE = "src/test/resources/models.json";

  @Autowired
  private ModelService modelService;

  public void buildDatabase() throws JAXBException, IOException, DatatypeConfigurationException {
    log.info("[ModelServiceTest] Reading model file: {}", MODEL_FILE);
    JsonProxy json = new JsonProxy();
    FileReader fileReader = new FileReader(MODEL_FILE);
    List<Model> input = json.deserializeList(fileReader, Model.class);
    log.info("[ModelServiceTest] Reading model entries: {}", input.size());

    // Clear the contents of the database so he have a fresh start.
    modelService.delete();

    for (Model model : input) {
      log.info("[ModelServiceTest] Adding model idx {} to database.", model.getIdx());
      modelService.create(model);
    }
  }

  /**
   * Add tests as more API are used by the nsi-driver.
   *
   * @throws JAXBException
   * @throws IOException
   * @throws DatatypeConfigurationException
   */
  @Test
  public void verify() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();

    // Count the models.
    Assert.assertEquals(3, modelService.countByTopologyId("urn:ogf:network:es.net:2013:"));

    // Get a specific model.
    Model model = modelService.getByModelId("eb9efcf8-4987-430e-a47e-b456dcd8f46c");
    Assert.assertNotNull(model);
    Assert.assertEquals(1506766513000L, model.getVersion());
    Assert.assertEquals("urn:ogf:network:es.net:2013:", model.getTopologyId());
    Assert.assertEquals("eb9efcf8-4987-430e-a47e-b456dcd8f46c", model.getModelId());

    // Get current model.
    model = modelService.getCurrent("urn:ogf:network:es.net:2013:");
    Assert.assertNotNull(model);
    Assert.assertEquals(1506852913000L, model.getVersion());
    Assert.assertEquals("urn:ogf:network:es.net:2013:", model.getTopologyId());
    Assert.assertEquals("dd58cadb-55e0-410c-891a-ddb2666e100b", model.getModelId());

    // Get all the topologies.
    Collection<Model> models = modelService.get();
    Assert.assertEquals(18, models.size());

    models = modelService.getByTopologyId("urn:ogf:network:es.net:2013:");
    Assert.assertEquals(3, models.size());

    models = modelService.getByTopologyId("urn:ogf:network:example.net:2013:");
    Assert.assertEquals(15, models.size());

    // Test ifModifiedSince - should return no results.
    models = modelService.getByTopologyId("urn:ogf:network:es.net:2013:", 1506852913000L);
    Assert.assertEquals(0, models.size());

    // Test ifModifiedSince - should return no results.
    models = modelService.getByTopologyId("urn:ogf:network:es.net:2013:", 1506680113001L);
    Assert.assertEquals(2, models.size());

    // Test ifModifiedSince - should return 1 result.
    models = modelService.getByTopologyId("urn:ogf:network:es.net:2013:", 1506766513000L);
    Assert.assertEquals(1, models.size());

    // Test the update of a model.
    model = modelService.getByModelId("dd58cadb-55e0-410c-891a-ddb2666e100b");
    Assert.assertNotNull(model);
    Assert.assertEquals(1506852913000L, model.getVersion());
    Assert.assertEquals("urn:ogf:network:es.net:2013:", model.getTopologyId());
    Assert.assertEquals("dd58cadb-55e0-410c-891a-ddb2666e100b", model.getModelId());
    long time = System.currentTimeMillis();
    model.setVersion(time);

    Model update = modelService.update(model);
    Assert.assertNotNull(update);
    Assert.assertEquals(time, update.getVersion());

    // Test if we read the object is comes back changed.
    model = modelService.getByModelId("dd58cadb-55e0-410c-891a-ddb2666e100b");
    Assert.assertNotNull(model);
    Assert.assertEquals("urn:ogf:network:es.net:2013:", model.getTopologyId());
    Assert.assertEquals("dd58cadb-55e0-410c-891a-ddb2666e100b", model.getModelId());
    Assert.assertEquals(time, model.getVersion());

    // Now make a new object, copy the contents, update the time.
    Model newModel = new Model();
    newModel.setIdx(model.getIdx());
    newModel.setTopologyId(model.getTopologyId());
    newModel.setModelId(model.getModelId());
    newModel.setBase(model.getBase());
    time = System.currentTimeMillis();
    newModel.setVersion(time);

    update = modelService.update(newModel);
    Assert.assertNotNull(update);
    Assert.assertEquals(time, update.getVersion());
    Assert.assertEquals(newModel.getIdx(), update.getIdx());

    // Test if we read the object is comes back changed.
    model = modelService.getByModelId("dd58cadb-55e0-410c-891a-ddb2666e100b");
    Assert.assertNotNull(model);
    Assert.assertEquals("urn:ogf:network:es.net:2013:", model.getTopologyId());
    Assert.assertEquals("dd58cadb-55e0-410c-891a-ddb2666e100b", model.getModelId());
    Assert.assertEquals(time, model.getVersion());
  }

  @Test
  public void purge() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();

    Collection<Model> models = modelService.getByTopologyId("urn:ogf:network:example.net:2013:");
    models.parallelStream().forEach(System.out::println);
    Assert.assertEquals(15, models.size());

    modelService.purge("urn:ogf:network:example.net:2013:", 10);

    models = modelService.getByTopologyId("urn:ogf:network:example.net:2013:");
    models.parallelStream().forEach(System.out::println);
    Assert.assertEquals(10, models.size());

  }
}
