package net.es.sense.rm.driver.api.mrml;

/**
 *
 * @author hacksaw
 */
public enum BandwidthType {
  undefined(0), guaranteedCapped(1), guaranteed(2), bestEffort(3);

  private final int value ;

  BandwidthType(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }
}
