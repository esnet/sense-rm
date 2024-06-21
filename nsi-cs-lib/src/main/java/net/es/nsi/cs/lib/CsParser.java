package net.es.nsi.cs.lib;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.JaxbParser;
import org.ogf.schemas.nsi._2013._12.connection.types.*;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;
import org.ogf.schemas.nsi._2013._12.services.types.TypeValueType;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.List;

/**
 *
 * @author hacksaw
 */
public class CsParser extends JaxbParser {

  private final static String PACKAGES =
      "org.ogf.schemas.nsi._2013._12.connection.types:" +
      "org.ogf.schemas.nsi._2013._12.services.point2point:" +
      "org.ogf.schemas.nsi._2013._12.services.types:" +
      "oasis.names.tc.saml._2_0.assertion:" +
      "org.ogf.schemas.nsi._2013._12.framework.headers:" +
      "org.ogf.schemas.nsi._2013._12.framework.types:" +
      "org.w3._2000._09.xmldsig_:" +
      "org.w3._2001._04.xmlenc_";

  private final static org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory P2P_FACTORY =
      new org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory();

  private final static org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory TYPES_FACTORY =
          new org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory();

  private CsParser() {
    super(PACKAGES);
  }

  private static class ParserHolder {
    public static final CsParser INSTANCE = new CsParser();
  }

  public static CsParser getInstance() {
    return ParserHolder.INSTANCE;
  }

  public P2PServiceBaseType node2p2ps(Node node) throws JAXBException {
    return (P2PServiceBaseType) this.node2Jaxb(node).getValue();
  }

  public String p2ps2xml(P2PServiceBaseType p2ps) throws JAXBException {
    return this.jaxb2Xml(P2P_FACTORY.createP2Ps(p2ps));
  }

  public P2PServiceBaseType xml2p2ps(String xml) throws JAXBException {
    return this.xml2Jaxb(P2PServiceBaseType.class, xml);
  }

  public String reserve2xml(ReserveType r) throws JAXBException {
    return this.jaxb2Xml(TYPES_FACTORY.createReserve(r));
  }

  public ReserveType xml2reserve(String xml) throws JAXBException {
    return this.xml2Jaxb(ReserveType.class, xml);
  }

  // querySummaryConfirmed
  public String querySummaryConfirmed2xml(QuerySummaryConfirmedType value) throws JAXBException {
    JAXBElement<QuerySummaryConfirmedType> jaxb = TYPES_FACTORY.createQuerySummaryConfirmed(value);
    return this.jaxb2Xml(jaxb);
  }

  // QuerySummaryResultType
  private final static QName _QuerySummaryResultType_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/connection/types", "QuerySummaryResultType");
  public String qsrt2xml(QuerySummaryResultType query) throws JAXBException {
    JAXBElement<QuerySummaryResultType> jaxb = new JAXBElement<QuerySummaryResultType>(_QuerySummaryResultType_QNAME, QuerySummaryResultType.class, null, query);
    return this.jaxb2Xml(jaxb);
  }

  public String errorEvent2xml(ErrorEventType error) throws JAXBException {
    JAXBElement<ErrorEventType> jaxb = TYPES_FACTORY.createErrorEvent(error);
    return this.jaxb2Xml(jaxb);
  }

  public String genericError2xml(GenericErrorType error) throws JAXBException {
    JAXBElement<GenericErrorType> jaxb = TYPES_FACTORY.createError(error);
    return this.jaxb2Xml(jaxb);
  }

  public String reserveTimeoutRequest2xml(ReserveTimeoutRequestType value) throws JAXBException {
    JAXBElement<ReserveTimeoutRequestType> jaxb = TYPES_FACTORY.createReserveTimeout(value);
    return this.jaxb2Xml(jaxb);
  }

  public String dataPlaneStateChange2xml(DataPlaneStateChangeRequestType value) throws JAXBException {
    JAXBElement<DataPlaneStateChangeRequestType> jaxb = TYPES_FACTORY.createDataPlaneStateChange(value);
    return this.jaxb2Xml(jaxb);
  }

  public String reserveConfirmedType2xml(ReserveConfirmedType value) throws JAXBException {
    JAXBElement<ReserveConfirmedType> jaxb = TYPES_FACTORY.createReserveConfirmed(value);
    return this.jaxb2Xml(jaxb);
  }

  public P2PServiceBaseType getP2PS(List<Object> anyList) throws JAXBException {
    for (Object any : anyList) {
      if (any instanceof JAXBElement) {
        JAXBElement jaxb = (JAXBElement) any;
        if (jaxb.getDeclaredType() == P2PServiceBaseType.class) {
          // Get the network identifier from and STP
          return (P2PServiceBaseType) jaxb.getValue();
        }
      } else if (any instanceof org.w3c.dom.Element) {
        org.w3c.dom.Element element = (org.w3c.dom.Element) any;
        if ("p2ps".equalsIgnoreCase(element.getLocalName())) {
          return CsParser.getInstance().node2p2ps((Node) element);
        }
      }
    }
    return null;
  }

  private final static QName _Capacity_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/services/point2point", "capacity");
  public Long getCapacity(List<Object> anyList) throws JAXBException {
    return anyList.stream()
        .filter((object) -> (object instanceof JAXBElement))
        .map((object) -> (JAXBElement) object)
        .filter((jaxb) -> (jaxb.getName().equals(_Capacity_QNAME)))
        .map((jaxb) -> (Long) jaxb.getValue())
        .findFirst()
        .orElse(null);
  }

  public boolean getHitless(List<Object> anyList) throws JAXBException {
    return anyList.stream()
        .filter((object) -> (object instanceof JAXBElement))
        .map((object) -> (JAXBElement) object)
        .filter((jaxb) -> (jaxb.getValue() instanceof TypeValueType))
        .map((jaxb) -> (TypeValueType) jaxb.getValue())
        .filter(tvt -> "disruption-hitless".equalsIgnoreCase(tvt.getType()))
        .map(TypeValueType::getValue)
        .map(Boolean::parseBoolean)
        .findFirst()
        .orElse(false);
  }
}
