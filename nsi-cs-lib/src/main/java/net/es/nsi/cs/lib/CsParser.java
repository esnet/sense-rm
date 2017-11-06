package net.es.nsi.cs.lib;

import javax.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.JaxbParser;
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
}
