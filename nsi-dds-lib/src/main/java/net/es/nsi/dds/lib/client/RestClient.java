package net.es.nsi.dds.lib.client;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.dds.lib.dao.ClientType;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.RequestEntityProcessing;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Provides REST client for communication with remote DDS servers.
 *
 * @author hacksaw
 */
public class RestClient {

  private static final Logger log = LoggerFactory.getLogger(RestClient.class);
  private final Client client;

  // Time for idle data timeout.
  private static final String TCP_SO_TIMEOUT = "tcpSoTimeout";
  private static final int SO_TIMEOUT = 60 * 1000;

  // Time for the socket to connect.
  private static final String TCP_CONNECT_TIMEOUT = "tcpConnectTimeout";
  private static final int CONNECT_TIMEOUT = 20 * 1000;

  // Time to block for a socket from the connection manager.
  private static final String TCP_CONNECT_REQUEST_TIMEOUT = "tcpConnectRequestTimeout";
  private static final int CONNECT_REQUEST_TIMEOUT = 30 * 1000;

  // Connection provider pool configuration defaults.
  private final static int MAX_CONNECTION_PER_ROUTE = 10;
  private final static int MAX_CONNECTION_TOTAL = 80;

  public RestClient() {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    ClientConfig clientConfig = getClientConfig(connectionManager, MAX_CONNECTION_PER_ROUTE, MAX_CONNECTION_TOTAL);
    client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
    client.property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, Level.FINEST.getName());
    client.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY);
  }

  public RestClient(ClientType ct) {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    ClientConfig clientConfig = getClientConfig(connectionManager, ct.getMaxConnPerRoute(), ct.getMaxConnTotal());
    client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
    client.property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, Level.FINEST.getName());
    client.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY);
  }

  public RestClient(ClientType ct, HttpsContext hc) throws KeyStoreException, IOException,
          NoSuchAlgorithmException, CertificateException, KeyManagementException,
          UnrecoverableKeyException, IllegalArgumentException {

    // Check to see if the client should be configured with a secure context.
    if (!ct.isSecure()) {
      // Looks like they meant to invoke the insecure version of this constructor.
      throw new IllegalArgumentException("Client context must be set to secure, but is set to false.");
    }

    HostnameVerifier hostnameVerifier;
    if (hc.isProduction()) {
      hostnameVerifier = new DefaultHostnameVerifier();
    } else {
      hostnameVerifier = new NoopHostnameVerifier();
    }

    PlainConnectionSocketFactory socketFactory = PlainConnectionSocketFactory.getSocketFactory();
    RegistryBuilder<ConnectionSocketFactory> rb = RegistryBuilder.create();
    if (socketFactory != null) {
      rb.register("http", socketFactory);
    }
    if (hc.getSSLContext() != null) {
      rb.register("https", new SSLConnectionSocketFactory(hc.getSSLContext(), hostnameVerifier));
    }
    final Registry<ConnectionSocketFactory> registry = rb.build();

    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
    ClientConfig clientConfig = getClientConfig(connectionManager, ct.getMaxConnPerRoute(), ct.getMaxConnTotal());
    client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
    client.property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, Level.FINEST.getName());
    client.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY);
  }

  /**
   * Creates a client configuration based on the provided configuration.
   *
   * @param connectionManager Connection manager used to configure the client configuration.
   * @param maxPerRoute The max connections per destination.
   * @param maxTotal The max connection total across all destinations.
   * @return The new client configuration.
   */
  private ClientConfig getClientConfig(PoolingHttpClientConnectionManager connectionManager,
          int maxPerRoute, int maxTotal) {
    ClientConfig clientConfig = new ClientConfig();

    // We want to use the Apache connector for chunk POST support.
    clientConfig.connectorProvider(new ApacheConnectorProvider());
    connectionManager.setDefaultMaxPerRoute(maxPerRoute);
    connectionManager.setMaxTotal(maxTotal);
    connectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
    clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

    clientConfig.register(GZipEncoder.class);
    clientConfig.register(new MoxyXmlFeature());
    clientConfig.register(new LoggingFeature(java.util.logging.Logger.getGlobal(), Level.ALL,
            LoggingFeature.Verbosity.PAYLOAD_ANY, 10000));
    clientConfig.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);

    // Apache specific configuration.
    RequestConfig.Builder custom = RequestConfig.custom();
    custom.setExpectContinueEnabled(true);
    custom.setRelativeRedirectsAllowed(true);
    custom.setRedirectsEnabled(true);
    custom.setSocketTimeout(Integer.parseInt(System.getProperty(TCP_SO_TIMEOUT, Integer.toString(SO_TIMEOUT))));
    custom.setConnectTimeout(Integer.parseInt(System.getProperty(TCP_CONNECT_TIMEOUT, Integer.toString(CONNECT_TIMEOUT))));
    custom.setConnectionRequestTimeout(Integer.parseInt(System.getProperty(TCP_CONNECT_REQUEST_TIMEOUT, Integer.toString(CONNECT_REQUEST_TIMEOUT))));
    clientConfig.property(ApacheClientProperties.REQUEST_CONFIG, custom.build());

    return clientConfig;
  }

  public Client get() {
    return client;
  }

  public void close() {
    client.close();
  }

  private static class FollowRedirectFilter implements ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
      if (requestContext == null || responseContext == null ||
          responseContext.getStatus() != Response.Status.FOUND.getStatusCode()) {
        return;
      }

      log.debug("Processing redirect for {} {} to {}", requestContext.getMethod(),
          requestContext.getUri().toASCIIString(), responseContext.getLocation().toASCIIString());

      Client inClient = requestContext.getClient();
      Object entity = requestContext.getEntity();
      MultivaluedMap<String, Object> headers = requestContext.getHeaders();
      String method = requestContext.getMethod();
      Response resp;
      if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
        resp = inClient.target(responseContext.getLocation())
                .request(requestContext.getMediaType())
                .headers(headers)
                .method(requestContext.getMethod(), Entity.entity(new GenericEntity<JAXBElement<?>>((JAXBElement<?>) entity) {
                }, Nsi.NSI_DDS_V1_XML));
      } else {
        resp = inClient.target(responseContext.getLocation())
                .request(requestContext.getMediaType())
                .headers(headers)
                .method(requestContext.getMethod());
      }

      responseContext.setEntityStream((InputStream) resp.getEntity());
      responseContext.setStatusInfo(resp.getStatusInfo());
      responseContext.setStatus(resp.getStatus());
      responseContext.getHeaders().putAll(resp.getStringHeaders());
      resp.close();

      log.debug("Processing redirect with result {}", resp.getStatus());
    }
  }
}
