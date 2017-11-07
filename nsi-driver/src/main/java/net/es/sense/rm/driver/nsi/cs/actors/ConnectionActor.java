package net.es.sense.rm.driver.nsi.cs.actors;

import akka.actor.UntypedAbstractActor;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.jaxb.JaxbParser;
import net.es.nsi.cs.lib.Helper;
import net.es.nsi.cs.lib.NsiHeader;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.dds.messages.TimerMsg;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.ogf.schemas.nsi._2013._12.connection.provider.ConnectionProviderPort;
import org.ogf.schemas.nsi._2013._12.connection.provider.ConnectionServiceProvider;
import org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory;
import org.ogf.schemas.nsi._2013._12.connection.types.QueryType;
import org.ogf.schemas.nsi._2013._12.framework.headers.CommonHeaderType;
import org.ogf.schemas.nsi._2013._12.framework.types.ServiceExceptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
@Scope("prototype")
public class ConnectionActor extends UntypedAbstractActor {
  @Autowired
  Environment environment;

  @Autowired
  private NsiActorSystem nsiActorSystem;

  @Autowired
  private NsiProperties nsiProperties;

  private static final ObjectFactory FACTORY = new ObjectFactory();

  @Override
  public void preStart() {
    log.info("[ConnectionActor] preStart() is scheduling audit.");
    TimerMsg message = new TimerMsg();
    nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getConnectionAuditTimer(),
            TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
  }

  @Override
  public void onReceive(Object msg) {
    log.info("[ConnectionActor] onRecieve({}).", msg.getClass().getName());
    if (msg instanceof TimerMsg) {
      // Perform connection audit.
      try {
        connectionSummaryAudit();
      } catch (ServiceException ex) {
        log.error("[ConnectionActor] onReceive eating service exception");
      }

      // Schedule next audit.
      TimerMsg message = (TimerMsg) msg;
      // Insert code here to handle local documents (i.e. create and push to remote DDS server?
      nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(nsiProperties.getConnectionAuditTimer(),
              TimeUnit.SECONDS), this.getSelf(), message, nsiActorSystem.getActorSystem().dispatcher(), null);
    } else {
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
    ConnectionServiceProvider provider = new ConnectionServiceProvider();
    ConnectionProviderPort proxy = provider.getConnectionServiceProviderPort();
    BindingProvider bp = (BindingProvider) proxy;
    Map<String, Object> context = bp.getRequestContext();
    context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, nsiProperties.getProviderConnectionURL());
    context.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

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
      proxy.querySummary(query, header);
      log.info("[ConnectionActor] Ack recieved, providerNSA = {}, correlationId = {}", header.value.getProviderNSA(), header.value.getCorrelationId());
    } catch (org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException ex) {
      log.error("[ConnectionActor] querySummary exception - {} {}", ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      log.error(JaxbParser.jaxb2String(ServiceExceptionType.class, ex.getFaultInfo()));
      throw ex;
    }
  }

  /**
   * Send a recursive query to our associated NSA to get a list of all available
   * connections.  We add these to the connection repository for processing into
   * MRML topology.
   *
   */
  private void connectionRecursiveAudit() throws ServiceException {
    ConnectionServiceProvider provider = new ConnectionServiceProvider();
    ConnectionProviderPort proxy = provider.getConnectionServiceProviderPort();
    BindingProvider bp = (BindingProvider) proxy;
    Map<String, Object> context = bp.getRequestContext();
    context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, nsiProperties.getProviderConnectionURL());
    context.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

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
      proxy.queryRecursive(query, header);
      log.info("[ConnectionActor] Ack recieved, providerNSA = {}, correlationId = {}", header.value.getProviderNSA(), header.value.getCorrelationId());
    } catch (org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException ex) {
      log.error("[ConnectionActor] queryRecursive exception - {} {}", ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      log.error(JaxbParser.jaxb2String(ServiceExceptionType.class, ex.getFaultInfo()));
      throw ex;
    }
  }
}
