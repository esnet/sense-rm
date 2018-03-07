package net.es.sense.rm.driver.api;

import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.es.sense.rm.model.ModelResource;

/**
 *
 * @author hacksaw
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class ModelResponse extends ResourceResponse {
  Optional<ModelResource> model = Optional.empty();
}
