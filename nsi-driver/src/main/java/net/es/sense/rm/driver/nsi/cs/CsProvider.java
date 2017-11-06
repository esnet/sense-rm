package net.es.sense.rm.driver.nsi.cs;

import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class CsProvider {
    // Runtime properties.
  @Autowired
  private NsiProperties nsiProperties;

  // The actor system used to send notifications.
  @Autowired
  private CsController csController;

  public void init() {
    log.debug("[CsProvider] Initializing CS provider with database contents:");
  }

  public void start() {
    log.info("[CsProvider] starting...");
    csController.start();
    log.info("[CsProvider] start complete.");
  }
}
