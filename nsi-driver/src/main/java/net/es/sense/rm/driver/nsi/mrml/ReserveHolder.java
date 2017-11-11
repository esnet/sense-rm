package net.es.sense.rm.driver.nsi.mrml;

import java.util.HashMap;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveType;

/**
 *
 * @author hacksaw
 */
@lombok.Data
public class ReserveHolder {
  private ReserveType reserve;
  private String switchingSubnetId;
  private HashMap<String, StpHolder> ports = new HashMap<>();

}
