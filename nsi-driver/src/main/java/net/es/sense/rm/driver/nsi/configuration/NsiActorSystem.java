package net.es.sense.rm.driver.nsi.configuration;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.spring.SpringApplicationContext;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class NsiActorSystem {

  // The application context is needed to initialize the Akka Spring Extension.
  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiProperties nsiProperties;

  private ActorSystem actorSystem;

  @PostConstruct
  public void init() throws UnsupportedOperationException {
    if (springExtension == null) {
      throw new UnsupportedOperationException("springExtension has not been initialized.");
    }

    log.info("[NsiActorSystem] Initializing DDS actor framework for uRA = {}", nsiProperties.getNsaId());

    // Initialize the AKKA actor system.
    actorSystem = ActorSystem.create("SENSE-NSI-System", ConfigFactory.load());

    // Initialize the application context in the Akka Spring Extension
    springExtension.initialize(applicationContext);

    log.info("[NsiActorSystem] ... actor framework initialized for uRA = {}", nsiProperties.getNsaId());
  }

  public static NsiActorSystem getInstance() {
    NsiActorSystem dds = SpringApplicationContext.getBean("nsiActorSystem", NsiActorSystem.class);
    return dds;
  }

  public ActorSystem getActorSystem() throws UnsupportedOperationException {
    if (actorSystem == null) {
      throw new UnsupportedOperationException("actorSystem has not been initialized.");
    }
    return actorSystem;
  }
}
