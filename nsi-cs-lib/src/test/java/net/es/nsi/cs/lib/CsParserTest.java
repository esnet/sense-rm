package net.es.nsi.cs.lib;

import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.common.util.XmlUtilities;
import org.junit.Test;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationRequestCriteriaType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveType;
import org.ogf.schemas.nsi._2013._12.connection.types.ScheduleType;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;
import org.ogf.schemas.nsi._2013._12.services.types.DirectionalityType;
import org.ogf.schemas.nsi._2013._12.services.types.TypeValueType;

import javax.xml.datatype.DatatypeConfigurationException;

@Slf4j
public class CsParserTest {
  private static final org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory CS_FACTORY
      = new org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory();
  private static final org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory P2PS_FACTORY
      = new org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory();

  @Test
  public void testP2PSDecoding() throws DatatypeConfigurationException, JAXBException {
    // Create and populate a P2PS service structure for the NSI-CS request.
    P2PServiceBaseType p2ps = P2PS_FACTORY.createP2PServiceBaseType();
    p2ps.setCapacity(500L);
    p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
    p2ps.setSymmetricPath(Boolean.TRUE);
    p2ps.setSourceSTP("urn:ogf:network:es.net:2013::denv-cr6:1_1_c25_1:+?vlan=3693");
    p2ps.setDestSTP("urn:ogf:network:es.net:2013::eqxld8-cr6:2_1_c1_1:+?vlan=1786");

    // Base the reservation off of the specified existsDuring criteria.
    ScheduleType sch = CS_FACTORY.createScheduleType();
    sch.setStartTime(CS_FACTORY.createScheduleTypeStartTime(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis())));
    sch.setEndTime(CS_FACTORY.createScheduleTypeEndTime(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 10000)));

    ReservationRequestCriteriaType rrc = CS_FACTORY.createReservationRequestCriteriaType();
    rrc.setVersion(0);
    rrc.setSchedule(sch);
    rrc.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
    rrc.getAny().add(P2PS_FACTORY.createP2Ps(p2ps));

    ReserveType r = CS_FACTORY.createReserveType();
    r.setConnectionId("12345");
    r.setGlobalReservationId("global-1");
    r.setDescription("This is a description.");
    r.setCriteria(rrc);

    String encoded = CsParser.getInstance().reserve2xml(r);
    log.debug("[testP2PSDecoding] encoded reserve = \n{}", encoded);

    ReserveType decoded = CsParser.getInstance().xml2reserve(encoded);
    log.debug("[testP2PSDecoding] decoded reserve = {}", decoded.getGlobalReservationId());
    P2PServiceBaseType service = CsParser.getInstance().getP2PS(decoded.getCriteria().getAny());
    log.debug("[testP2PSDecoding] decoded reserve P2Ps capacity = {}", service.getCapacity());
  }

  @Test
  public void testCapacityDecoding() throws DatatypeConfigurationException, JAXBException {
    // Create and populate a P2PS service structure for the NSI-CS request.
    P2PServiceBaseType p2ps = P2PS_FACTORY.createP2PServiceBaseType();
    p2ps.setCapacity(500L);
    p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
    p2ps.setSymmetricPath(Boolean.TRUE);
    p2ps.setSourceSTP("urn:ogf:network:es.net:2013::denv-cr6:1_1_c25_1:+?vlan=3693");
    p2ps.setDestSTP("urn:ogf:network:es.net:2013::eqxld8-cr6:2_1_c1_1:+?vlan=1786");

    // Base the reservation off of the specified existsDuring criteria.
    ScheduleType sch = CS_FACTORY.createScheduleType();
    sch.setStartTime(CS_FACTORY.createScheduleTypeStartTime(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis())));
    sch.setEndTime(CS_FACTORY.createScheduleTypeEndTime(XmlUtilities.longToXMLGregorianCalendar(System.currentTimeMillis() + 10000)));

    ReservationRequestCriteriaType rrc = CS_FACTORY.createReservationRequestCriteriaType();
    rrc.setVersion(0);
    rrc.setSchedule(sch);
    rrc.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
    rrc.getAny().add(P2PS_FACTORY.createCapacity(1000L));
    TypeValueType disruption = new TypeValueType();
    disruption.setType("disruption-hitless");
    disruption.setValue("true");
    rrc.getAny().add(P2PS_FACTORY.createParameter(disruption));

    ReserveType r = CS_FACTORY.createReserveType();
    r.setConnectionId("12345");
    r.setGlobalReservationId("global-1");
    r.setDescription("This is a description.");
    r.setCriteria(rrc);

    String encoded = CsParser.getInstance().reserve2xml(r);
    log.debug("[testCapacityDecoding] encoded reserve = \n{}", encoded);

    ReserveType decoded = CsParser.getInstance().xml2reserve(encoded);
    log.debug("[testCapacityDecoding] decoded reserve = {}", decoded.getGlobalReservationId());

    Long capacity = CsParser.getInstance().getCapacity(decoded.getCriteria().getAny());
    log.debug("[testCapacityDecoding] decoded reserve P2Ps capacity = {}", capacity);

    boolean hitless = CsParser.getInstance().getHitless(decoded.getCriteria().getAny());
    log.debug("[testCapacityDecoding] decoded reserve P2Ps hitless = {}", hitless);
  }
}
