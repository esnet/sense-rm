package net.es.sense.rm.driver.nsi.dds;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.lib.client.DdsClient;
import net.es.sense.rm.driver.nsi.spring.SpringApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class DdsClientProvider {
  DdsClient client;

  public DdsClientProvider() {
    client = new DdsClient();
  }

  public static DdsClientProvider getInstance() {
    DdsClientProvider instance = SpringApplicationContext.getBean("ddsClientProvider", DdsClientProvider.class);
    return instance;
  }

  public DdsClient get() {
    return client;
  }
}
