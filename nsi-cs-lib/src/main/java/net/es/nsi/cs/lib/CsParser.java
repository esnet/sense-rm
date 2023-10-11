package net.es.nsi.cs.lib;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import net.es.nsi.common.jaxb.JaxbParser;
import org.ogf.schemas.nsi._2013._12.connection.types.DataPlaneStateChangeRequestType;
import org.ogf.schemas.nsi._2013._12.connection.types.ErrorEventType;
import org.ogf.schemas.nsi._2013._12.connection.types.GenericErrorType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryResultType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveTimeoutRequestType;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;
import org.w3c.dom.Node;

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

  public P2PServiceBaseType xml2p2ps(String xml) throws JAXBException {
    return this.xml2Jaxb(P2PServiceBaseType.class, xml);
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
}
