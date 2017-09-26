package net.es.nsi.dds.lib.dao;

/**
 *
 * @author hacksaw
 */
@lombok.Builder
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ServerType {
  private SecureType secure;
  private StaticType path;
  private String address;
  private String port;
  private String packageName;
}
