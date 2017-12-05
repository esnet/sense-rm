package net.es.sense.rm.driver.nsi.cs.db;

/**
 * Models the NSI reservation states.
 * 
 * @author hacksaw
 */
public enum StateType {
  reserving(0), reserved(1), committing(2), committed(3), provisioning(4), provisioned(5),
  releasing(6), released(7), terminating(8), terminated(9), failed(10);

  private final long value;

  StateType(long value) {
    this.value = value;
  }

  public long value() {
    return value;
  }
}
