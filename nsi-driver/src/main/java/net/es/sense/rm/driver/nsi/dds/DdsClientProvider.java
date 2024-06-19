package net.es.sense.rm.driver.nsi.dds;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.lib.client.DdsClient;
import net.es.nsi.dds.lib.client.HttpsContext;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * The NSI-DDS client provider is a singleton that caches an NSI-DDS client to increase performance.
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class DdsClientProvider {
  private final DdsClient client;

  @Autowired
  public DdsClientProvider(NsiProperties nsiProperties) throws UnrecoverableKeyException, CertificateException,
      KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException, NoSuchProviderException {
    // If there is a secure SSL context specified then we process it.

    if (nsiProperties.getSecure() != null) {
      HttpsContext.getInstance().load(nsiProperties.getSecure());
    }

    // We need to initialize the DDS client with specified configuration.
    if (nsiProperties.getClient().isSecure()) {
      client = new DdsClient(nsiProperties.getClient(), HttpsContext.getInstance());
    } else {
      client = new DdsClient(nsiProperties.getClient());
    }
  }

  public static DdsClientProvider getInstance() {
    return SpringApplicationContext.getBean("ddsClientProvider", DdsClientProvider.class);
  }

  public DdsClient get() {
    return client;
  }
}
