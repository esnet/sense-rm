package net.es.sense.rm.driver.nsi.db;

import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.lib.jaxb.dds.ObjectFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
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
  private TestEntityManager entityManager;

  @Autowired
  private ModelRepository models;

  private final ObjectFactory FACTORY = new ObjectFactory();

  public void buildDatabase() throws JAXBException, IOException, DatatypeConfigurationException {
    // Clear the contents of the database so he have a fresh start.
    models.deleteAll();
    Model model = new Model();
    log.info("Model id: {}", model.getId());
    models.save(model);
  }

  @Test
  public void verify() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();

    for (Model model : models.findAll()) {
      log.info("id: {}", model.getId());
    }

  }
}
