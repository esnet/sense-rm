package net.es.sense.rm.driver.nsi.dds.db;

import akka.actor.Cancellable;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import javax.ws.rs.WebApplicationException;
import javax.xml.datatype.DatatypeConfigurationException;
import net.es.nsi.dds.lib.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.lib.jaxb.dds.SubscriptionRequestType;
import net.es.nsi.dds.lib.jaxb.dds.SubscriptionType;
import net.es.nsi.dds.lib.util.XmlUtilities;
import net.es.sense.rm.driver.nsi.dds.api.DiscoveryError;
import net.es.sense.rm.driver.nsi.dds.api.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class LocalSubscription implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String SUBSCRIPTIONS_URL = "subscriptions";
    private static final ObjectFactory FACTORY = new ObjectFactory();
    private String id;
    private String encoding;
    private SubscriptionType subscription;
    private Date lastModified;
    private Cancellable action;

    public LocalSubscription(SubscriptionRequestType request, String encoding, String baseURL) throws WebApplicationException {
        // The unique subcription id is defined on the server side.
        id = UUID.randomUUID().toString();

        // This is the encoding in which the client would like notifications sent.
        this.encoding = encoding;

        // We take the subscription request parameters passed and complete the
        // subscription resource.
        subscription = FACTORY.createSubscriptionType();
        subscription.setId(id);

        // This is the direct URL to the subscription resource.
        subscription.setHref(getSubscriptionURL(baseURL));

        // We manage the version of subscription resources.
        try {
            subscription.setVersion(XmlUtilities.xmlGregorianCalendar());
        } catch (DatatypeConfigurationException ex) {
            // Log and eat the error.
            log.error("addSubscription: failed to set version", ex);
        }

        // Validate the callback parameter was provided.
        if (request.getCallback() == null || request.getCallback().isEmpty()) {
            throw Exceptions.missingParameterException("subscription", "callback");
        }

        // Make sure it can be parsed into a URL.
        try {
            URL url = new URL(request.getCallback());
        }
        catch (MalformedURLException ex) {
            throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, "subscription", "callback");
        }

        subscription.setCallback(request.getCallback());

        // Will need to revisit what is valid later.
        subscription.setFilter(request.getFilter());

        if (request.getRequesterId() == null || request.getRequesterId().isEmpty()) {
            throw Exceptions.missingParameterException("subscription", "requesterId");
        }
        subscription.setRequesterId(request.getRequesterId());

        subscription.getAny().addAll(request.getAny());
        subscription.getOtherAttributes().putAll(request.getOtherAttributes());

        lastModified = new Date();
        lastModified.setTime(lastModified.getTime() - lastModified.getTime() % 1000);
    }

    private String getSubscriptionURL(String baseURL) throws WebApplicationException {
        URL url;
        try {
            if (!baseURL.endsWith("/")) {
                baseURL = baseURL + "/";
            }
            url = new URL(baseURL);
            url = new URL(url, SUBSCRIPTIONS_URL + "/" + this.id);
        } catch (MalformedURLException ex) {
            throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, "subscription", "href");
        }

        return url.toString();
    }

    /**
     * @return the subscription
     */
    public SubscriptionType getSubscription() {
        return subscription;
    }

    /**
     * @param subscription the subscription to set
     */
    public void setSubscription(SubscriptionType subscription) {
        this.subscription = subscription;
    }

    /**
     * @return the encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the lastModified
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * @return the event
     */
    public Cancellable getAction() {
        return action;
    }

    /**
     * @param event the event to set
     */
    public void setAction(Cancellable event) {
        this.action = event;
    }

    public void update(SubscriptionRequestType request, String encoding) throws WebApplicationException {
        if (request == null) {
            throw Exceptions.missingParameterException("subscription", "subscriptionRequest");
        }

        if (encoding == null) {
            String error = DiscoveryError.getErrorString(DiscoveryError.MISSING_PARAMETER, "PUT", "encoding");
            throw Exceptions.missingParameterException("PUT", "encoding");
        }

        this.encoding = encoding;
        lastModified.setTime(System.currentTimeMillis());
        subscription.setRequesterId(request.getRequesterId());
        subscription.setFilter(request.getFilter());
        subscription.setCallback(request.getCallback());
        subscription.getAny().addAll(request.getAny());
        subscription.getOtherAttributes().putAll(request.getOtherAttributes());
        try {
            subscription.setVersion(XmlUtilities.xmlGregorianCalendar());
        } catch (DatatypeConfigurationException ex) {
            // Log and eat the error.
            log.error("addSubscription: failed to set version", ex);
        }
    }
}
