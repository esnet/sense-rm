package net.es.nsi.common.util;

import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 * @author hacksaw
 */
public class XmlDate {

  public static XMLGregorianCalendar longToXMLGregorianCalendar(long time) throws DatatypeConfigurationException {
    if (time < 0) {
      throw new DatatypeConfigurationException("Illegal time value specified " + time);
    }

    GregorianCalendar cal = new GregorianCalendar();
    cal.setTimeInMillis(time);
    XMLGregorianCalendar newXMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    return newXMLGregorianCalendar;
  }

  public static XMLGregorianCalendar xmlGregorianCalendar() throws DatatypeConfigurationException {
    GregorianCalendar cal = new GregorianCalendar();
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
  }

  public static XMLGregorianCalendar xmlGregorianCalendar(Date date) throws DatatypeConfigurationException {
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(date);
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
  }

  public static XMLGregorianCalendar xmlGregorianCalendar(String date) throws DatatypeConfigurationException {
    DateTime dt = ISODateTimeFormat.dateTimeParser().parseDateTime(date);
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(dt.toDate());
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
  }

  public static Date xmlGregorianCalendarToDate(XMLGregorianCalendar cal) throws DatatypeConfigurationException {
    GregorianCalendar gregorianCalendar = cal.toGregorianCalendar();
    return gregorianCalendar.getTime();
  }
}
