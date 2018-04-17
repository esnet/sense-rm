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
    this.start = null;
    this.end = XmlUtilities.xmlGregorianCalendar();
    this.end.setYear(this.end.getYear() + 1);
  }

  public String getId() {
    return id;
  }

  public XMLGregorianCalendar getStart() {
    return start;
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
