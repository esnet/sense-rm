package net.es.nsi.dds.lib.client;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.net.ssl.SSLContext;
import net.es.nsi.dds.lib.constants.Properties;
import net.es.nsi.dds.lib.dao.KeyStoreType;
import net.es.nsi.dds.lib.dao.SecureType;
import org.glassfish.jersey.SslConfigurator;

public class HttpsConfig {
  private String basedir;
  private SecureType config;

  public HttpsConfig(SecureType config) throws IOException {
    if (config == null) {
      throw new IllegalArgumentException("HttpConfig: server configuration not provided");
    }

    // We will use the application basedir to fully qualify any relative paths.
    basedir = System.getProperty(Properties.SYSTEM_PROPERTY_BASEDIR);

    // Determine the keystore configuration.
    KeyStoreType keyStore = config.getKeyStore();
    if (keyStore == null) {
      // Check to see if the keystore was provided on the commandline.
      keyStore = KeyStoreType.builder()
              .file(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE, Properties.DEFAULT_SSL_KEYSTORE))
              .password(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD, Properties.DEFAULT_SSL_KEYSTORE_PASSWORD))
              .type(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE, Properties.DEFAULT_SSL_KEYSTORE_TYPE))
              .build();
    }

    keyStore.setFile(getAbsolutePath(keyStore.getFile()));

    KeyStoreType trustStore = config.getTrustStore();
    if (trustStore == null) {
      trustStore = KeyStoreType.builder()
              .file(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE, Properties.DEFAULT_SSL_TRUSTSTORE))
              .password(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_PASSWORD, Properties.DEFAULT_SSL_TRUSTSTORE_PASSWORD))
              .type(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_TYPE, Properties.DEFAULT_SSL_TRUSTSTORE_TYPE))
              .build();
    }

    trustStore.setFile(getAbsolutePath(trustStore.getFile()));

    this.config = config;
  }

  private String getAbsolutePath(String inPath) throws IOException {
    Path outPath = Paths.get(inPath);
    if (!outPath.isAbsolute()) {
      outPath = Paths.get(basedir, inPath);
    }

    return outPath.toRealPath().toString();
  }

  public SSLContext getSSLContext() {
    SslConfigurator sslConfig = SslConfigurator.newInstance()
            .trustStoreFile(config.getTrustStore().getFile())
            .trustStorePassword(config.getTrustStore().getPassword())
            .trustStoreType(config.getTrustStore().getType())
            .keyStoreFile(config.getKeyStore().getFile())
            .keyPassword(config.getKeyStore().getPassword())
            .keyStoreType(config.getKeyStore().getType())
            .securityProtocol("TLS");
    return sslConfig.createSSLContext();
  }

  public boolean isProduction() {
    return config.isProduction();
  }
}
