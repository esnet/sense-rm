package net.es.sense.rm.driver.nsi.cs.db;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Data
public class StpMapping implements Serializable {
  String stpId;
  String mrsPortId;
  String mrsLabelId;
  String mrsBandwidthId;
}
