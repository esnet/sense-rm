package net.es.sense.rm.driver.api;

import jakarta.ws.rs.core.Response.Status;
import lombok.Data;

import java.util.Optional;

/**
 *
 * @author hacksaw
 */
@Data
public class ResourceResponse {
  Status status = Status.OK;
  Optional<String> error = Optional.empty();

  public static Status exceptionToStatus(Exception ex) {
    if (ex instanceof IllegalArgumentException) {
      return Status.BAD_REQUEST;
    } else {
      return Status.INTERNAL_SERVER_ERROR;
    }
  }
}
