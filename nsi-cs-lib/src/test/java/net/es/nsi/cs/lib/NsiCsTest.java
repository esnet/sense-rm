package net.es.nsi.cs.lib;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class NsiCsTest {

  @LocalServerPort
  private int port;

  private static ObjectFactory FACTORY = new ObjectFactory();

  @Before
  public void setUp() {

  }

  @Test
  public void test() {

  }

/**
  @Test
  public void testReserveRequest() throws org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException, InterruptedException {
    Configuration config = new Configuration();
    ConnectionServiceRequester requester = new ConnectionServiceRequester();
    ConnectionRequesterPort proxy = requester.getConnectionServiceRequesterPort();
    BindingProvider bp = (BindingProvider) proxy;
    Map<String, Object> context = bp.getRequestContext();
    context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://localhost:" + port + "/nsi/ConnectionServiceRequester");
    context.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

    CommonHeaderType reserveHeader = NsiHeader.builder()
            .correlationId(Helper.getUUID())
            .providerNSA("urn:ogf:network:es.net:2013:nsa:nsi-aggr-west")
            .requesterNSA("urn:ogf:network:es.net:2013:nsa:sense-rm")
            .replyTo("http://localhost:" + port + "/nsi/ConnectionServiceRequester")
            .build()
            .getRequestHeaderType();

    Holder<CommonHeaderType> header = new Holder<>();
    header.value = reserveHeader;

    ReservationConfirmCriteriaType criteria = FACTORY.createReservationConfirmCriteriaType();
    criteria.setVersion(0);
    criteria.setServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");

    ReserveConfirmedType reserveConfirmed = FACTORY.createReserveConfirmedType();
    reserveConfirmed.setConnectionId(Helper.getUUID());
    reserveConfirmed.setGlobalReservationId(Helper.getGlobalReservationId());
    reserveConfirmed.setDescription("You smell like poop");
    reserveConfirmed.setCriteria(criteria);

    try {
      log.info("[NsiCsTest] Sending reserveConfirmed, correlationId = {}", reserveHeader.getCorrelationId());
      proxy.reserveConfirmed(reserveConfirmed, header);
      log.info("[NsiCsTest] Ack recieved, provider NSA = {}, correlationId = {}", header.value.getProviderNSA(), header.value.getCorrelationId());
    } catch (org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException ex) {
      log.error("[NsiCsTest] reserveConfirmed exception - " + ex.getFaultInfo().getErrorId() + " " + ex.getFaultInfo().getText());
      log.error(JaxbParser.jaxb2String(ServiceExceptionType.class, ex.getFaultInfo()));
      throw ex;
    }

    Thread.sleep(10000);
  }

  @Test
  public void testQueryRecursiveRequest() throws org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException, InterruptedException {
    Configuration config = new Configuration();
    ConnectionServiceProvider provider = new ConnectionServiceProvider();
    ConnectionProviderPort proxy = provider.getConnectionServiceProviderPort();
    BindingProvider bp = (BindingProvider) proxy;
    Map<String, Object> context = bp.getRequestContext();
    context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://localhost:9000/nsi-v2/ConnectionServiceProvider");
    context.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

    CommonHeaderType requestHeader = NsiHeader.builder()
            .correlationId(Helper.getUUID())
            .providerNSA("urn:ogf:network:opennsa.net:2015:nsa:safnari")
            .requesterNSA("urn:ogf:network:es.net:2013:nsa:sense-rm")
            .replyTo("http://localhost:" + port + "/nsi/ConnectionServiceRequester")
            .build()
            .getRequestHeaderType();

    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;

    QueryType query = FACTORY.createQueryType();
    //query.getConnectionId().add("a1090b94-aed2-44dc-b9ce-36975ed9fa89");

    try {
      log.info("[NsiCsTest] Sending queryRecursive: correlationId = {}", requestHeader.getCorrelationId());
      proxy.queryRecursive(query, header);
      log.info("Ack recieved, provider NSA = {}, correlationId = {}", header.value.getProviderNSA(), header.value.getCorrelationId());
    } catch (org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException ex) {
      log.error("[NsiCsTest] reserveConfirmed exception - " + ex.getFaultInfo().getErrorId() + " " + ex.getFaultInfo().getText());
      log.error(JaxbParser.jaxb2String(ServiceExceptionType.class, ex.getFaultInfo()));
      throw ex;
    }

    Thread.sleep(10000);
  }

  @Test
  public void testQuerySummaryRequest() throws org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException, InterruptedException {
    Configuration config = new Configuration();
    ConnectionServiceProvider provider = new ConnectionServiceProvider();
    ConnectionProviderPort proxy = provider.getConnectionServiceProviderPort();
    BindingProvider bp = (BindingProvider) proxy;
    Map<String, Object> context = bp.getRequestContext();
    context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://localhost:9000/nsi-v2/ConnectionServiceProvider");
    context.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

    CommonHeaderType requestHeader = NsiHeader.builder()
            .correlationId(Helper.getUUID())
            .providerNSA("urn:ogf:network:opennsa.net:2015:nsa:safnari")
            .requesterNSA("urn:ogf:network:es.net:2013:nsa:sense-rm")
            .replyTo("http://localhost:" + port + "/nsi/ConnectionServiceRequester")
            .build()
            .getRequestHeaderType();

    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;

    QueryType query = FACTORY.createQueryType();
    //query.getConnectionId().add("a1090b94-aed2-44dc-b9ce-36975ed9fa89");

    try {
      log.info("[NsiCsTest] Sending querySummary: correlationId = {}", requestHeader.getCorrelationId());
      proxy.querySummary(query, header);
      log.info("Ack recieved, provider NSA = {}, correlationId = {}", header.value.getProviderNSA(), header.value.getCorrelationId());
    } catch (org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException ex) {
      log.error("[NsiCsTest] reserveConfirmed exception - " + ex.getFaultInfo().getErrorId() + " " + ex.getFaultInfo().getText());
      log.error(JaxbParser.jaxb2String(ServiceExceptionType.class, ex.getFaultInfo()));
      throw ex;
    }

    Thread.sleep(10000);
  }
  * **/
}
