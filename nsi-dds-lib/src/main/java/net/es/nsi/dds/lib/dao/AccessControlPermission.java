package net.es.nsi.dds.lib.dao;

public enum AccessControlPermission {

  READ("read"),
  WRITE("write"),
  ADMIN("admin");
  private final String value;

  AccessControlPermission(String v) {
    value = v;
  }

  public String value() {
    return value;
  }

  public static AccessControlPermission fromValue(String v) {
    for (AccessControlPermission c : AccessControlPermission.values()) {
      if (c.value.equals(v)) {
        return c;
      }
    }
    throw new IllegalArgumentException(v);
  }

}
