package net.es.nsi.dds.lib.dao;

/**
 *
 * @author hacksaw
 */
@lombok.Builder
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class SecureType {
  @lombok.Builder.Default
  private boolean production = true;
  private KeyStoreType keyStore;
  private KeyStoreType trustStore;
}
