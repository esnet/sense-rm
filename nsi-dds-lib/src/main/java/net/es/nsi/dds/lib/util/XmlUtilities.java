package net.es.nsi.dds.lib.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * This {@link XmlUtilities} is a utility class providing tools for the manipulation of JAXB generated XML objects.
 *
 * @author hacksaw
 */
public class XmlUtilities {

  public final static long ONE_YEAR = 31536000000L;
  public final static long ONE_DAY = 86400000L;

  /**
   * Utility method to marshal a JAXB annotated java object to an XML formatted string. This class is generic enough to
   * be used for any JAXB annotated java object not containing the {@link XmlRootElement} annotation.
   *
   * @param xmlClass	The JAXB class of the object to marshal.
   * @param xmlObject	The JAXB object to marshal.
   * @return	String containing the XML encoded object.
   */
  public static String jaxbToString(Class<?> xmlClass, Object xmlObject) {

    // Make sure we are given the correct input.
    if (xmlClass == null || xmlObject == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    JAXBElement<?> jaxbElement = new JAXBElement(new QName("uri", "local"), xmlClass, xmlObject);

    return jaxbToString(xmlClass, jaxbElement);
  }

  public static String jaxbToString(Class<?> xmlClass, JAXBElement<?> jaxbElement) {
    return jaxbToString(false, xmlClass, jaxbElement);
  }

  public static String prettyPrint(Class<?> xmlClass, Object xmlObject) {

    // Make sure we are given the correct input.
    if (xmlClass == null || xmlObject == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    JAXBElement<?> jaxbElement = new JAXBElement(new QName("uri", "local"), xmlClass, xmlObject);

    return jaxbToString(true, xmlClass, jaxbElement);
  }

  public static String prettyPrint(Class<?> xmlClass, JAXBElement<?> jaxbElement) {
    return jaxbToString(true, xmlClass, jaxbElement);
  }

  public static String jaxbToString(boolean pretty, Class<?> xmlClass, JAXBElement<?> jaxbElement) {
    // Make sure we are given the correct input.
    if (xmlClass == null || jaxbElement == null) {
      return null;
    }

    // We will write the XML encoding into a string.
    StringWriter writer = new StringWriter();
    String result;
    try {
      // We will use JAXB to marshal the java objects.
      final JAXBContext jaxbContext = JAXBContext.newInstance(xmlClass);

      // Marshal the object.
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, pretty);
      jaxbMarshaller.marshal(jaxbElement, writer);
      result = writer.toString();
    } catch (JAXBException e) {
      // Something went wrong so get out of here.
      return null;
    } finally {
      try {
        writer.close();
      } catch (IOException ex) {
      }
    }

    // Return the XML string.
    return result;
  }

  public static <T> T xmlToJaxb(Class<T> xmlClass, String xml) throws JAXBException {
    JAXBContext jaxbContext = JAXBContext.newInstance(xmlClass);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    JAXBElement<?> element;
    try (StringReader reader = new StringReader(xml)) {
      element = (JAXBElement<?>) unmarshaller.unmarshal(reader);
    }

    return xmlClass.cast(element.getValue());
  }

  public static <T> T xmlToJaxb(Class<T> xmlClass, InputStream is) throws JAXBException, IOException {
    JAXBContext jaxbContext = JAXBContext.newInstance(xmlClass);
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    JAXBElement<?> element = (JAXBElement<?>) unmarshaller.unmarshal(is);
    is.close();

    return xmlClass.cast(element.getValue());
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

  public static Collection<String> getXmlFilenames(String path) throws NullPointerException {
    Collection<String> results = new ArrayList<>();
    File folder = new File(path);

    // We will grab all XML files from the target directory.
    File[] listOfFiles = folder.listFiles();
    if (listOfFiles != null) {
      String file;
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
          file = listOfFiles[i].getAbsolutePath();
          if (file.endsWith(".xml") || file.endsWith(".xml")) {
            results.add(file);
          }
        }
      }
    }
    return new CopyOnWriteArrayList(results);
  }
}
