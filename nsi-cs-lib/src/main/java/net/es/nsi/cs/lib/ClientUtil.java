package net.es.nsi.cs.lib;

import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.ogf.schemas.nsi._2013._12.connection.provider.ConnectionProviderPort;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility classes for building NSI-CS clients.
 *
 */
@Slf4j
public class ClientUtil {

  private final ConnectionProviderPort proxy;

  public ClientUtil(String url) {
    this.proxy = createProviderClient(url);
  }

  public ConnectionProviderPort getProxy() {
    return proxy;
  }

  /**
   * Creates a client class can be used to call provider at given URL
   *
   * @param url the URL of the provider to contact
   * @return the ConnectionProviderPort that you can use as the client
   */
  private ConnectionProviderPort createProviderClient(String url) {
    JaxWsProxyFactoryBean fb = new JaxWsProxyFactoryBean();
    fb.setAddress(url);
    fb.setProperties(setProps(fb.getProperties()));
    fb.setServiceClass(ConnectionProviderPort.class);
    return (ConnectionProviderPort) fb.create();
  }

  public static Map<String, Object> setProps(Map<String, Object> props) {
    if (props == null) {
      props = new HashMap<>();
    }
      props.put("jaxb.additionalContextClasses",
          new Class[]{
      org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory.class,
          org.ogf.schemas.nsi._2013._12.services.types.ObjectFactory.class
    });

    return props;
  }
}
