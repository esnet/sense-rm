/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.cs.lib;


import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

@Slf4j
public class Helper {

	/**
	 * Utility method to marshal a JAXB annotated java object to an XML string.
	 *
	 * @param messageClass	The class of the object to marshal.
	 * @param message		The object to marshal.
	 * @return				String containing the XML encoded object.
	 */
	public static String dump(Class<?> messageClass, Object message) {

		// Make sure we are given the correct input.
		if (messageClass == null || message == null) {
			return null;
		}

		// We will write the XML encoding into a string.
		StringWriter writer = new StringWriter();

		try {
			// We will use JAXB to marshal the java objects.
			final JAXBContext jaxbContext = JAXBContext.newInstance(messageClass);

			// We do not have @XmlRootElement annotations on the classes so
			// we need to manually create the JAXBElement.
			JAXBElement<?> element = new JAXBElement(new QName("uri", "local"), messageClass, message);

			// Marshal the object.
			jaxbContext.createMarshaller().marshal(element, writer);
		} catch (JAXBException e) {
			// Something went wrong so get out of here.
			log.error("MessageDump.dump: Error marshalling object " +
					messageClass.getName() + ": " + e.toString());
			return null;
		}

		// Return the XML string.
		return writer.toString();
	}

    private final static String URN_UUID = "urn:uuid:";

    public static String getUUID() {
        return URN_UUID + UUID.randomUUID().toString();
    }

    private final static String URN_SERVICE = "urn:ogf:network:service:";

    public static String getGlobalReservationId() {
        return URN_SERVICE + UUID.randomUUID().toString();
    }

    /**
     * Computes the day that has the given offset in days to today
     * and returns it as an instance of <code>Calendar</code>.
     *
     * @param offsetDays   the offset in day relative to today
     * @return a <code>Calendar</code> instance that is the begin of the day
     *     with the specified offset
     */
    public static GregorianCalendar getRelativeCalendar(int offsetDays) {
        GregorianCalendar today = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        return getRelativeCalendar(today, offsetDays);
    }

    /**
     * Computes the day that has the given offset in days from the specified
     * <em>from</em> date and returns it as an instance of <code>Calendar</code>.
     *
     * @param from         the base date as <code>Calendar</code> instance
     * @param offsetDays   the offset in day relative to today
     * @return a <code>Calendar</code> instance that is the begin of the day
     *     with the specified offset from the given day
     */
    public static GregorianCalendar getRelativeCalendar(Calendar from, int offsetDays) {
        GregorianCalendar temp =
            new GregorianCalendar(
                from.get(Calendar.YEAR),
                from.get(Calendar.MONTH),
                from.get(Calendar.DATE),
                0,
                0,
                0);
        temp.add(Calendar.DATE, offsetDays);
        return temp;
    }

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