package net.es.sense.rm.driver.nsi.mrml;

import net.es.sense.rm.driver.schema.Mrs;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 *
 * @author hacksaw
 */
public class MrsBandwidthService {

  private final Resource service;

  public MrsBandwidthService(Resource service) {
    this.service = service;
  }

  public String getId() {
    return service.getURI();
  }

  public MrsUnits getUnit() {
    return MrsUnits.valueOf(service.getProperty(Mrs.unit).getString().toLowerCase());
  }

  public long getMaximumCapacity() {
    Statement maximumCapacity = service.getProperty(Mrs.maximumCapacity);
    return maximumCapacity.getLong();
  }

}
