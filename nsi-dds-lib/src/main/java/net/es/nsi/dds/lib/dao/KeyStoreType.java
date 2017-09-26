package net.es.nsi.dds.lib.dao;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hacksaw
 */
@lombok.Builder
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class KeyStoreType {
  private String file;
  private String password;
  @lombok.Builder.Default
  private String type = "JKS";
  @lombok.Singular
  private final Map<String, Object> otherProperties = new HashMap<>();
}
