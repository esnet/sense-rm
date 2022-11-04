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
package net.es.sense.rm.driver.nsi.mrml;

import com.google.common.base.Strings;
import java.util.Date;
import java.util.Optional;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.common.util.XmlDate;
import net.es.nsi.common.util.XmlUtilities;
import net.es.sense.rm.driver.schema.Nml;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 *
 * @author hacksaw
 */
public class NmlExistsDuring {

  private final String id;
  private final XMLGregorianCalendar start;
  private final XMLGregorianCalendar end;

  public NmlExistsDuring(Resource service) throws DatatypeConfigurationException {
    this.id = service.getURI();
    this.start = getTime(service, Nml.start);
    this.end = getTime(service, Nml.end);
  }

  public NmlExistsDuring(String id) throws DatatypeConfigurationException {
    this.id = id;
    this.start = getPaddedStart();
    this.end = XmlUtilities.xmlGregorianCalendar();
    this.end.setYear(this.end.getYear() + 1);
  }

  public String getId() {
    return id;
  }

  public XMLGregorianCalendar getStart() {
    return start;
  }

  /**
   * OpenNSA will not accept a starTime in the past so we want to give
   * ourselves a 60 second padding for a startTime in the future to
   * account for delay in propagation.  If the specified startTime is
   * in the past, or within 60 seconds from now, we return a startTime
   * of now just to be safe.
   *
   * Update: we also need to return a time 60 seconds in the future if
   * a startTime is not present to help with a new OpenNSA bug.
   *
   * @return
   * @throws DatatypeConfigurationException
   */
  public XMLGregorianCalendar getPaddedStart() throws DatatypeConfigurationException {
    // We want pad the current date with 60 seconds before comparing.
    if (start != null) {
      Date now = new Date(System.currentTimeMillis() + 1000 * 60);
      if (now.before(XmlUtilities.xmlGregorianCalendarToDate(start))) {
        return start;
      }

      return XmlUtilities.xmlGregorianCalendar(now);
    }

    return null;
  }

  public XMLGregorianCalendar getEnd() {
    return end;
  }

  private XMLGregorianCalendar getTime(Resource service, DatatypeProperty property)
          throws DatatypeConfigurationException {
    Optional<Statement> s = Optional.ofNullable(service.getProperty(property));
    if (s.isPresent() && !Strings.isNullOrEmpty(s.get().getString())) {
      return XmlDate.xmlGregorianCalendar(s.get().getString());
    }

    return null;
  }

  public static String id(String urn) {
    return urn + ":existsDuring";
  }
}
