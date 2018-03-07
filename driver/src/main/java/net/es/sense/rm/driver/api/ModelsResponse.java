package net.es.sense.rm.driver.api;

import java.util.ArrayList;
import java.util.Collection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.es.sense.rm.model.ModelResource;

/**
 *
 * @author hacksaw
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class ModelsResponse extends ResourceResponse {
  Collection<ModelResource> models = new ArrayList<>();
}
