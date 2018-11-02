package net.es.sense.rm.driver.nsi.dds;

import java.util.Collection;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.dds.lib.jaxb.dds.DocumentType;
import net.es.nsi.dds.lib.jaxb.dds.FilterType;
import net.es.nsi.dds.lib.jaxb.dds.NotificationType;
import net.es.sense.rm.driver.nsi.dds.db.Document;

/**
 *
 * @author hacksaw
 */
public interface DdsProviderI {

  public void processNotification(NotificationType notification);

  public Document addDocument(DocumentType document, Source context) throws WebApplicationException;

  public Document deleteDocument(String nsa, String type, String id) throws WebApplicationException;
  
  public Document updateDocument(DocumentType request, Source context) throws WebApplicationException, InvalidVersionException;

  public Collection<Document> getNewerDocuments(long lastDiscovered);

  public Collection<Document> getDocuments(FilterType filter);

  public Collection<Document> getDocuments(String nsa, String type, String id, long lastDiscovered);

  public Collection<Document> getDocumentsByNsa(String nsa, String type, String id, long lastDiscovered) throws WebApplicationException;

  public Collection<Document> getDocumentsByNsaAndType(String nsa, String type, long lastDiscovered) throws WebApplicationException;

  public Collection<Document> getDocumentsByNsaAndTypeAndId(String nsa, String type, String id, long lastDiscovered) throws WebApplicationException;

  public Document getDocument(String nsa, String type, String id, long lastDiscovered) throws WebApplicationException;

  // public Document getDocument(DocumentType document) throws WebApplicationException;
  public Collection<Document> getLocalDocuments(String type, String id, long lastDiscovered) throws WebApplicationException;

  public Collection<Document> getLocalDocumentsByType(String type, String id, long lastDiscovered) throws WebApplicationException;

  public Document getLocalDocument(String type, String id, long lastDiscovered) throws WebApplicationException;
/**
  public Subscription addSubscription(SubscriptionRequestType request, String encoding);

  public Subscription deleteSubscription(String id) throws WebApplicationException;

  public Subscription editSubscription(String id, SubscriptionRequestType request, String encoding) throws WebApplicationException;
**/
}
