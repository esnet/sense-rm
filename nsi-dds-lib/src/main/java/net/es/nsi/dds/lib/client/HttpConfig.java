package net.es.nsi.dds.lib.client;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.es.nsi.dds.lib.constants.Properties;
import net.es.nsi.dds.lib.dao.SecureType;
import net.es.nsi.dds.lib.dao.ServerType;
import net.es.nsi.dds.lib.dao.StaticType;

public class HttpConfig {

  private static final String DEFAULT_ADDRESS = "localhost";
  private static final String DEFAULT_PORT = "8401";
  private static final String DEFAULT_PACKAGENAME = "net.es.nsi.dds.lib";

  private Optional<String> address;
  private Optional<String> port;
  private Optional<String> packageName;
  private Optional<String> staticPath = Optional.absent();
  private Optional<String> relativePath = Optional.absent();
  private boolean secure = false;
  private Optional<SecureType> secureConfig = Optional.absent();
  private Optional<HttpsConfig> httpsConfig = Optional.absent();
  private String basedir;

  public HttpConfig(String address, String port, String packageName) {
    this.address = Optional.fromNullable(Strings.emptyToNull(address));
    this.port = Optional.fromNullable(Strings.emptyToNull(port));
    this.packageName = Optional.fromNullable(Strings.emptyToNull(packageName));
  }

  public HttpConfig(ServerType config) throws IOException {
    if (config == null) {
      throw new IllegalArgumentException("HttpConfig: server configuration not provided");
    }

    // We will use the application basedir to fully qualify any relative paths.
    basedir = System.getProperty(Properties.SYSTEM_PROPERTY_BASEDIR);

    // These two parameters must be present.
    address = Optional.fromNullable(Strings.emptyToNull(config.getAddress()));
    port = Optional.fromNullable(Strings.emptyToNull(config.getPort()));
    packageName = Optional.fromNullable(Strings.emptyToNull(config.getPackageName()));

        // staticPath is optional, and only provided if an HTTP server should
    // also be started for static content.
    Optional<StaticType> aStatic = Optional.fromNullable(config.getPath());
    if (aStatic.isPresent()) {
      staticPath = Optional.fromNullable(Strings.emptyToNull(aStatic.get().getPath()));
      relativePath = Optional.fromNullable(Strings.emptyToNull(aStatic.get().getRelative()));
      if (staticPath.isPresent()) {
        staticPath = Optional.fromNullable(getAbsolutePath(staticPath.get()));
      }
    }

        // Server secure settings are optional, and only provided if standalone
    // HTTPS capabilities are needed.
    secureConfig = Optional.fromNullable(config.getSecure());
    if (secureConfig.isPresent()) {
      secure = true;
      httpsConfig = Optional.of(new HttpsConfig(secureConfig.get()));
    }

  }

  private String getAbsolutePath(String inPath) throws IOException {
    Path outPath = Paths.get(inPath);
    if (!outPath.isAbsolute()) {
      outPath = Paths.get(basedir, inPath);
    }

    return outPath.toRealPath().toString();
  }

  public URI getURI() {
    if (secure) {
      return URI.create("https://" + address.or(DEFAULT_ADDRESS) + ":" + port.or(DEFAULT_PORT));
    }

    return URI.create("http://" + address.or(DEFAULT_ADDRESS) + ":" + port.or(DEFAULT_PORT));
  }

  public URL getURL() throws MalformedURLException {
    return getURI().toURL();
  }

  public String getAddress() {
    return address.or(DEFAULT_ADDRESS);
  }

  public String getPort() {
    return port.or(DEFAULT_PORT);
  }

  /**
   * @return the url
   */
  public String getUrl() {
    return getURI().toString();
  }

  /**
   * @return the packageName
   */
  public String getPackageName() {
    return packageName.or(DEFAULT_PACKAGENAME);
  }

  /**
   * @return the staticPath
   */
  public String getStaticPath() {
    return staticPath.orNull();
  }

  /**
   * @return the wwwPath
   */
  public String getRelativePath() {
    return relativePath.orNull();
  }

  public boolean isSecure() {
    return secure;
  }

  public SecureType getSecureConfig() {
    return secureConfig.orNull();
  }

  public HttpsConfig getHttpsConfig() {
    return httpsConfig.orNull();
  }
}
