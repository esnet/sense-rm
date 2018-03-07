package net.es.sense.rm.driver.api;

import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.es.sense.rm.model.DeltaResource;

/**
 *
 * @author hacksaw
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class DeltaResponse extends ResourceResponse {
  Optional<DeltaResource> delta = Optional.empty();
}
