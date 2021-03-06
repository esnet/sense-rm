package net.es.sense.rm.api;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author hacksaw
 */
@Configuration
@ComponentScan({
  "net.es.sense.rm.api",
  "net.es.sense.rm.measurements"
})
@EnableAutoConfiguration
public class ApiTestConfiguration {

}
