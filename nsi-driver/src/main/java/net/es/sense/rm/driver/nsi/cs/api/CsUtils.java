/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.sense.rm.driver.nsi.cs.api;

import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.cs.lib.CsParser;
import net.es.nsi.cs.lib.SimpleStp;
import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;
import org.w3c.dom.Node;

/**
 *
 * @author hacksaw
 */
public class CsUtils {

  public static void serializeP2PS(String serviceType, List<Object> any, Reservation reservation) throws JAXBException {
    if (Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(serviceType) ||
            Nsi.NSI_SERVICETYPE_EVTS_OSCARS.equalsIgnoreCase(serviceType) ||
            Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_1.equalsIgnoreCase(serviceType) ||
            Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_2.equalsIgnoreCase(serviceType)) {
      reservation.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
      for (Object object : any) {
        if (object instanceof JAXBElement) {
          JAXBElement jaxb = (JAXBElement) object;
          if (jaxb.getValue() instanceof P2PServiceBaseType) {
            P2PServiceBaseType p2ps = (P2PServiceBaseType) jaxb.getValue();
            SimpleStp stp = new SimpleStp(p2ps.getSourceSTP());
            reservation.setTopologyId(stp.getNetworkId());
            reservation.setService(CsParser.getInstance().p2ps2xml(p2ps));
            break;
          }
        } else if (object instanceof org.apache.xerces.dom.ElementNSImpl) {
          org.apache.xerces.dom.ElementNSImpl element = (org.apache.xerces.dom.ElementNSImpl) object;
          if ("p2ps".equalsIgnoreCase(element.getLocalName())) {
            P2PServiceBaseType p2ps = CsParser.getInstance().node2p2ps((Node) element);
            SimpleStp stp = new SimpleStp(p2ps.getSourceSTP());
            reservation.setTopologyId(stp.getNetworkId());
            reservation.setService(CsParser.getInstance().p2ps2xml(p2ps));
            break;
          }
        }
      }
    }
  }

  public static long getStartTime(JAXBElement<XMLGregorianCalendar> time) {
    if (time == null || time.getValue() == null) {
      return 0;
    }

    return time.getValue().toGregorianCalendar().getTimeInMillis();
  }

  public static long getEndTime(JAXBElement<XMLGregorianCalendar> time) {
    if (time == null || time.getValue() == null) {
      return Long.MAX_VALUE;
    }

    return time.getValue().toGregorianCalendar().getTimeInMillis();
  }
}
