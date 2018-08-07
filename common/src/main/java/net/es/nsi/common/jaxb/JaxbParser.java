package net.es.nsi.common.jaxb;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A singleton to load the very expensive JAXBContext once.
 *
 * @author hacksaw
 */
@Slf4j
public class JaxbParser {
  private JAXBContext jc;

  public JaxbParser(String packages) {
    try {
      // Load a JAXB context.
      jc = JAXBContext.newInstance(packages);
    } catch (JAXBException jaxb) {
      log.error("JaxbParser: Failed to load JAXB instance for packages " + packages, jaxb);
    }
  }

  private Marshaller marshaller() throws JAXBException {
    Marshaller marshaller = jc.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    return marshaller;
  }

  private Unmarshaller unmarshaller() throws JAXBException {
    return jc.createUnmarshaller();
  }

  /**
   * Parse the given file into the specified JAXB object.
   *
   * @param <T> Target type of the JAXB object.
   * @param xmlClass Target class of the JAXB object.
   * @param file The file containing the XML document.
   * @return The target JAXB object.
   * @throws JAXBException Could not parse the specified XML document.
   * @throws IOException Could not open the specified file.
   */
  public <T extends Object> T parseFile(Class<T> xmlClass, String file)
          throws JAXBException, IOException {
    // Parse the specified file.
    try (FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
      return xml2Jaxb(xmlClass, bufferedInputStream);
    }
  }

  /**
   * Write the JAXB object to the specified file.
   *
   * @param jaxbElement The JAXB element to write to file.
   * @param file The filename of the target file.
   * @throws JAXBException Could not convert the JAXB object to an XML document.
   * @throws IOException The specified file could not be created.
   */
  public void writeFile(JAXBElement<?> jaxbElement, String file) throws JAXBException, IOException {
    File fd = new File(file);
    if (!fd.exists()) {
      log.debug("Creating file " + fd.getAbsolutePath());
      FileUtils.touch(fd);
    }

    // Parse the specified file.
    try (FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream =
                    new BufferedOutputStream(fileOutputStream)) {
      marshaller().marshal(jaxbElement, fileOutputStream);
    }
  }

  /**
   * Convert the specified JAXB object to a DOM object.
   *
   * @param jaxbElement JAXB object to convert.
   * @return The converted DOM object.
   * @throws JAXBException JAXB object could not be converted to DOM object.
   * @throws ParserConfigurationException Could not create a DOM document builder.
   */
  public Document jaxb2Dom(JAXBElement<?> jaxbElement) throws JAXBException, ParserConfigurationException {
    // Convert JAXB representation to DOM.
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.newDocument();
    marshaller().marshal(jaxbElement, doc);
    return doc;
  }

  /**
   * Covert specified DOM object to JAXB.
   *
   * @param doc The DOM object to convert.
   * @return The converted JAXB object.
   * @throws JAXBException JAXBException JAXB object could not be created.
   */
  public JAXBElement<?> dom2Jaxb(Document doc) throws JAXBException {
    return (JAXBElement<?>) unmarshaller().unmarshal(doc);
  }

  public JAXBElement<?> node2Jaxb(Node node) throws JAXBException {
    return (JAXBElement<?>) unmarshaller().unmarshal(node);
  }

  /**
   * Convert the specified JAXB object to a string.
   *
   * @param jaxbElement JAXB object to convert.
   * @return A serialized XML string.
   * @throws JAXBException JAXB object could not be created.
   */
  public String jaxb2Xml(JAXBElement<?> jaxbElement) throws JAXBException {
    // We will write the XML encoding into a string.
    StringWriter writer = new StringWriter();
    String result;
    try {
      marshaller().marshal(jaxbElement, writer);
      result = writer.toString();
    } catch (JAXBException ex) {
      // Something went wrong so get out of here.
      log.error("Failed to serialize JAXB structure.", ex);
      throw ex;
    } finally {
      try {
        writer.close();
      } catch (IOException ex) {
      }
    }

    // Return the XML string.
    return result;
  }

