package net.es.sense.rm.driver.nsi.dds.actors;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.es.sense.rm.driver.nsi.configuration.NsiProperties;
import net.es.sense.rm.driver.nsi.dds.db.Subscription;
import net.es.sense.rm.driver.nsi.dds.db.SubscriptionRepository;
import net.es.sense.rm.driver.nsi.dds.messages.RegistrationEvent;
import net.es.sense.rm.driver.nsi.dds.messages.StartMsg;
import net.es.sense.rm.driver.nsi.dds.messages.SubscriptionQuery;
import net.es.sense.rm.driver.nsi.dds.messages.SubscriptionQueryResult;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
@Component
@Scope("prototype")
public class RegistrationRouter extends UntypedAbstractActor {

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  private final LoggingAdapter log = Logging.getLogger(getContext().system(), "RegistrationRouter");

  private Router router;

  @Override
  public void preStart() {
    log.info("[preStart] Initializing registrationActors, debug= {}.", log.isDebugEnabled());

    List<Routee> routees = new ArrayList<>();
    for (int i = 0; i < nsiProperties.getDdsPoolSize(); i++) {
      ActorRef r = getContext().actorOf(springExtension.props("registrationActor"));
      getContext().watch(r);
      routees.add(new ActorRefRoutee(r));
    }
    router = new Router(new RoundRobinRoutingLogic(), routees);

    log.info("[preStart] Initialization completed.");
  }

  @Override
  public void onReceive(Object msg) {
    // Check to see if we got the go ahead to start registering.
    if (msg instanceof StartMsg) {
      log.info("[onReceive] Start event");

      // Create a Register event to start us off.
      RegistrationEvent event = new RegistrationEvent();
      event.setEvent(RegistrationEvent.Event.Register);
      msg = event;
    }

    if (msg instanceof RegistrationEvent) {
      RegistrationEvent re = (RegistrationEvent) msg;
      if (null != re.getEvent()) switch (re.getEvent()) {
        case Register:
          // This is our first time through after initialization.
          log.debug("[onReceive] routeRegister");
          routeRegister();
          break;

        case Audit:
          // A regular audit event.
          log.debug("[onReceive] routeAudit");
          routeAudit();
          break;

        case Delete:
          // We are shutting down so clean up.
          log.debug("[onReceive] routeShutdown");
          routeShutdown();
          break;

        default:
          break;
      }
    } else if (msg instanceof SubscriptionQuery) {
      log.debug("[onReceive] Subscription query.");
      SubscriptionQuery query = (SubscriptionQuery) msg;
      SubscriptionQueryResult subscription = new SubscriptionQueryResult();
      subscription.setSubscription(getSubscription(query.getUrl()));
      getSender().tell(subscription, self());
    } else if (msg instanceof Terminated) {
      log.debug("[onReceive] Terminated event.");
      router = router.removeRoutee(((Terminated) msg).actor());
      ActorRef r = getContext().actorOf(Props.create(RegistrationActor.class));
      getContext().watch(r);
      router = router.addRoutee(new ActorRefRoutee(r));
    } else {
      log.error("[onReceive] Unhandled event.");
      unhandled(msg);
    }

    RegistrationEvent event = new RegistrationEvent();
    event.setEvent(RegistrationEvent.Event.Audit);
    getContext().getSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getDdsAuditTimer(), TimeUnit.SECONDS),
            this.getSelf(), event, getContext().getSystem().dispatcher(), null);
  }

  private void routeRegister() {
    // We need to register invoke a registration actor for each remote DDS
    // we are peering with.
    nsiProperties.getPeers().forEach((url) -> {
      RegistrationEvent regEvent = new RegistrationEvent();
      regEvent.setEvent(RegistrationEvent.Event.Register);
      regEvent.setUrl(url);
      log.debug("[onReceive] registering url={}", url);
      router.route(regEvent, this.getSelf());
    });
  }

  private void routeAudit() {
    // Check the list of discovery URL against what we already have.
    List<String> discoveryURL = nsiProperties.getPeers();
    Set<String> subscriptionURL = subscriptionRepository.keySet();

    for (String url : discoveryURL) {
      // See if we already have seen this URL.  If we have not then
      // we need to create a new remote subscription.
      Subscription sub = subscriptionRepository.findOne(url);
      if (sub == null) {
        // We have not seen this before.
        log.debug("[onReceive] creating new registration for url={}.", url);

        RegistrationEvent regEvent = new RegistrationEvent();
        regEvent.setEvent(RegistrationEvent.Event.Register);
        regEvent.setUrl(url);
        router.route(regEvent, this.getSelf());
      } else {
        // We have seen this URL before.
        log.debug("[onReceive] auditing registration for url={}.", url);
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
            .map((url) -> subscriptionRepository.findOne(url))
            .filter((sub) -> (sub != null)).map((sub) -> {
      // Should always be true unless modified while we are processing.
      RegistrationEvent regEvent = new RegistrationEvent();
      regEvent.setEvent(RegistrationEvent.Event.Delete);
      regEvent.setUrl(sub.getDdsURL());
      return regEvent;
    }).forEach((regEvent) -> {
      router.route(regEvent, getSelf());
    });
  }

  private void routeShutdown() {
    subscriptionRepository.keySet().stream().map((url) -> subscriptionRepository.findOne(url))
            .filter((sub) -> (sub != null)).map((sub) -> {
      // Should always be true unless modified while we are processing.
      RegistrationEvent regEvent = new RegistrationEvent();
      regEvent.setEvent(RegistrationEvent.Event.Delete);
      regEvent.setUrl(sub.getDdsURL());
      return regEvent;
    }).forEachOrdered((regEvent) -> {
      router.route(regEvent, getSelf());
    });
  }

  public boolean isSubscription(String url) {
    return subscriptionRepository.findByHref(url) != null;
  }

  public Subscription getSubscription(String url) {
    return subscriptionRepository.findByHref(url);
  }
}
