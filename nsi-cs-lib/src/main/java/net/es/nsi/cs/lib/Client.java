package net.es.nsi.cs.lib;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.Handler;
import org.ogf.schemas.nsi._2013._12.connection.provider.ConnectionProviderPort;
import org.ogf.schemas.nsi._2013._12.connection.provider.ConnectionServiceProvider;

import java.util.List;
import java.util.Map;

/**
 * A NSI-CS client for communications with an NSA.
 *
 * @author hacksaw
 */
public class Client {

  org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

  private final ConnectionProviderPort proxy;

  public Client(String providerUrl) {
    logger.info("Client: created client for " + providerUrl);
    ConnectionServiceProvider provider = new ConnectionServiceProvider();
    proxy = provider.getConnectionServiceProviderPort();
    BindingProvider bp = (BindingProvider) proxy;

    // Add the message logging handler.
    List<Handler> handlers = bp.getBinding().getHandlerChain();
    handlers.add(new SoapLogger());
    bp.getBinding().setHandlerChain(handlers);

    // Add the factories associated with our custom types.
    Map<String, Object> context = bp.getRequestContext();
    context.put("jaxb.additionalContextClasses",
            new Class[]{
              org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory.class,
              org.ogf.schemas.nsi._2013._12.services.types.ObjectFactory.class
            });
    context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, providerUrl);
    context.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

    logger.info("Client: created client for " + providerUrl);
  }

  public ConnectionProviderPort getProxy() {
    return proxy;
  }
}
