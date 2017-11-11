package net.es.sense.rm.driver.nsi.mrml;

/**
 *
 * @author hacksaw
 */
public enum MrsUnits {
  bps(1), kpbs(1000), mbps(1000000), gbps(1000000000);

  private final long value;

  MrsUnits(long value) {
    this.value = value;
  }

  public long value() {
    return value;
  }

  public static long normalize(long val, MrsUnits fromUnit, MrsUnits toUnit)  {
    long bps = val * fromUnit.value();
    return bps / toUnit.value();
  }
}
