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
  private KeyStoreType keyStore;
  private KeyStoreType trustStore;
  private boolean production;

  public boolean isProduction() {
    return production;
  }
}
