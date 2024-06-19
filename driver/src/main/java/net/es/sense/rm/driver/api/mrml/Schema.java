package net.es.sense.rm.driver.api.mrml;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.jena.riot.Lang;

@AllArgsConstructor
@Getter
public class Schema {
  private String uri;
  private String path;
  private Lang type;
}
