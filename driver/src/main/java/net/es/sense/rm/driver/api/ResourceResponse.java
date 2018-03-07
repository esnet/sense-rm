package net.es.sense.rm.driver.api;

import java.util.Optional;
import javax.ws.rs.core.Response.Status;
import lombok.Data;

/**
 *
 * @author hacksaw
 */
@Data
public class ResourceResponse {
  Status status = Status.OK;
  Optional<String> error = Optional.empty();
}
