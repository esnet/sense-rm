package net.es.sense.rm.driver.nsi.dds.actors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jakarta.ws.rs.core.Response.Status;
import net.es.nsi.dds.lib.client.DdsClient;
import net.es.nsi.dds.lib.client.SubscriptionResult;
import net.es.nsi.dds.lib.client.SubscriptionsResult;
import net.es.nsi.dds.lib.jaxb.dds.SubscriptionType;
import net.es.sense.rm.driver.nsi.dds.DdsProvider;
import net.es.sense.rm.driver.nsi.dds.db.Subscription;
import net.es.sense.rm.driver.nsi.dds.db.SubscriptionService;
import net.es.sense.rm.driver.nsi.dds.messages.RegistrationEvent;
import net.es.sense.rm.driver.nsi.dds.messages.RegistrationEvent.Event;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The Registration Actor handles local DDS subscription registrations to peer DDS services.
 *
 * @author hacksaw
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RegistrationActor extends UntypedAbstractActor {
  LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  private static final String NOTIFICATIONS_URL = "notifications";

  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private SubscriptionService subscriptionService;

  @Autowired
  private DdsProvider ddsProvider;

  @Override
  public void preStart() {
  }

  @Override
  public void onReceive(Object msg) {
    if (msg instanceof RegistrationEvent event) {
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

    // We want to subscribe to the following DDS service.
    final String remoteDdsURL = event.getUrl();

    // Build our callback url.
    String notificationURL;
    try {
      notificationURL = getNotificationURL();
    } catch (MalformedURLException mx) {
      log.error("[RegistrationActor]: failed to get my notification callback URL, failing registration for {}", remoteDdsURL, mx);
      return;
    }

    // Send a subscription request.
    final DdsClient client = ddsProvider.getDdsClient();

    // We will register for all events on all documents.
    SubscriptionResult subscribe = client.subscribe(remoteDdsURL, nsiProperties.getNsaId(), notificationURL);
    if (Status.CREATED == subscribe.getStatus()) {
      SubscriptionType newSubscription = subscribe.getSubscription();

      // We will need to save this in our subscription cache.
      Subscription subscription = new Subscription();
      subscription.setDdsURL(remoteDdsURL);
      subscription.setHref(newSubscription.getHref());
      subscription.setCreated(subscribe.getLastModified());
      subscriptionService.create(subscription);

      // Now that we have registered a new subscription make sure we clean up
      // any old ones that may exist on the remote DDS.
      SubscriptionsResult unsubscribe = client.unsubscribe(remoteDdsURL, nsiProperties.getNsaId(), newSubscription.getId());
      for (SubscriptionType s : unsubscribe.getSubscriptions()) {
        log.info("[RegistrationActor] unregistered stale subscription, id = {}", s.getHref());
      }
    } else {
      log.error("[RegistrationActor] failed to create subscription {}, result = {}", remoteDdsURL,
              subscribe.getStatus());
    }
  }

  private void update(RegistrationEvent event) throws IllegalArgumentException {
    if (event.getEvent() != Event.Update) {
      throw new IllegalArgumentException("update: invalid event type " + event.getEvent());
    }

    // First we retrieve the remote subscription to see if it is still
    // valid.  If it is not then we register again, otherwise we leave it
    // alone for now.
    final String remoteDdsURL = event.getUrl();
    Subscription subscription = subscriptionService.get(remoteDdsURL);
    if (subscription == null) {
      throw new IllegalArgumentException("update: invalid subscription URL " + remoteDdsURL);
    }

    String subscriptionURL = subscription.getHref();
    Date lastModified = new Date();
    lastModified.setTime(subscription.getLastModified());
    subscription.setLastAudit(System.currentTimeMillis());

    log.debug("[RegistrationActor] getting subscription={},lastModified={}, subLastModified={}",
              subscriptionURL, lastModified, subscription.getLastModified());

    // Read the remote subscription to determine if it exists and last update time.
    final DdsClient client = ddsProvider.getDdsClient();
    SubscriptionResult subscribe = client.getSubscription(remoteDdsURL, subscriptionURL, lastModified);

    switch (subscribe.getStatus()) {
    // We found the subscription and it was updated.
      case NOT_MODIFIED:
        // The subscription exists and has not been modified.
        log.debug("[RegistrationActor] subscription exists (not modified), url={}.", subscriptionURL);
        subscription.setLastSuccessfulAudit(System.currentTimeMillis());
        subscriptionService.update(subscription);
        break;
    // We did not find the subscription so will need to create a new one.
      case OK:
        // The subscription exists but was modified since our last query.
        // Save the new version even though we should have know about it.
        subscription.setLastModified(subscribe.getLastModified());
        subscription.setLastSuccessfulAudit(System.currentTimeMillis());
        subscription.setHref(subscribe.getSubscription().getHref());
        log.info("[RegistrationActor] Subscription update detected, url={}, lastModified={}",
                subscriptionURL, subscribe.getLastModified());
        subscriptionService.update(subscription);
        break;
    // An unexpected error has occurred.
      case NOT_FOUND:
        // Looks like our subscription was removed. We need to add it back in.
        log.error("[RegistrationActor] Subscription not found, url={}", subscriptionURL);
        // Remove the stored subscription since a new one will be created.
        subscriptionService.delete(subscription.getDdsURL());
        event.setEvent(Event.Register);
        register(event);
        break;
      default:
        log.error("[RegistrationActor] Subscription get failed, url={}, error={}", subscriptionURL, subscribe.getStatus());

        // Remove the stored subscription since a new one will be created.
        subscriptionService.delete(subscription.getDdsURL());
        event.setEvent(Event.Register);
        register(event);
        break;
    }
  }

  private void delete(RegistrationEvent event) throws IllegalArgumentException {
    log.debug("[RegistrationActor.delete] event = {}, url = {}, initiator = {}, path = {}",
        event.getEvent(), event.getUrl(), event.getInitiator(), event.getPath());
    if (event.getEvent() != Event.Delete) {
      throw new IllegalArgumentException("[RegistrationActor] Delete contains invalid event type " + event.getEvent());
    }

    Subscription subscription = subscriptionService.get(event.getUrl());
    if (subscription != null) {
      final DdsClient client = ddsProvider.getDdsClient();
      client.unsubscribe(event.getUrl(), subscription.getHref());
    }
  }
}
