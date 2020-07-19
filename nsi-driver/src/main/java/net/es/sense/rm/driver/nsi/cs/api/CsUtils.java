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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CsUtils {

  public static boolean serializeP2PS(String serviceType, List<Object> any,
          Reservation reservation) throws JAXBException {

    // Indicate in the return if we found a P2P structure.
    boolean found = false;

    if (Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(serviceType) ||
            Nsi.NSI_SERVICETYPE_EVTS_OSCARS.equalsIgnoreCase(serviceType) ||
            Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_1.equalsIgnoreCase(serviceType) ||
            Nsi.NSI_SERVICETYPE_EVTS_OPENNSA_2.equalsIgnoreCase(serviceType)) {

      // Normalize to this serviceType.
      reservation.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);

      // Look for the associated P2P service element.
      for (Object object : any) {
        if (object instanceof JAXBElement) {
          JAXBElement jaxb = (JAXBElement) object;
          if (jaxb.getValue() instanceof P2PServiceBaseType) {
            // Found the P2PS service entry.
            P2PServiceBaseType p2ps = (P2PServiceBaseType) jaxb.getValue();

            // Determine if this is a single domain connection.
            SimpleStp srcStp = new SimpleStp(p2ps.getSourceSTP());
            SimpleStp dstStp = new SimpleStp(p2ps.getDestSTP());
            if (!srcStp.getNetworkId().equalsIgnoreCase(dstStp.getNetworkId())) {
              log.error("[serializeP2PS]: source and destination networkId for STP do not match: {} != {}", srcStp, dstStp);
            }

            // Set the topology based on the source STP for now.  We can reject
            // later if this is not the networkId we are looking for.
            reservation.setTopologyId(srcStp.getNetworkId());
            reservation.setService(CsParser.getInstance().p2ps2xml(p2ps));
            found = true;
            break;
          }
        } else if (object instanceof org.w3c.dom.Element) {
          org.w3c.dom.Element element = (org.w3c.dom.Element) object;
          if ("p2ps".equalsIgnoreCase(element.getLocalName())) {
            // Found the P2PS service entry.
            P2PServiceBaseType p2ps = CsParser.getInstance().node2p2ps((Node) element);

            // Determine if this is a single domain connection.
            SimpleStp srcStp = new SimpleStp(p2ps.getSourceSTP());
            SimpleStp dstStp = new SimpleStp(p2ps.getDestSTP());
            if (!srcStp.getNetworkId().equalsIgnoreCase(dstStp.getNetworkId())) {
              log.error("[serializeP2PS]: source and destination networkId for STP do not match: {} != {}", srcStp, dstStp);
            }

            // Set the topology based on the source STP for now.  We can reject
            // later if this is not the networkId we are looking for.
            reservation.setTopologyId(srcStp.getNetworkId());
            reservation.setService(CsParser.getInstance().p2ps2xml(p2ps));
            found = true;
            break;
          } 
        } else {
          log.debug("[serializeP2PS] ignoring element = {}", object.getClass().getName());
        }
      }
    }

    return found;
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
