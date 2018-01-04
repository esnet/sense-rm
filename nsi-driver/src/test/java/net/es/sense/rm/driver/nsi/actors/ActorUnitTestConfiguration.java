package net.es.sense.rm.driver.nsi.actors;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author hacksaw
 */
@Configuration
@ComponentScan({
  "net.es.sense.rm.driver.nsi.actors",
  "net.es.sense.rm.driver.nsi.configuration",
  "net.es.sense.rm.driver.nsi.properties",
  "net.es.sense.rm.driver.nsi.spring"})
@EnableAutoConfiguration
public class ActorUnitTestConfiguration {

}
