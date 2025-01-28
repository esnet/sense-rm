package net.es.sense.rm.driver.nsi.dds.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.dds.db.Subscription;
import net.es.sense.rm.driver.nsi.dds.db.SubscriptionService;
import net.es.sense.rm.driver.nsi.dds.messages.RegistrationEvent;
import net.es.sense.rm.driver.nsi.dds.messages.SubscriptionQuery;
import net.es.sense.rm.driver.nsi.dds.messages.SubscriptionQueryResult;
import net.es.sense.rm.driver.nsi.messages.Message;
import net.es.sense.rm.driver.nsi.messages.StartMsg;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 * The Registration Router handles routing and distribution of DDS subscription
 * registrations to RegistrationActors.
 *
 * @author hacksaw
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RegistrationRouter extends UntypedAbstractActor {
  private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  // These fields are injected through the constructor.
  private final SpringExtension springExtension;
  private final NsiActorSystem nsiActorSystem;
  private final NsiProperties nsiProperties;
  private final SubscriptionService subscriptionService;

  // The AKKA router that is created on preStart().
  private Router router;

  /**
   * Default constructor called by Spring to initialize the actor.
   *
   * @param springExtension
   * @param nsiActorSystem
   * @param nsiProperties
   * @param subscriptionService
   */
  public RegistrationRouter(SpringExtension springExtension, NsiActorSystem nsiActorSystem,
                            NsiProperties nsiProperties, SubscriptionService subscriptionService) {
    this.springExtension = springExtension;
    this.nsiActorSystem = nsiActorSystem;
    this.nsiProperties = nsiProperties;
    this.subscriptionService = subscriptionService;
  }

  /**
   * Initialize the actor by creating pool of RegistrationActors to do the work.
   */
  @Override
  public void preStart() {
    log.info("[RegistrationRouter] Initializing registrationActors, debug= {}.", log.isDebugEnabled());

    List<Routee> routees = new ArrayList<>();
    for (int i = 0; i < nsiProperties.getDdsPoolSize(); i++) {
      ActorRef r = getContext().actorOf(springExtension.props("registrationActor"));
      getContext().watch(r);
      routees.add(new ActorRefRoutee(r));
    }
    router = new Router(new RoundRobinRoutingLogic(), routees);

    // Kick off those that need to be started.
    nsiActorSystem.getActorSystem().scheduler()
        .scheduleOnce(Duration.create(60, TimeUnit.SECONDS), this.getSelf(),
            new StartMsg("RegistrationRouter:preStart", this.self().path()),
            nsiActorSystem.getActorSystem().dispatcher(), null);

    log.info("[RegistrationRouter] Initialization completed.");
  }

  /**
   * Process an incoming registration messages and distribute to actor pool.
   *
   * @param msg The message to process.
   */
  @Override
  public void onReceive(Object msg) {
    log.info("[RegistrationRouter::onReceive] onReceive {}", Message.getDebug(msg));

    // Check to see if we got the go ahead to start registering.
    if (msg instanceof StartMsg message) {
      log.info("[RegistrationRouter::onReceive] start timer so convert to registration event.");

      // Create a Register event to start us off.
      RegistrationEvent event = new RegistrationEvent(message.getInitiator(), message.getPath());
      event.setEvent(RegistrationEvent.Event.Register);
      msg = event;
    }

    if (msg instanceof RegistrationEvent re) {
      if (null != re.getEvent()) switch (re.getEvent()) {
        case Register -> {
          // This is our first time through after initialization.
          log.info("[RegistrationRouter::onReceive] routeRegister");
          routeRegister();
        }
        case Audit -> {
          // A regular audit event.
          log.info("[RegistrationRouter::onReceive] routeAudit request.");
          routeAudit();
        }
        case Delete -> {
          // We are shutting down so clean up.
          log.info("[RegistrationRouter::onReceive] routeShutdown");
          routeShutdown();
          return;
        }
        default -> {
          return;
        }
      }
    } else if (msg instanceof SubscriptionQuery query) {
      log.info("[RegistrationRouter::onReceive] Subscription query.");
      SubscriptionQueryResult subscription = new SubscriptionQueryResult();
      subscription.setSubscription(getSubscription(query.getUrl()));
      getSender().tell(subscription, self());
      return;
    } else if (msg instanceof Terminated terminated) {
      log.error("[RegistrationRouter::onReceive] terminate event for {}", terminated.actor().path());
      router = router.removeRoutee(terminated.actor());
      ActorRef r = getContext().actorOf(Props.create(RegistrationActor.class));
      getContext().watch(r);
      router = router.addRoutee(new ActorRefRoutee(r));
      return;
    } else {
      log.error("[RegistrationRouter::onReceive] Unhandled event {}", Message.getDebug(msg));
      unhandled(msg);
      return;
    }

    RegistrationEvent event = new RegistrationEvent("RegistrationRouter:onReceive", this.getSelf().path());
    event.setEvent(RegistrationEvent.Event.Audit);
    getContext().getSystem().scheduler()
        .scheduleOnce(Duration.create(nsiProperties.getDdsAuditTimer(), TimeUnit.SECONDS),
            this.getSelf(), event, getContext().getSystem().dispatcher(), null);
  }

  /**
   * Route an incoming registration message to an actor for processing.
   */
  private void routeRegister() {
    // We need to register invoke a registration actor for each remote DDS
    // we are peering with.
    nsiProperties.getPeers().forEach((url) -> {
      RegistrationEvent regEvent = new RegistrationEvent();
      regEvent.setEvent(RegistrationEvent.Event.Register);
      regEvent.setUrl(url);
      log.info("[RegistrationRouter::routeRegister] registering url={}", url);
      router.route(regEvent, this.getSelf());
    });
  }

  /**
   * Route an incoming registration audit message to an actor for processing.
   */
  private void routeAudit() {
    // Check the list of discovery URL against what we already have.
    List<String> discoveryURL = nsiProperties.getPeers();
    Set<String> subscriptionURL = subscriptionService.keySet();

    for (String url : discoveryURL) {
      log.info("[RegistrationRouter::routeAudit] processing discovery URL={}.", url);

      // See if we already have seen this URL.  If we have not then
      // we need to create a new remote subscription.
      Subscription sub = subscriptionService.get(url);
      if (sub == null) {
        // We have not seen this before.
        log.info("[RegistrationRouter::routeAudit] creating new registration for url={}.", url);

        RegistrationEvent regEvent = new RegistrationEvent();
        regEvent.setEvent(RegistrationEvent.Event.Register);
        regEvent.setUrl(url);
        router.route(regEvent, this.getSelf());
      } else {
        // We have seen this URL before.
        log.info("[RegistrationRouter::routeAudit] auditing registration for url={}.", url);
        RegistrationEvent regEvent = new RegistrationEvent();
        regEvent.setEvent(RegistrationEvent.Event.Update);
        regEvent.setUrl(url);
        router.route(regEvent, this.getSelf());

        // Remove from the existing list as processed.
        subscriptionURL.remove(url);
      }
    }

    // Now we see if there are any URL we missed from the old list and
    // unsubscribe them since we seem to no longer be interested.
    subscriptionURL.stream()
            .map(subscriptionService::get)
            .filter(Objects::nonNull).map((sub) -> {
          // Should always be true unless modified while we are processing.
          log.info("[RegistrationRouter::routeAudit] deleting remote url={}.", sub.getDdsURL());
          RegistrationEvent regEvent = new RegistrationEvent();
          regEvent.setEvent(RegistrationEvent.Event.Delete);
          regEvent.setUrl(sub.getDdsURL());
          return regEvent;
    }).forEach((regEvent) -> {
      router.route(regEvent, getSelf());
    });
  }

  /**
   * Route an incoming shutdown message to an actor for processing.
   */
  private void routeShutdown() {
    log.info("[RegistrationRouter::routeShutdown] shutting down.");
    subscriptionService.keySet().stream().map(subscriptionService::get)
            .filter(Objects::nonNull).map((sub) -> {
          // Should always be true unless modified while we are processing.
          log.info("[RegistrationRouter::routeShutdown] deleting subscription url = {}", sub.getDdsURL());
          RegistrationEvent regEvent = new RegistrationEvent();
          regEvent.setEvent(RegistrationEvent.Event.Delete);
          regEvent.setUrl(sub.getDdsURL());
          return regEvent;
    }).forEachOrdered((regEvent) -> {
      router.route(regEvent, getSelf());
    });
  }

  /**
   * Return true if the provided URL is a valid subscription.
   *
   * @param url
   * @return
   */
  public boolean isSubscription(String url) {
    return subscriptionService.getByHref(url) != null;
  }

  /**
   * Get the subscription associated with the provided URL.
   *
   * @param url The URL identifying the subscription to retrieve.
   * @return The matching subscription if one exists.
   */
  public Subscription getSubscription(String url) {
    return subscriptionService.getByHref(url);
  }
}
