package net.es.nsi.dds.lib.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import net.es.nsi.dds.lib.constants.Properties;
import net.es.nsi.dds.lib.dao.KeyStoreType;
import net.es.nsi.dds.lib.dao.SecureType;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.glassfish.jersey.SslConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton to manage the HTTPS security context.
 *
 * @author hacksaw
 */
public enum HttpsContext {
  INSTANCE;

  private final Logger log = LoggerFactory.getLogger(RestClient.class);
  private SSLContext sslContext;
  private boolean isProduction;

  /**
   * Construct an HttpConfig object.
   */
  HttpsContext() {
    log.debug("[HttpsContext]: constructor invoked");

    // If the BouncyCastle provider is not register add it in.
    if (Security.getProvider("BC") == null) {
      log.debug("Adding BouncyCastleProvider provider");
      Security.addProvider(new BouncyCastleProvider());
    }

    // If the BouncyCastle JSSE provider is not register add it in.
    if (Security.getProvider("BCJSSE") == null) {
      log.debug("Adding BouncyCastleJsseProvider provider");
      Security.addProvider(new BouncyCastleJsseProvider());
    }

    log.debug("[HttpsContext]: constructor complete");
  }

  /**
   * Get the single instance of the HttpsContext object.
   *
   * @return The current HttpsContext object.
   */
  public static HttpsContext getInstance() {
    return INSTANCE;
  }

