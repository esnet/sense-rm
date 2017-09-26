package net.es.nsi.dds.lib.util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class XmlUtilitiesTest {

  @Test
  public void dateTime() throws DatatypeConfigurationException {
    XMLGregorianCalendar a = XmlUtilities.xmlGregorianCalendar("2017-09-06T10:15:13.096Z");
    XMLGregorianCalendar b = XmlUtilities.xmlGregorianCalendar("2017-09-06T10:15:13.096+00:00");
    Assert.assertTrue(a.compare(b) == DatatypeConstants.EQUAL);

    a = XmlUtilities.xmlGregorianCalendar("2017-06-19T12:54:16+02:00");
    b = XmlUtilities.xmlGregorianCalendar("2017-06-19T12:54:16.000+02:00");
    Assert.assertTrue(a.compare(b) == DatatypeConstants.EQUAL);

    a = XmlUtilities.xmlGregorianCalendar("2017-06-19T12:54:16+02:00");
    b = XmlUtilities.xmlGregorianCalendar("2017-06-19T10:54:16+00:00");
    Assert.assertTrue(a.compare(b) == DatatypeConstants.EQUAL);
  }
}
