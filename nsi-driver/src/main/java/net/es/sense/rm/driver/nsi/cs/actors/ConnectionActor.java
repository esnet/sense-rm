package net.es.sense.rm.driver.nsi.cs.actors;

import java.util.concurrent.TimeUnit;
import akka.actor.UntypedAbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.soap.SOAPFaultException;
import net.es.nsi.common.jaxb.JaxbParser;
import net.es.nsi.cs.lib.Client;
import net.es.nsi.cs.lib.Helper;
import net.es.nsi.cs.lib.NsiHeader;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.cs.api.QuerySummary;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import net.es.sense.rm.driver.nsi.messages.Message;
import net.es.sense.rm.driver.nsi.messages.TimerMsg;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.ogf.schemas.nsi._2013._12.connection.provider.Error;
import org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.QueryType;
import org.ogf.schemas.nsi._2013._12.framework.headers.CommonHeaderType;
import org.ogf.schemas.nsi._2013._12.framework.types.ServiceExceptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ConnectionActor extends UntypedAbstractActor {
  LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

  @Autowired
  Environment environment;

  @Autowired
  private NsiActorSystem nsiActorSystem;

  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private ReservationService reservationService;

  private static final ObjectFactory FACTORY = new ObjectFactory();

  @Override
  public void preStart() {
    log.info("[ConnectionActor] preStart() is scheduling audit.");
    TimerMsg message = new TimerMsg("ConnectionActor:preStart", this.self().path());
    nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getConnectionAuditTimer(),
            TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
  }

  @Override
  public void onReceive(Object msg) {
    log.info("[ConnectionActor] onReceive {}", msg.getClass().getName());
    if (msg instanceof TimerMsg) {
      // Perform connection audit.
      try {
        //connectionSummaryAudit();
        connectionSummarySyncAudit();
      } catch (ServiceException ex) {
        log.error(ex, "[ConnectionActor] onReceive connection audit failed");
      }

      // Schedule next audit.
      // Insert code here to handle local documents (i.e. create and push to remote DDS server?
      nsiActorSystem.getActorSystem().scheduler()
          .scheduleOnce(Duration.create(nsiProperties.getConnectionAuditTimer(), TimeUnit.SECONDS),
              this.getSelf(), (TimerMsg) msg, nsiActorSystem.getActorSystem().dispatcher(), null);
    } else {
      log.error("[ConnectionActor::onReceive] Unhandled event {}", Message.getDebug(msg));
      unhandled(msg);
    }
  }

  /**
   * Send a summary query to our associated NSA to get a list of all available
   * connections.  We add these to the connection repository for processing into
   * MRML topology.
   *
   */
  private void connectionSummaryAudit() throws ServiceException {
    Client nsiClient = new Client(nsiProperties.getProviderConnectionURL());

    CommonHeaderType requestHeader = NsiHeader.builder()
            .correlationId(Helper.getUUID())
            .providerNSA(nsiProperties.getProviderNsaId())
            .requesterNSA(nsiProperties.getNsaId())
            .replyTo(nsiProperties.getRequesterConnectionURL())
            .build()
            .getRequestHeaderType();

    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;

    QueryType query = FACTORY.createQueryType();
    try {
      log.info("[ConnectionActor] Sending querySummary: correlationId = {}", requestHeader.getCorrelationId());
      nsiClient.getProxy().querySummary(query, header);
      log.info("[ConnectionActor] Ack received, providerNSA = {}, correlationId = {}",
              header.value.getProviderNSA(), header.value.getCorrelationId());
    } catch (org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException ex) {
      log.error("[ConnectionActor] querySummary exception - {} {}",
              ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      log.error(JaxbParser.jaxb2String(ServiceExceptionType.class, ex.getFaultInfo()));
      throw ex;
    }
  }

  private static final org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory CS_FACTORY
      = new org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory();

  /**
   * Send a summary query to our associated NSA to get a list of all available
   * connections.  We add these to the connection repository for processing into
   * MRML topology.
   *
   */
  private void connectionSummarySyncAudit() throws ServiceException {
    Client nsiClient = new Client(nsiProperties.getProviderConnectionURL());
    Holder<CommonHeaderType> header = getNsiCsHeader();
    QueryType query = CS_FACTORY.createQueryType();
    try {
      log.info("[connectionSummarySyncAudit] Sending querySummarySync: providerNSA = {}, correlationId = {}",
          header.value.getProviderNSA(), header.value.getCorrelationId());
      QuerySummaryConfirmedType querySummarySync = nsiClient.getProxy().querySummarySync(query, header);
      log.info("[connectionSummarySyncAudit] QuerySummaryConfirmed received, providerNSA = {}, correlationId = {}",
          header.value.getProviderNSA(), header.value.getCorrelationId());

      QuerySummary q = new QuerySummary(nsiProperties.getNetworkId(), reservationService);
      q.process(querySummarySync, header);
    } catch (Error ex) {
      log.error("[connectionSummarySyncAudit] querySummarySync exception on operation - {} {}",
          ex.getFaultInfo().getServiceException().getErrorId(),
          ex.getFaultInfo().getServiceException().getText());
    } catch (org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException ex) {
      log.error("[connectionSummarySyncAudit] querySummarySync exception processing - {} {}",
          ex.getFaultInfo().getErrorId(),
          ex.getFaultInfo().getText());
    } catch (SOAPFaultException ex) {
      log.error("[connectionSummarySyncAudit] querySummarySync SOAPFaultException exception", ex);
    } catch (Exception ex) {
      log.error("[connectionSummarySyncAudit] querySummarySync exception processing results", ex);
    }
  }

  /**
   * This method returns a fully populated NSI-CS header for inclusion in a SOAP message.
   *
   * @return NSI-CS header.
   */
  private Holder<CommonHeaderType> getNsiCsHeader() {
    Holder<CommonHeaderType> header = new Holder<>();
    header.value = NsiHeader.builder()
        .correlationId(Helper.getUUID())
        .providerNSA(nsiProperties.getProviderNsaId())
        .requesterNSA(nsiProperties.getNsaId())
        .replyTo(nsiProperties.getRequesterConnectionURL())
        .build()
        .getRequestHeaderType();
    return header;
  }

  /**
   * Send a recursive query to our associated NSA to get a list of all available
   * connections.  We add these to the connection repository for processing into
   * MRML topology.
   *
   */
  private void connectionRecursiveAudit() throws ServiceException {
    Client nsiClient = new Client(nsiProperties.getProviderConnectionURL());

    CommonHeaderType requestHeader = NsiHeader.builder()
            .correlationId(Helper.getUUID())
            .providerNSA(nsiProperties.getProviderNsaId())
            .requesterNSA(nsiProperties.getNsaId())
            .replyTo(nsiProperties.getRequesterConnectionURL())
            .build()
            .getRequestHeaderType();

    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;

    QueryType query = FACTORY.createQueryType();
    try {
      log.info("[ConnectionActor] Sending queryRecursive: correlationId = {}", requestHeader.getCorrelationId());
      nsiClient.getProxy().queryRecursive(query, header);
      log.info("[ConnectionActor] Ack recieved, providerNSA = {}, correlationId = {}", header.value.getProviderNSA(), header.value.getCorrelationId());
    } catch (org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException ex) {
      log.error("[ConnectionActor] queryRecursive exception - {} {}", ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      log.error(JaxbParser.jaxb2String(ServiceExceptionType.class, ex.getFaultInfo()));
      throw ex;
    }
  }
}
