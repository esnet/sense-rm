package net.es.sense.rm.driver.api;

import java.util.ArrayList;
import java.util.Collection;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.es.sense.rm.model.DeltaResource;

/**
 *
 * @author hacksaw
 */
@Data
@EqualsAndHashCode(callSuper=true)
public class DeltasResponse extends ResourceResponse {
  Collection<DeltaResource> deltas = new ArrayList<>();
}
