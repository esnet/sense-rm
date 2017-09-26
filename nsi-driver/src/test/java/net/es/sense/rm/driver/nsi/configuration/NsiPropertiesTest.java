package net.es.sense.rm.driver.nsi.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.CoreUnitTestConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
/**
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CoreUnitTestConfiguration.class)
@ActiveProfiles("test")
@ContextConfiguration
public class NsiPropertiesTest {

  @Autowired
  private NsiProperties config;

  private NsiProperties nsi;

  @Before
  public void before() throws IOException {
    File file = new File("src/test/resources/nsi.yaml");
    File absoluteFile = file.getAbsoluteFile();
    YAMLFactory yamlFactory = new YAMLFactory();
    ObjectMapper mapper = new ObjectMapper(yamlFactory);
    mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
    nsi = mapper.readValue(absoluteFile, NsiProperties.class);
  }

  @Test
  public void testEquality() {
    Assert.assertEquals(nsi.getDdsUrl(), config.getDdsUrl());
    Assert.assertEquals(nsi.getProxy(), config.getProxy());
    Assert.assertEquals(nsi.getNsaId(), config.getNsaId());
    Assert.assertEquals(nsi.getDdsAuditTimer(), config.getDdsAuditTimer());
    Assert.assertEquals(nsi.getPeers().size(), config.getPeers().size());
    Assert.assertTrue(nsi.getPeers().containsAll(config.getPeers()));
    Assert.assertEquals(nsi.getProviderNsaId(), config.getProviderNsaId());
    Assert.assertEquals(nsi.getNetworkId(), config.getNetworkId());
    Assert.assertFalse(config.getServer().getSecure().isProduction());
  }
}