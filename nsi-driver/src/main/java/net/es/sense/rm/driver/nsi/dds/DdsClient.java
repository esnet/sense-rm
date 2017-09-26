package net.es.sense.rm.driver.nsi.dds;

import net.es.nsi.dds.lib.client.RestClient;
import net.es.sense.rm.driver.nsi.spring.SpringApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Component
public class DdsClient extends RestClient {

  public static DdsClient getInstance() {
    DdsClient instance = SpringApplicationContext.getBean("ddsClient", DdsClient.class);
    return instance;
  }
}
