package net.es.sense.rm.driver.nsi.dds.actors;

import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.lib.constants.Nsi;
import net.es.nsi.dds.lib.jaxb.DdsParser;
import net.es.nsi.dds.lib.jaxb.dds.DocumentEventType;
import net.es.nsi.dds.lib.jaxb.dds.ErrorType;
import net.es.nsi.dds.lib.jaxb.dds.FilterCriteriaType;
import net.es.nsi.dds.lib.jaxb.dds.FilterType;
import net.es.nsi.dds.lib.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.lib.jaxb.dds.SubscriptionListType;
import net.es.nsi.dds.lib.jaxb.dds.SubscriptionRequestType;
import net.es.nsi.dds.lib.jaxb.dds.SubscriptionType;
import net.es.nsi.dds.lib.util.UrlHelper;
import net.es.sense.rm.driver.nsi.dds.DdsClient;
import net.es.sense.rm.driver.nsi.dds.db.Subscription;
import net.es.sense.rm.driver.nsi.dds.db.SubscriptionService;
import net.es.sense.rm.driver.nsi.dds.messages.RegistrationEvent;
import net.es.sense.rm.driver.nsi.dds.messages.RegistrationEvent.Event;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The Registration Actor handles local DDS subscription registrations to peer DDS services.
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class RegistrationActor extends UntypedAbstractActor {

  private static final String NOTIFICATIONS_URL = "notifications";

  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private SubscriptionService subscriptionService;

  @Autowired
  private DdsClient ddsClient;

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), "RegistrationActor");
  private final ObjectFactory FACTORY = new ObjectFactory();

  @Override
  public void preStart() {
  }

  @Override
  public void onReceive(Object msg) {
    if (msg instanceof RegistrationEvent) {
      RegistrationEvent event = (RegistrationEvent) msg;
      log.debug("[RegistrationActor] event={}, url={}", event.getEvent().name(), event.getUrl());

      switch (event.getEvent()) {
        case Register:
          register(event);
          break;
        case Update:
          update(event);
          break;
        case Delete:
          delete(event);
          break;
        default:
          unhandled(msg);
          break;
      }
    } else {
      unhandled(msg);
    }
  }

  private String getNotificationURL() throws MalformedURLException {
    String baseURL = nsiProperties.getDdsUrl();
    URL url;
    if (!baseURL.endsWith("/")) {
      baseURL = baseURL + "/";
    }
    url = new URL(baseURL);
    url = new URL(url, NOTIFICATIONS_URL);
    return url.toExternalForm();
  }

  /**
   * Create a new subscription on the specified remote DDS service.
   *
   * @param event
   */
  private void register(RegistrationEvent event) throws IllegalArgumentException {
    if (event.getEvent() != Event.Register) {
      throw new IllegalArgumentException("register: invalid event type " + event.getEvent());
    }

    final String remoteDdsURL = event.getUrl();

    // We will register for all events on all documents.
    FilterCriteriaType criteria = FACTORY.createFilterCriteriaType();
    criteria.getEvent().add(DocumentEventType.ALL);
    FilterType filter = FACTORY.createFilterType();
    filter.getInclude().add(criteria);
    SubscriptionRequestType request = FACTORY.createSubscriptionRequestType();
    request.setFilter(filter);
    request.setRequesterId(nsiProperties.getNsaId());

    try {
      request.setCallback(getNotificationURL());
    } catch (MalformedURLException mx) {
      log.error("[RegistrationActor]: failed to get my notification callback URL, failing registration for {}", remoteDdsURL, mx);
      return;
    }

    Client client = ddsClient.get();
    WebTarget webTarget = client.target(remoteDdsURL).path("subscriptions");

    Response response = null;
    try {
      log.debug("[RegistrationActor] registering with remote DDS {}", remoteDdsURL);
      String encoded = DdsParser.getInstance().subscriptionRequest2Xml(request);
      response = webTarget.request(Nsi.NSI_DDS_V1_XML)
              .header(HttpHeaders.CONTENT_ENCODING, "gzip")
              .post(Entity.entity(encoded, Nsi.NSI_DDS_V1_XML));

      if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
        // Looks like we were successful so save the subscription information.
        SubscriptionType newSubscription = response.readEntity(SubscriptionType.class);

        log.debug("[RegistrationActor] registered with remote DDS {}, id={}, href={}", remoteDdsURL, newSubscription.getId(), newSubscription.getHref());

        Subscription subscription = new Subscription();
        subscription.setDdsURL(remoteDdsURL);
        subscription.setHref(newSubscription.getHref());
        if (response.getLastModified() == null) {
          // We should have gotten a valid lastModified date back.  Fake one
          // until we have worked out all the failure cases.  This will open
          // a small window of inaccuracy.
          log.error("[RegistrationActor] invalid LastModified header for id={}, href={}", newSubscription.getId(), newSubscription.getHref());
          subscription.setCreated((System.currentTimeMillis() / 1000) * 1000);
        } else {
          subscription.setCreated(response.getLastModified().getTime());
        }

        subscriptionService.create(subscription);

        // Now that we have registered a new subscription make sure we clean up
        // any old ones that may exist on the remote DDS.
        deleteOldSubscriptions(remoteDdsURL, newSubscription.getId());
      } else {
        log.error("[RegistrationActor] failed to create subscription {}, result = {}", remoteDdsURL,
                response.getStatusInfo().getReasonPhrase());

        ErrorType error = response.readEntity(ErrorType.class);
        if (error != null) {
          log.error("[RegistrationActor] Add of subscription failed, id={}, label={}, resource={}, description={}",
                  error.getId(), error.getLabel(), error.getResource(), error.getDescription());
        } else {
          log.error("[RegistrationActor] Add of subscription failed, endpoint={}, reason={}",
                  remoteDdsURL, response.getStatusInfo().getReasonPhrase());
        }
      }
    } catch (JAXBException | IOException ex) {
      log.error("[RegistrationActor] error on endpoint {}", remoteDdsURL, ex);
    } finally {
      if (response != null) {
        response.close();
      }
    }
  }

  private void deleteOldSubscriptions(String remoteDdsURL, String id) {
    Client client = ddsClient.get();

    WebTarget webTarget = client.target(remoteDdsURL).path("subscriptions")
            .queryParam("requesterId", nsiProperties.getNsaId());
    Response response = null;
    try {
      response = webTarget.request(Nsi.NSI_DDS_V1_XML).get();

      if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        // Looks like we were successful so save the subscription information.
        SubscriptionListType subscriptions = response.readEntity(SubscriptionListType.class);

        // For each subscription returned registered to our nsaId we check to
        // see if it is the one we just registered (current subscription).  If
        // it is not we delete the subscription.
        subscriptions.getSubscription()
                .stream()
                .filter((subscription) -> (!id.equalsIgnoreCase(subscription.getId())))
                .map((subscription) -> {
                  // Found one we need to remove.
                  log.debug("[RegistrationActor] found stale subscription {} on DDS {}", subscription.getHref(),
                          webTarget.getUri().toASCIIString());
                  return subscription;
                })
                .forEach((subscription) -> {
                  deleteSubscription(remoteDdsURL, subscription.getHref());
                });
      } else {
        log.error("RegistrationActor] Failed to retrieve list of subscriptions {}, result = {}", remoteDdsURL,
                response.getStatusInfo().getReasonPhrase());
        ErrorType error = response.readEntity(ErrorType.class);
        if (error != null) {
          log.error("[RegistrationActor] Subscription get failed, href={}, error={}.", webTarget.getUri().toASCIIString(), error.getId());
        } else {
          log.error("[RegistrationActor] Subscription get failed, href={}, reason={}.", webTarget.getUri().toASCIIString(), response.getStatusInfo().getReasonPhrase());
        }
      }
    } catch (Exception ex) {
      log.error("[RegistrationActor] GET failed for href={}, ex={}", webTarget.getUri().toASCIIString(), ex);
    } finally {
      if (response != null) {
        response.close();
      }
    }
  }

  private void update(RegistrationEvent event) throws IllegalArgumentException {
    if (event.getEvent() != Event.Update) {
      throw new IllegalArgumentException("update: invalid event type " + event.getEvent());
    }

    Client client = ddsClient.get();

    // First we retrieve the remote subscription to see if it is still
    // valid.  If it is not then we register again, otherwise we leave it
    // alone for now.
    Subscription subscription = subscriptionService.get(event.getUrl());
    String subscriptionURL = subscription.getHref();

    // Check to see if the remote subscription URL is absolute or relative.
    WebTarget webTarget;
    if (UrlHelper.isAbsolute(subscriptionURL)) {
      webTarget = client.target(subscriptionURL);
    } else {
      webTarget = client.target(subscription.getDdsURL()).path(subscriptionURL);
    }

    String absoluteURL = webTarget.getUri().toASCIIString();

    // Read the remote subscription to determine existanxe and last update time.
    subscription.setLastAudit(System.currentTimeMillis());
    Response response = null;
    try {
      Date lastModified = new Date();
      lastModified.setTime(subscription.getLastModified());

      log.debug("[RegistrationActor] getting subscription={},lastModified={}", absoluteURL, lastModified);

      response = webTarget.request(Nsi.NSI_DDS_V1_XML).header("If-Modified-Since",
              DateUtils.formatDate(lastModified, DateUtils.PATTERN_RFC1123)).get();

      // We found the subscription and it was not updated.
      if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
        // The subscription exists and has not been modified.
        log.debug("[RegistrationActor] subscription exists (not modified), url={}.", absoluteURL);
        subscription.setLastSuccessfulAudit(System.currentTimeMillis());
      } // We found the subscription and it was updated.
      else if (response.getStatus() == Response.Status.OK.getStatusCode()) {
        // The subscription exists but was modified since our last query.
        // Save the new version even though we should have know about it.
        subscription.setLastModified(response.getLastModified().getTime());
        subscription.setLastSuccessfulAudit(System.currentTimeMillis());
        SubscriptionType update = response.readEntity(SubscriptionType.class);
        subscription.setHref(update.getHref());
        log.info("[RegistrationActor] Subscription update detected, url={}, lastModified={}",
                absoluteURL, response.getLastModified().toString());
      } // We did not find the subscription so will need to create a new one.
      else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
        // Looks like our subscription was removed. We need to add it back in.
        log.error("[RegistrationActor] Subscription not found, url={}", absoluteURL);

        // Remove the stored subscription since a new one will be created.
        subscriptionService.delete(subscription.getDdsURL());
        event.setEvent(Event.Register);
        register(event);
      } // An unexpected error has occured.
      else {
        // Some other error we cannot handle at the moment.
        ErrorType error = response.readEntity(ErrorType.class);
        if (error != null) {
          log.error("[RegistrationActor] Subscription get failed, url={}, error={}", absoluteURL, error.getId());
        } else {
          log.error("[RegistrationActor] Subscription get failed, url={}, reason={}", absoluteURL,
                  response.getStatusInfo().getReasonPhrase());
        }
      }
    } catch (IllegalArgumentException ex) {
      log.error("[RegistrationActor] GET failed for url={}, ex={}", absoluteURL, ex);
    } finally {
      if (response != null) {
        response.close();
      }
    }
  }

  private void delete(RegistrationEvent event) throws IllegalArgumentException {
    if (event.getEvent() != Event.Delete) {
      throw new IllegalArgumentException("[RegistrationActor] Delete contains invalid event type " + event.getEvent());
    }

    Subscription subscription = subscriptionService.get(event.getUrl());
    if (deleteSubscription(subscription.getDdsURL(), subscription.getHref())) {
      subscriptionService.delete(subscription.getDdsURL());
    }
  }

  private boolean deleteSubscription(String remoteDdsURL, String remoteSubscriptionURL) {
    Client client = ddsClient.get();

    // Check to see if the remote subscription URL is absolute or relative.
    WebTarget webTarget;
    if (UrlHelper.isAbsolute(remoteSubscriptionURL)) {
      webTarget = client.target(remoteSubscriptionURL);
    } else {
      webTarget = client.target(remoteDdsURL).path(remoteSubscriptionURL);
    }

    String absoluteURL = webTarget.getUri().toASCIIString();

    boolean result = true;
    Response response = null;
    try {
      response = webTarget.request(Nsi.NSI_DDS_V1_XML).delete();

      if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
        // Successfully deleted the subscription.
        log.info("[RegistrationActor] Subscription deleted, url={}.", absoluteURL);
      } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
        log.error("[RegistrationActor] Subscription deleted FAILED, url={}, reason={}.", absoluteURL,
                response.getStatusInfo().getReasonPhrase());
      } else {
        ErrorType error = response.readEntity(ErrorType.class);
        if (error != null) {
          log.error("[RegistrationActor] Subscription deleted FAILED, url={}, errorId = {}", absoluteURL, error.getId());
        }
        else {
          log.error("[RegistrationActor] Subscription deleted FAILED, url={}, reason = {}", absoluteURL, response.getStatusInfo().getReasonPhrase());
        }
        result = false;
      }

    } catch (Exception ex) {
      log.error("[RegistrationActor] Subscription deleted FAILED, url={}, ex = {}", absoluteURL, ex);
      result = false;
    } finally {
      if (response != null) {
        response.close();
      }
    }
    return result;
  }
}
