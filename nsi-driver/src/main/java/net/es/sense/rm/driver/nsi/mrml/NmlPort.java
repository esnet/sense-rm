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

  // MRML id mapping information;
  @lombok.Builder.Default
  private Optional<String> mrsLabelId = Optional.empty();
  @lombok.Builder.Default
  private Optional<String> mrsBandwidthId = Optional.empty();

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

  /**
   * OSCARS mapping:
   *
   * type - guaranteedCapped
   *
   * maximumReservableCapacity - the maximum amount of bandwidth that can be reserved by circuits in bps.  This is
   * mapped to the MRML individualCapacity attribute.
   *
   * minimumReservableCapacity - the minimum amount of bandwidth that can be reserved by a circuit in bps.  This is
   * mapped to the MRML minimumCapacity attribute.
   *
   * capacity - total bandwidth of the port in bps.  This maps to the MRML maximumCapacity attribute.
   *
   * granularity - the increments that bandwidth can be reserved by a circuit in bps.  This maps to the
   * MRML granularity attribute.
   */

  @lombok.Builder.Default
  private MrsBandwidthType type = MrsBandwidthType.undefined;

  // the increments that bandwidth can be reserved by a circuit in bps.
  @lombok.Builder.Default
  private Optional<Long> granularity = Optional.empty(); //granularity

  // maximumCapacity - total port capacity (a guaranteed reservation could bust up to this capacity).
  @lombok.Builder.Default
  private Optional<Long> maximumCapacity = Optional.empty(); // capacity

  // minimumCapacity - what is the smallest capacity reservation that could be requested?
  @lombok.Builder.Default
  private Optional<Long> minimumCapacity = Optional.empty(); //minimumReservableCapacity

  // This is a temporal attribute identifying the amount of bandwidth consumed at a point in time.
  @lombok.Builder.Default
  private Optional<Long> usedCapacity = Optional.empty(); // Need multiple BandwidthService elements to model temporal values.

  // availableCapacity - the reservable capacity remaining on the port. (calculation based on point in time)
  // availableCapacity = reservableCapacity - usedCapacity; (for each time slot)
  @lombok.Builder.Default
  private Optional<Long> availableCapacity = Optional.empty(); // Need multiple BandwidthService elements to model temporal values.

  // reservableCapacity - max capacity allocated to this queue.
  @lombok.Builder.Default
  private Optional<Long> reservableCapacity = Optional.empty(); // Use capacity for now.

  // individualCapacity - per service maximum (a policy enforced maximum capacity per service request).
  @lombok.Builder.Default
  private Optional<Long> individualCapacity = Optional.empty(); // maximumReservableCapacity

  @lombok.Builder.Default
  private Optional<Integer> interfaceMTU = Optional.empty();

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
      if ("vlan".equalsIgnoreCase(label.getType())) {
        lgt.setLabeltype("http://schemas.ogf.org/nml/2012/10/ethernet#vlan");
      } else {
        lgt.setLabeltype(label.getType());
      }
      lgt.setValue(label.getValue());
      labels.add(lgt);
    });

    return labels;
  }
}
