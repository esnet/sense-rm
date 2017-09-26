package net.es.sense.rm.driver.nsi.mrml;

import com.twitter.common.base.Either;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.lib.jaxb.nml.NmlLabelGroupType;
import net.es.nsi.dds.lib.jaxb.nml.NmlPortGroupType;
import net.es.nsi.dds.lib.jaxb.nml.NmlPortType;

/**
 *
 * @author hacksaw
 */
@Slf4j
@lombok.Data
@lombok.Builder
public class NmlPort {
  private String id;
  private String topologyId;
  private Optional<String> name;
  private Orientation orientation;
  @lombok.Builder.Default
  private Optional<String> encoding = Optional.empty();
  //@lombok.Builder.Default
  //private Optional<String> labelType = Optional.empty();
  @lombok.Builder.Default
  private Set<NmlLabelGroupType> labels = new LinkedHashSet<>();

  // References to different ports.
  @lombok.Builder.Default
  private Optional<String> isAlias = Optional.empty();
  @lombok.Builder.Default
  private Optional<String> inboundPort = Optional.empty();
  @lombok.Builder.Default
  private Optional<String> outboundPort = Optional.empty();
  @lombok.Builder.Default
  private Optional<String> parentPort = Optional.empty();
  private Either <NmlPortGroupType, NmlPortType> port;

  // Ethernet port metrics.
  @lombok.Builder.Default
  private Optional<Long> granularity = Optional.empty();
  @lombok.Builder.Default
  private Optional<Long> maximumReservableCapacity = Optional.empty();
  @lombok.Builder.Default
  private Optional<Long> minimumReservableCapacity = Optional.empty();
  @lombok.Builder.Default
  private Optional<Long> individualCapacity = Optional.empty();
  @lombok.Builder.Default
  private Optional<Integer> interfaceMTU = Optional.empty();
  @lombok.Builder.Default
  private Optional<Long> capacity = Optional.empty();
}
