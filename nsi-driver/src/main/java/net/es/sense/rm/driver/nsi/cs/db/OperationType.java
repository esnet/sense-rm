package net.es.sense.rm.driver.nsi.cs.db;

/**
 *
 * @author hacksaw
 */
public enum OperationType {
  reserve(0), commit(1), provision(2), release(3), terminate(4);

  private final long value;

  OperationType(long value) {
    this.value = value;
  }

  public long value() {
    return value;
  }
}
