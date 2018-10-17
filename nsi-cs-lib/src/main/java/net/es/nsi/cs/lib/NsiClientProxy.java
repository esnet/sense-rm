package net.es.nsi.cs.lib;

import java.util.Map;
import javax.xml.ws.BindingProvider;
import org.ogf.schemas.nsi._2013._12.connection.provider.ConnectionProviderPort;
import org.ogf.schemas.nsi._2013._12.connection.provider.ConnectionServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NsiClientProxy {
    private final static Logger logger = LoggerFactory.getLogger(NsiClientProxy.class);

    public ConnectionProviderPort getProxy(String endpoint, String userID, String password) {

        ConnectionServiceProvider provider = new ConnectionServiceProvider();

        BindingProvider bp = (BindingProvider) provider.getConnectionServiceProviderPort();

        Map<String, Object> context = bp.getRequestContext();

        // Update the new endpoint if one was provided.
        if (endpoint != null && !endpoint.isEmpty()) {
            context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
            logger.info("New service endoint: " + endpoint);
        }

        // Set the HTTP basic authentication parameters if provided.
        if (userID != null && !userID.isEmpty()) {
            context.put(BindingProvider.USERNAME_PROPERTY, userID);
            context.put(BindingProvider.PASSWORD_PROPERTY, password);
            context.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
        }

        // Bypass transport layer certificate security for HTTPS.
        //logger.info("Bypass transport layer certificate security for HTTPS.");
        //context.put(JAXWSProperties.HOSTNAME_VERIFIER, new TestHostnameVerifier());
        //TestSecurityProvider.registerProvider();

        return provider.getConnectionServiceProviderPort();
    }
}
