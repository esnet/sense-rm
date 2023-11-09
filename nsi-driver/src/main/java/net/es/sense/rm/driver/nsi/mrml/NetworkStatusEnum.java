package net.es.sense.rm.driver.nsi.mrml;

/**
 * Models the possible enumerated values for the MRML NetworkStatus state.
 */
public enum NetworkStatusEnum {
  ACTIVATING("activating"),
  ACTIVATED("activated"),
  ACTIVATE_ERROR("activate-error"),
  DEACTIVATING("deactivating"),
  DEACTIVATED("deactivated"),
  DEACTIVATE_ERROR("deactivate-error"),
  UNKNOWN("unknown"), ERROR("error");

  private final String value;

  NetworkStatusEnum(String label) {
    this.value = label;
  }

  @Override
  public String toString() {
    return this.value;
  }
}