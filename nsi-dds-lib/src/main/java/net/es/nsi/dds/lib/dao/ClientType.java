package net.es.nsi.dds.lib.dao;

/**
 *
 * @author hacksaw
 */
@lombok.Builder
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ClientType {
  @lombok.Builder.Default
  private int maxConnPerRoute = 10;

  @lombok.Builder.Default
  private int maxConnTotal = 80;

  @lombok.Builder.Default
  private boolean secure = false;
}
