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
      log.info("[ModelServiceTest] Adding model id {} to database.", model.getId());
      modelService.create(model);
    }
  }

  @Test
  public void verify() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();

    Collection<Model> models = modelService.get();
    Assert.assertEquals(3, models.size());

    // Get current model.
    models = modelService.get(true, "urn:ogf:network:es.net:2013:");
    Assert.assertEquals(1, models.size());
    Assert.assertTrue(models.stream().findFirst().isPresent());
    models.stream().findFirst().ifPresent(m -> {
      Assert.assertEquals(1506852913000L, m.getVersion());
      Assert.assertEquals("urn:ogf:network:es.net:2013:", m.getTopologyId());
      Assert.assertEquals("dd58cadb-55e0-410c-891a-ddb2666e100b", m.getModelId());
    });

    // Test ifModifiedSince - should return no results.
    models = modelService.get(1506852913000L, true, "urn:ogf:network:es.net:2013:");
    Assert.assertEquals(0, models.size());

    // Test ifModifiedSince - should return no results.
    models = modelService.get(1506852913000L, false, "urn:ogf:network:es.net:2013:");
    Assert.assertEquals(0, models.size());

    // Test ifModifiedSince - should return 1 result.
    models = modelService.get(1506766513000L, true, "urn:ogf:network:es.net:2013:");
    Assert.assertEquals(1, models.size());
  }
}
