package net.es.sense.rm.driver.nsi.mrml;

import net.es.nsi.cs.lib.SimpleStp;

/**
 *
 * @author hacksaw
 */
public class StpHolder {
  private final String mrsPortId;
  private final SimpleStp stp;
  private final MrsBandwidthService bw;

  public StpHolder(String mrsPortId, SimpleStp stp, MrsBandwidthService bw) {
    this.mrsPortId = mrsPortId;
    this.stp = stp;
    this.bw = bw;
  }

  public String getMrsPortId() {
    return mrsPortId;
  }


  public SimpleStp getStp() {
    return stp;
  }

  public MrsBandwidthService getBw() {
    return bw;
  }
}
