package net.es.sense.rm.driver.nsi.mrml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.es.nsi.dds.lib.jaxb.nml.NmlSwitchingServiceType;

/**
 *
 * @author hacksaw
 */
@lombok.Data
public class NmlSwitchingSubnet {
  private String id;
  private String topologyId;
  private String serviceId;
  private long discovered;

  private List<NmlPort> ports = new ArrayList<>();

  private Optional<Long> startTime = Optional.empty();

  private Optional<Long> endTime = Optional.empty();

  private Optional<String> tag = Optional.empty();

  private NmlSwitchingServiceType switchingService;

  public static String id(String topology, String localId) {
    return topology + ":switchingSubnet+" + localId;
  }
}
