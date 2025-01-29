package net.es.sense.rm.driver.nsi.dds.actors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jakarta.ws.rs.core.Response.Status;
import net.es.nsi.common.util.XmlUtilities;
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
  private final NsiProperties nsiProperties;
  private final SubscriptionService subscriptionService;
  private final DdsProvider ddsProvider;

  /**
   * Constructor for bean injection.
   *
   * @param nsiProperties Configuration settings.
   * @param subscriptionService The subscription service for storing NSI-DDS subscriptions.
   * @param ddsProvider The DDS provider for reference to DDS services such as the DDS client.
   */
  public RegistrationActor(NsiProperties nsiProperties, SubscriptionService subscriptionService,
                           DdsProvider ddsProvider) {
    this.nsiProperties = nsiProperties;
    this.subscriptionService = subscriptionService;
    this.ddsProvider = ddsProvider;
  }

  @Override
  public void preStart() {
  }

  @Override
  public void onReceive(Object msg) {
    if (msg instanceof RegistrationEvent event) {
      log.info("[RegistrationActor::onReceive] event={}", event);

      switch (event.getEvent()) {
        case Register -> register(event);
        case Update -> update(event);
        case Delete -> delete(event);
        default -> unhandled(msg);
      }
    } else {
      log.error("[RegistrationActor::onReceive] unhandled={}", msg.getClass());
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

    log.info("[RegistrationActor::register] received event = {}", event);

    // We want to subscribe to the following DDS service.
    final String remoteDdsURL = event.getUrl();

    // Build our callback url.
    String notificationURL;
    try {
      notificationURL = getNotificationURL();
    } catch (MalformedURLException mx) {
      log.error("[RegistrationActor::register]: failed to get notification callback URL, " +
          "failing registration for {}", remoteDdsURL, event, mx);
      return;
    }

    // Send a subscription request.
    final DdsClient client = ddsProvider.getDdsClient();

    log.info("[RegistrationActor::register] subscribing remoteDdsURL = {}, nsaId = {}, notificationURL = {}",
        remoteDdsURL, nsiProperties.getNsaId(), notificationURL);

    // We will register for all events on all documents.
    SubscriptionResult subscribe = client.subscribe(remoteDdsURL, nsiProperties.getNsaId(), notificationURL);
    if (Status.CREATED == subscribe.getStatus()) {
      SubscriptionType newSubscription = subscribe.getSubscription();

      log.info("[RegistrationActor::register] received subscription = {}",
          XmlUtilities.jaxbToXml(SubscriptionType.class, newSubscription));

      // We will need to save this in our subscription cache.
      Subscription subscription = new Subscription();
      subscription.setDdsURL(remoteDdsURL);
      subscription.setHref(newSubscription.getHref());
      subscription.setCreated(subscribe.getLastModified());
      subscriptionService.create(subscription);

      log.info("[RegistrationActor::register] saving new subscription = {}", subscription);

      // Now that we have registered a new subscription make sure we clean up
      // any old ones that may exist on the remote DDS.
      SubscriptionsResult unsubscribe =
          client.unsubscribe(remoteDdsURL, nsiProperties.getNsaId(), newSubscription.getId());
      for (SubscriptionType s : unsubscribe.getSubscriptions()) {
        log.info("[RegistrationActor::register] unregistered stale subscription = {}", subscribe);
      }
    } else {
      log.error("[RegistrationActor::register] failed to create subscription " +
              "remoteDdsURL = {}, subscription = {}", remoteDdsURL, subscribe);
    }
  }

  /**
   * This method handles the registration update event.
   *
   * @param event The update event to be processed.
   *
   * @throws IllegalArgumentException
   */
  private void update(RegistrationEvent event) throws IllegalArgumentException {
    if (event.getEvent() != Event.Update) {
      throw new IllegalArgumentException("update: invalid event type " + event.getEvent());
    }

    log.info("[RegistrationActor::update] processing update event = {}", event);

    // First we retrieve the remote subscription to see if it is still
    // valid.  If it is not then we register again, otherwise we leave it
    // alone for now.
    final String remoteDdsURL = event.getUrl();
    Subscription subscription = subscriptionService.get(remoteDdsURL);
    if (subscription == null) {
      log.error("[RegistrationActor::update] invalid subscription URL " + remoteDdsURL);
      throw new IllegalArgumentException("[RegistrationActor::update] invalid subscription URL " + remoteDdsURL);
    }

    log.info("[RegistrationActor::update] found matching subscription = {}", subscription);

    String subscriptionURL = subscription.getHref();
    Date lastModified = new Date();
    lastModified.setTime(subscription.getLastModified());
    subscription.setLastAudit(System.currentTimeMillis());

    // Read the remote subscription to determine if it exists and last update time.
    final DdsClient client = ddsProvider.getDdsClient();
    SubscriptionResult subscribe = client.getSubscription(remoteDdsURL, subscriptionURL, lastModified);

    log.info("[RegistrationActor::update] getSubscription result = {}", subscribe);

    switch (subscribe.getStatus()) {
      // We found the subscription and it was updated.
      case NOT_MODIFIED -> {
        // The subscription exists and has not been modified.
        log.info("[RegistrationActor::update] subscription exists (not modified), url={}.", subscriptionURL);
        subscription.setLastSuccessfulAudit(System.currentTimeMillis());
        subscriptionService.update(subscription);
      }
      // We did not find the subscription so will need to create a new one.
      case OK -> {
        // The subscription exists but was modified since our last query.
        // Save the new version even though we should have know about it.
        subscription.setLastModified(subscribe.getLastModified());
        subscription.setLastSuccessfulAudit(System.currentTimeMillis());
        subscription.setHref(subscribe.getSubscription().getHref());
        log.info("[RegistrationActor::update] Subscription update detected, url={}, lastModified={}",
            subscriptionURL, subscribe.getLastModified());
        subscriptionService.update(subscription);
      }
      // An unexpected error has occurred.
      case NOT_FOUND -> {
        // Looks like our subscription was removed. We need to add it back in.
        log.error("[RegistrationActor::update] Subscription not found, url={}", subscriptionURL);
        // Remove the stored subscription since a new one will be created.
        subscriptionService.delete(subscription.getDdsURL());
        event.setEvent(Event.Register);
        register(event);
      }
      default -> {
        log.error("[RegistrationActor::update] Subscription get failed, url={}, error={}",
            subscriptionURL, subscribe.getStatus());

        // Remove the stored subscription since a new one will be created.
        subscriptionService.delete(subscription.getDdsURL());
        event.setEvent(Event.Register);
        register(event);
      }
    }
  }

  private void delete(RegistrationEvent event) throws IllegalArgumentException {
    log.info("[RegistrationActor::delete] event = {}", event);
    if (event.getEvent() != Event.Delete) {
      throw new IllegalArgumentException("[RegistrationActor] Delete contains invalid event type " + event.getEvent());
    }

    Subscription subscription = subscriptionService.get(event.getUrl());
    log.info("[RegistrationActor::delete] looked up subscription = {}", subscription);
    if (subscription != null) {
      final DdsClient client = ddsProvider.getDdsClient();
      SubscriptionResult unsubscribe = client.unsubscribe(event.getUrl(), subscription.getHref());
      log.info("[RegistrationActor::delete] unsubscribe result = {}", unsubscribe);
    }
  }
}
