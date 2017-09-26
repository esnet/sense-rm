package net.es.sense.rm.driver.nsi.configuration;

import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import akka.actor.ActorSystem;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.spring.SpringApplicationContext;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Slf4j
@lombok.Data
@Configuration
@Component
public class ApplicationConfiguration {

  // The application context is needed to initialize the Akka Spring Extension.
  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiProperties nsiProperties;

  public static ApplicationConfiguration getInstance() {
    ApplicationConfiguration config = SpringApplicationContext.getBean("applicationConfiguration", ApplicationConfiguration.class);
    return config;
  }

  public ActorSystem getActorSystem() {
    return applicationContext.getBean("actorSystem", ActorSystem.class);
  }

  public NsiProperties getNsiProperties() {
    return nsiProperties;
  }
}
