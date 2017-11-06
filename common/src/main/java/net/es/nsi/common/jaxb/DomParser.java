package net.es.nsi.common.jaxb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author hacksaw
 */
public class DomParser {

  /**
   * Convert an InputStream to Dom Document.
   *
   * @param is
   * @return
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  public static Document xml2Dom(InputStream is)
          throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();

    return builder.parse(is);
  }

  public static Document xml2Dom(String xml)
          throws ParserConfigurationException, SAXException, IOException {
    InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    return xml2Dom(stream);
  }

  /**
   * Convert a DOM Document to a string.
   *
   * @param doc
   * @return
   * @throws javax.xml.transform.TransformerException
   * @throws javax.xml.transform.TransformerConfigurationException
   */
  public static String doc2Xml(Document doc)
          throws TransformerException, TransformerConfigurationException {
    doc.setXmlStandalone(true);
    doc.setStrictErrorChecking(true);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer trans = tf.newTransformer();
    trans.transform(new DOMSource(doc), new StreamResult(baos));
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }

  public static String prettyPrint(Document document)
          throws TransformerConfigurationException, TransformerException {
    document.setXmlStandalone(true);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    transformer.transform(new DOMSource(document), new StreamResult(baos));
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }
}
