package net.es.sense.rm.driver.nsi.mrml;

/**
 *
 * @author hacksaw
 */
public enum MrsBandwidthType {
  undefined(0), guaranteedCapped(1), guaranteed(2), bestEffort(3);

  private final int value ;

  MrsBandwidthType(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }
}
