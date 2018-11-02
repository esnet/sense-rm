package net.es.sense.rm.driver.nsi.dds.db;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.dds.lib.jaxb.DdsParser;
import net.es.nsi.dds.lib.jaxb.dds.DocumentType;
import net.es.nsi.dds.lib.jaxb.dds.ObjectFactory;
import net.es.sense.rm.driver.nsi.dds.api.DiscoveryError;
import net.es.sense.rm.driver.nsi.dds.api.Exceptions;

/**
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

  @Id
  @Basic(optional=false)
  private String id;

  @Basic(optional=false)
  private String nsa;

  @Basic(optional=false)
  private String type;

  @Basic(optional=false)
  private String documentId;

  // When we first encountered this document.
  private long lastDiscovered;

  // When this document expires.
  private long expires;

  // The document discovered from the DDS.
  @Lob
  @Basic(fetch=FetchType.LAZY, optional=true)
  private String document;

  /**
   *
   * @param documentT
   * @param baseURL
   * @throws WebApplicationException
   */
  public Document(DocumentType documentT, String baseURL) throws WebApplicationException {
    // Populate the searchable fields.
    this.id = documentId(documentT);
    this.nsa = documentT.getNsa().trim();
    this.type = documentT.getType().trim();
    this.documentId = documentT.getId().trim();

    // This is the time we discovered this documentT.
    //Date discovered = new Date();
    //discovered.setTime(discovered.getTime() - discovered.getTime() % 1000);
    lastDiscovered = (System.currentTimeMillis() / 1000) * 1000;

    if (documentT.getExpires() != null) {
      try {
        expires = XmlUtilities.xmlGregorianCalendarToDate(documentT.getExpires()).getTime();
      } catch (DatatypeConfigurationException ex) {
        throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, "document", "expires");
      }
    }
    else {
      expires = Long.MAX_VALUE;
    }

    // Store the documentT contents.
    documentT.setNsa(this.nsa);
    documentT.setType(this.type);
    documentT.setId(this.documentId);
    documentT.setHref(getDocumentURL(baseURL));

    try {
      this.document = DdsParser.getInstance().document2Xml(documentT);
    } catch (JAXBException | IOException ex) {
      throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, "document", "DocumentType");
    }
  }

  public Document(DocumentType documentT) throws WebApplicationException {
    this(documentT, null);
  }

  /**
   *
   * @param nsa
   * @param type
   * @param id
   * @return
   * @throws WebApplicationException
   */
  public static String documentId(String nsa, String type, String id) throws WebApplicationException {
    if (nsa == null || nsa.trim().isEmpty()) {
      throw Exceptions.missingParameterException("document", "nsa");
    } else if (type == null || type.trim().isEmpty()) {
      throw Exceptions.missingParameterException("document", "type");
    } else if (id == null || id.trim().isEmpty()) {
      throw Exceptions.missingParameterException("document", "id");
    }

    StringBuilder sb = new StringBuilder();

    try {
      sb.append(URLEncoder.encode(nsa.trim(), "UTF-8"));
      sb.append("/").append(URLEncoder.encode(type.trim(), "UTF-8"));
      sb.append("/").append(URLEncoder.encode(id.trim(), "UTF-8"));
    } catch (UnsupportedEncodingException ex) {
      throw Exceptions.illegalArgumentException(DiscoveryError.DOCUMENT_INVALID, "document", "id");
    }
    return sb.toString();
  }

  /**
   *
   * @param documentT
   * @return
   * @throws IllegalArgumentException
   */
  public static String documentId(DocumentType documentT) throws IllegalArgumentException {
    return documentId(documentT.getNsa(), documentT.getType(), documentT.getId());
  }

  /**
   *
   * @param baseURL
   * @return
   * @throws WebApplicationException
   */
  private String getDocumentURL(String baseURL) throws WebApplicationException {
    if (baseURL == null) {
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
}
