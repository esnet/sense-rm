package net.es.sense.rm.driver.nsi.cs.db;

/**
 *
 * @author hacksaw
 */
public enum OperationType {
  reserve(0), reserveCommit(1), reserveAbort(2), provision(3), release(4), terminate(5);

  private final long value;

  OperationType(long value) {
    this.value = value;
  }

  public long value() {
    return value;
  }
}
