package net.es.sense.rm.driver.nsi.mrml;

import com.twitter.common.base.Either;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.es.nsi.cs.lib.SimpleLabel;
import net.es.nsi.cs.lib.SimpleStp;
import net.es.nsi.dds.lib.jaxb.nml.NmlLabelGroupType;
import net.es.nsi.dds.lib.jaxb.nml.NmlPortGroupType;
import net.es.nsi.dds.lib.jaxb.nml.NmlPortType;
import net.es.nsi.dds.lib.jaxb.nml.NmlSwitchingServiceType;
import net.es.nsi.dds.lib.jaxb.nml.ObjectFactory;

/**
 *
 * @author hacksaw
 */
@lombok.Data
@lombok.Builder
public class NmlPort {

  private final static ObjectFactory FACTORY = new ObjectFactory();

  private String id;
  private String topologyId;
  private Optional<String> name;
  private Orientation orientation;

  @lombok.Builder.Default
  private Optional<String> tag = Optional.empty();

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

  private Either<NmlPortGroupType, NmlPortType> port;

  @lombok.Builder.Default
  private List<String> children = new ArrayList<>();

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

  @lombok.Builder.Default
  private Optional<Long> startTime = Optional.empty();

  @lombok.Builder.Default
  private Optional<Long> endTime = Optional.empty();

  @lombok.Builder.Default
  private List<NmlSwitchingServiceType> switchingServices = new ArrayList<>();

  public Optional<String> getServiceId() {
    if (tag.isPresent()) {
      return Optional.of("serviceId=" + tag.get());
    }
    else {
      return Optional.empty();
    }
  }

  public Set<NmlLabelGroupType> setNmlLabels(SimpleStp stp) {
    Set<SimpleLabel> simpleLabels = stp.getLabels();
    simpleLabels.stream().forEach(label -> {
      NmlLabelGroupType lgt = FACTORY.createNmlLabelGroupType();
      lgt.setLabeltype(label.getType());
      lgt.setValue(label.getValue());
      labels.add(lgt);
    });

    return labels;
  }
}