  /**
   * Convert security configuration from JAXB configuration file type to something usable by system.
   *
   * @param config The JAXB SecureType object containing the TLS configuration information.
   *
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchProviderException
   * @throws KeyStoreException
   * @throws IOException
   * @throws CertificateException
   * @throws UnrecoverableKeyException
   */
  public synchronized void load(SecureType config) throws KeyManagementException, NoSuchAlgorithmException,
          NoSuchProviderException, KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {

    log.debug("[HttpsContext].load invoked");

    if (config == null) {
      throw new IllegalArgumentException("[HttpsContext].load: server configuration not provided");
    }

    // We will use the application basedir to fully qualify any relative paths.
    String basedir = System.getProperty(Properties.SYSTEM_PROPERTY_BASEDIR);

    // Determine the keystore configuration.
    KeyStoreType keyStore = config.getKeyStore();
    if (keyStore == null) {
      // Check to see if the keystore was provided on the commandline.
      keyStore = new KeyStoreType();
      keyStore.setFile(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE, Properties.DEFAULT_SSL_KEYSTORE));
      keyStore.setPassword(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD, Properties.DEFAULT_SSL_KEYSTORE_PASSWORD));
      keyStore.setType(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE, Properties.DEFAULT_SSL_KEYSTORE_TYPE));
    }

    keyStore.setFile(getAbsolutePath(basedir, keyStore.getFile()));

    KeyStoreType trustStore = config.getTrustStore();
    if (trustStore == null) {
      // Check to see if the truststore was provided on the commandline.
      trustStore = new KeyStoreType();
      trustStore.setFile(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE, Properties.DEFAULT_SSL_TRUSTSTORE));
      trustStore.setPassword(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_PASSWORD, Properties.DEFAULT_SSL_TRUSTSTORE_PASSWORD));
      trustStore.setType(System.getProperty(Properties.SYSTEM_PROPERTY_SSL_TRUSTSTORE_TYPE, Properties.DEFAULT_SSL_TRUSTSTORE_TYPE));
    }

    trustStore.setFile(getAbsolutePath(basedir, trustStore.getFile()));

    // Initialize the SSL context.
    sslContext = initContext(config);

    // Production boolean controls the hostverifier used during SSL setup.
    isProduction = config.isProduction();

    log.debug("[HttpsContext].load: done.");
  }

  /**
   * Get the absolute path for inPath.
   *
   * @param basedir
   * @param inPath
   * @return
   * @throws IOException
   */
  private String getAbsolutePath(String basedir, String inPath) throws IOException {
    Path outPath = Paths.get(inPath);
    if (!outPath.isAbsolute()) {
      outPath = Paths.get(basedir, inPath);
    }

    return outPath.toRealPath().toString();
  }

  /**
   * Get the default SSL context and add our specific configuration.
   *
   * @return New SSLContext for HTTP client.
   *
   * @throws java.security.KeyManagementException
   * @throws java.security.NoSuchAlgorithmException
   * @throws java.security.KeyStoreException
   * @throws java.io.IOException
   * @throws java.security.cert.CertificateException
   * @throws java.security.UnrecoverableKeyException
   */
  private SSLContext initContext(SecureType st) throws KeyManagementException, NoSuchAlgorithmException,
          KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {

    // Log what security providers are available to us.
    for (Provider provider : Security.getProviders()) {
      log.debug("[HttpsContext].initContext: Provider - {}, {}", provider.getName(), provider.getInfo());
    }

    try {
      // Configure the SSL context.
      SSLContext ctx = SSLContext.getInstance("TLS");

      // Initialize the KeyManagerFactory with provided configuration.
      KeyManagerFactory keyMgrFact = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyMgrFact.init(getKeyStore(st.getKeyStore()), st.getKeyStore().getPassword().toCharArray());

      // Initialize the TrustManagerFactory with provided configuration.
      TrustManagerFactory trustMgrFact = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustMgrFact.init(getKeyStore(st.getTrustStore()));

      // Initialize the SSL context with key managers.
      ctx.init(keyMgrFact.getKeyManagers(), trustMgrFact.getTrustManagers(), new SecureRandom());

      // Set this newly configured provider as the default provider.
      SSLContext.setDefault(ctx);

      // For giggles lets dump the default context.
      dumpSSLContext("[HttpsContext].initContext: defaultContext", SslConfigurator.getDefaultContext());
      return ctx;
    } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException
            | IOException | CertificateException | UnrecoverableKeyException ex) {
      log.error("[HttpsContext].initContext: could not find SSL provider", ex);
      throw ex;
    }
  }

  /**
   * Get an instance of a keystore based on requested type and initializes with the provide keystore file.
   *
   * @param ks
   * @return
   * @throws KeyStoreException
   * @throws IOException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   */
  private KeyStore getKeyStore(KeyStoreType ks) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    // Open specified keystore.
    File file = new File(ks.getFile());
    if (!file.exists()) {
      log.error("[HttpsContext].getKeyStore: file {} does not exist.", ks.getFile());
      throw new FileNotFoundException(String.format("[HttpsContext].getKeyStore: file %s does not exist", ks.getFile()));
    }
    KeyStore keyStore;
    try (InputStream stream = Files.newInputStream(file.toPath())) {
      keyStore = KeyStore.getInstance(ks.getType());
      keyStore.load(stream, ks.getPassword().toCharArray());
    }
    return keyStore;
  }

  /**
   * Debug class that will dump information relating to the provided SSLContext.
   *
   * @param prefix - prefix pre-pended to the debug string.
   * @param c - SSLContext to dump to debug.
   */
  private void dumpSSLContext(String prefix, SSLContext c) {
    log.debug("{} - Provider = {}, {}", prefix, c.getProvider().getName(), c.getProvider().getInfo());

    SSLParameters supportedSSLParameters = c.getSupportedSSLParameters();
    String[] cipherSuites = supportedSSLParameters.getCipherSuites();
    for (String cipher : cipherSuites) {
      log.debug("{} - default cipher = {}", prefix, cipher);
    }

    for (String proto : supportedSSLParameters.getApplicationProtocols()) {
      log.debug("{} - application protocol = {}", prefix, proto);
    }
  }


  /**
   * Get the SSL context.
   *
   * @return New SSLContext for HTTP client.
   */
  public SSLContext getSSLContext() {
    return sslContext;
  }

  /**
   * Is this server configured for production?
   *
   * @return true if configured for production.
   */
  public boolean isProduction() {
    return isProduction;
  }
}
