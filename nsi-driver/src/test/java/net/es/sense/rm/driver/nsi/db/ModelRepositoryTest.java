package net.es.sense.rm.driver.nsi.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import lombok.extern.slf4j.Slf4j;
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
@SpringBootTest(classes = { ModelRepository.class, Model.class, DbUnitTestConfiguration.class })
@DataJpaTest
@ActiveProfiles("test")
public class ModelRepositoryTest {

  @Autowired
  private ModelRepository models;

  public void buildDatabase() throws JAXBException, IOException, DatatypeConfigurationException {
    // Clear the contents of the database so he have a fresh start.
    models.deleteAll();
    Model model = new Model();
    model.setModelId("ce4762ef-5bb3-420b-8dfd-1b1f099ed4ff");
    model.setTopologyId("urn:ogf:network:es.net:2013:");
    model.setVersion("bignumber:largenumber");
    model.setCreated(0);
    model.setBase(new String(Files.readAllBytes(Paths.get("src/test/resources/mrml.ttl"))));
    log.info("Model id: {}", model.getModelId());
    models.save(model);
  }

  @Test
  public void verify() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();

    for (Model model : models.findAll()) {
      log.info("id: {}", model.getModelId());
    }

  }
}