  public String jaxb2XmlFormatter(JAXBElement<?> jaxbElement) throws JAXBException {
    // We will write the XML encoding into a string.
    StringWriter writer = new StringWriter();
    String result;
    try {
      Marshaller marshaller = marshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshaller.marshal(jaxbElement, writer);
      result = writer.toString();
    } catch (JAXBException ex) {
      // Something went wrong so get out of here.
      log.error("Failed to serialize JAXB structure.", ex);
      throw ex;
    } finally {
      try {
        writer.close();
      } catch (IOException ex) {
      }
    }

    // Return the XML string.
    return result;
  }

  /**
   * Convert the specified string representation of a XML document to a JAXB object.
   *
   * @param <T> Target type of the JAXB object.
   * @param xmlClass The target class of the JAXB object.
   * @param xml The string containing the XML document to convert.
   * @return JAXB object representing the XML document.
   * @throws JAXBException Could not parse the specified XML document.
   */
  public <T extends Object> T xml2Jaxb(Class<T> xmlClass, String xml) throws JAXBException, IllegalArgumentException {
    Optional<JAXBElement<T>> element;
    try (StringReader reader = new StringReader(xml)) {
      element = Optional.ofNullable((JAXBElement<T>) unmarshaller().unmarshal(reader));
    }

    if (!element.isPresent()) {
      throw new IllegalArgumentException("Unable to convert string to JAXB, class=" + xmlClass.getName() + ", xml=\n" + xml);
    } else if (element.get().getDeclaredType() == xmlClass) {
      return xmlClass.cast(element.get().getValue());
    }

    throw new JAXBException("Expected XML for class " + xmlClass.getCanonicalName() + " but found " + element.get().getDeclaredType().getCanonicalName());
  }

  /**
   * Convert the XML document contained in the InputStream to a JAXB object.
   *
   * @param <T> Target type of the JAXB object.
   * @param xmlClass The target class of the JAXB object.
   * @param is InputStream containing the XML document.
   * @return JAXB object representing the XML document.
   * @throws JAXBException Could not parse the specified XML document.
   * @throws IOException InputStream could not be read.
   */
  public <T extends Object> T xml2Jaxb(Class<T> xmlClass, InputStream is) throws JAXBException, IOException {
    JAXBElement<T> element = (JAXBElement<T>) unmarshaller().unmarshal(is);
    if (element == null) {
      throw new IllegalArgumentException("Unable to convert stream to JAXB, class=" + xmlClass.getName());
    } else if (element.getDeclaredType() == xmlClass) {
      return xmlClass.cast(element.getValue());
    }

    throw new JAXBException("Expected XML for class " + xmlClass.getCanonicalName() + " but found " + element.getDeclaredType().getCanonicalName());
  }

  /**
   * Convert the XML document contained in the BufferedInputStream to a JAXB object.
   *
   * @param <T> Target type of the JAXB object.
   * @param xmlClass The target class of the JAXB object.
   * @param is BufferedInputStream containing the XML document.
   * @return JAXB object representing the XML document.
   * @throws JAXBException Could not parse the specified XML document.
   * @throws IOException BufferedInputStream could not be read.
   */
  public <T extends Object> T xml2Jaxb(Class<T> xmlClass, BufferedInputStream is) throws JAXBException, IOException {
    JAXBElement<T> element = (JAXBElement<T>) unmarshaller().unmarshal(is);
    if (element.getDeclaredType() == xmlClass) {
      return xmlClass.cast(element.getValue());
    }

    throw new JAXBException("Expected XML for class " + xmlClass.getCanonicalName() + " but found " + element.getDeclaredType().getCanonicalName());
  }

  /**
   * Utility method to marshal a JAXB annotated java object to an XML string.
   *
   * @param messageClass	The class of the object to marshal.
   * @param message             The object to marshal.
   * @return                    String containing the XML encoded object.
   */
  public static String jaxb2String(Class<?> messageClass, Object message) {

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
      log.error("[JaxbParser] jaxb2String: Error marshalling object "
              + messageClass.getName() + ": " + e.toString());
      return null;
    }

    // Return the XML string.
    return writer.toString();
  }
}
