package net.es.sense.rm.driver.nsi.mrml;

import java.util.ArrayList;
import java.util.List;
import net.es.nsi.dds.lib.jaxb.nml.NmlSwitchingServiceType;
import net.es.nsi.dds.lib.jaxb.nml.ServiceDefinitionType;

/**
 *
 * @author hacksaw
 */
@lombok.Builder
@lombok.Data
public class ServiceHolder {
  private String serviceType;
  private NmlSwitchingServiceType switchingService;
  private ServiceDefinitionType serviceDefinition;
  @lombok.Builder.Default
  private List<NmlSwitchingSubnet> switchingSubnets = new ArrayList<>();
}
