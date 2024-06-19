package net.es.sense.rm.driver.nsi.dds.db;

import com.google.common.base.Strings;
import jakarta.persistence.*;
import jakarta.ws.rs.WebApplicationException;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.dds.lib.jaxb.DdsParser;
import net.es.nsi.dds.lib.jaxb.dds.DocumentType;
import net.es.nsi.dds.lib.jaxb.dds.ObjectFactory;
import net.es.sense.rm.driver.nsi.dds.api.DiscoveryError;
import net.es.sense.rm.driver.nsi.dds.api.Exceptions;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * This class represents an NSI-DDS document as stored in the database.
 *
 * @author hacksaw
 */
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Data
@Entity
@Table(name = "documents")
public class Document implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final String DOCUMENTS_URL = "documents";
  private static final ObjectFactory FACTORY = new ObjectFactory();

  // Unique database index identifier.
  @Id
  @Basic(optional=false)
  private String id;

  // The unique NSA identifier URI owning this document.
  @Basic(optional=false)
  private String nsa;

  // The document type (i.e. vnd.ogf.nsi.nsa.v1+xml or vnd.ogf.nsi.topology.v2+xml).
  @Basic(optional=false)
  private String type;

  // The document identifier unique in the context of the NSA identifier.
  @Basic(optional=false)
  private String documentId;

  // The version of the document.
  private long version;

  // When we first encountered this document.
  private long lastDiscovered;

  // When this document expires.
  private long expires;

  // The document discovered from the DDS.
  @Lob
  @Basic(fetch=FetchType.LAZY, optional=true)
  private String document;

  /**
   * Document constructor accepting a document type object and a baseURL for setting the href.
   *
   * @param documentT The document object to used for initialization.
   * @param baseURL The base URL for the document.
   * @throws WebApplicationException For any errors creating the document.
   */
  public Document(DocumentType documentT, String baseURL) throws WebApplicationException {
    // Populate the searchable fields.
    this.id = documentId(documentT);
    this.nsa = documentT.getNsa();
    this.type = documentT.getType();
    this.documentId = documentT.getId();

    // This is the time we discovered this documentT.
    lastDiscovered = now();
    expires = expires(documentT.getExpires());
    try {
      version = getTime(documentT.getVersion());
    } catch (DatatypeConfigurationException ex) {
      throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, this.id, "version");
    }

    // Store the documentT contents.
    documentT.setHref(getDocumentURL(baseURL));

    try {
      this.document = DdsParser.getInstance().document2Xml(documentT);
    } catch (JAXBException | IOException ex) {
      throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, this.id, "DocumentType");
    }
  }

  /**
   * Document constructor accepting a document type object used for initialization.
   *
   * @param documentT The document object to used for initialization.
   * @throws WebApplicationException For any errors creating the document.
   */
  public Document(DocumentType documentT) throws WebApplicationException {
    this(documentT, null);
  }

  /**
   * Create an NSI-DDS document identifier that is URL encoded.
   *
   * @param nsa The NSA owning the document.
   * @param type The type of document.
   * @param id The unique identifier for the document.
   * @return URL encoded globally unique document identifier.
   * @throws WebApplicationException If the document identifier cannot be URL encoded.
   */
  public static String documentId(String nsa, String type, String id) throws WebApplicationException {
    if (Strings.isNullOrEmpty(nsa)) {
      throw Exceptions.missingParameterException("document", "nsa");
    } else if (Strings.isNullOrEmpty(type)) {
      throw Exceptions.missingParameterException("document", "type");
    } else if (Strings.isNullOrEmpty(id)) {
      throw Exceptions.missingParameterException("document", "id");
    }

    StringBuilder sb = new StringBuilder();
    sb.append(URLEncoder.encode(nsa, StandardCharsets.UTF_8));
    sb.append("/").append(URLEncoder.encode(type, StandardCharsets.UTF_8));
    sb.append("/").append(URLEncoder.encode(id, StandardCharsets.UTF_8));
    return sb.toString();
  }

  /**
   * Create an NSI-DDS document identifier that is URL encoded.
   *
   * @param documentT The document to use as the source for the identifier.
   * @return URL encoded globally unique document identifier.
   * @throws WebApplicationException If the document identifier cannot be URL encoded.
   */
  public static String documentId(DocumentType documentT) throws WebApplicationException {
    return documentId(documentT.getNsa(), documentT.getType(), documentT.getId());
  }

  /**
   *
   * @param baseURL
   * @return
   * @throws WebApplicationException
   */
  private String getDocumentURL(String baseURL) throws WebApplicationException {
    if (Strings.isNullOrEmpty(baseURL)) {
      return null;
    }

    URL url;
    try {
      if (!baseURL.endsWith("/")) {
        baseURL = baseURL + "/";
      }
      url = new URL(baseURL);
      url = new URL(url, DOCUMENTS_URL + "/" + this.id);
    } catch (MalformedURLException ex) {
      throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, "document", "href");
    }

    return url.toExternalForm();
  }

  /**
   *
   * @return the document
   * @throws JAXBException
   * @throws IOException
   */
  public DocumentType getDocumentSummary() throws JAXBException, IOException {
    DocumentType documentT = DdsParser.getInstance().xml2Document(this.document);
    DocumentType newDocType = FACTORY.createDocumentType();
    newDocType.setExpires(documentT.getExpires());
    newDocType.setHref(documentT.getHref());
    newDocType.setId(documentT.getId());
    newDocType.setNsa(documentT.getNsa());
    newDocType.setType(documentT.getType());
    newDocType.setVersion(documentT.getVersion());
    return newDocType;
  }

  /**
   *
   * @return the document
   * @throws JAXBException
   * @throws IOException
   */
  public DocumentType getDocumentFull() throws JAXBException, IOException {
    return DdsParser.getInstance().xml2Document(this.document);
  }

  public void setDocumentType(DocumentType documentT) throws JAXBException, IOException {
    this.document = DdsParser.getInstance().document2Xml(documentT);
  }


  public static long now() {
    return (System.currentTimeMillis() / 1000) * 1000;
  }

  public static long expires(XMLGregorianCalendar expires) throws WebApplicationException {
    if (expires != null) {
      try {
        return getTime(expires);
      } catch (DatatypeConfigurationException ex) {
        throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, "document", "expires");
      }
    }
    else {
      return Long.MAX_VALUE;
    }
  }

  public static long getTime(XMLGregorianCalendar date) throws DatatypeConfigurationException {
    return XmlUtilities.xmlGregorianCalendarToDate(date).getTime();
  }
}
