package net.es.sense.rm.driver.nsi.dds;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.dds.lib.client.DdsClient;
import net.es.nsi.dds.lib.client.DocumentsResult;
import net.es.nsi.dds.lib.jaxb.dds.DocumentType;
import net.es.nsi.dds.lib.jaxb.dds.FilterType;
import net.es.nsi.dds.lib.jaxb.dds.NotificationType;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.dds.api.DiscoveryError;
import net.es.sense.rm.driver.nsi.dds.api.Exceptions;
import net.es.sense.rm.driver.nsi.dds.db.Document;
import net.es.sense.rm.driver.nsi.dds.db.DocumentService;
import net.es.sense.rm.driver.nsi.dds.messages.RegistrationEvent;
import net.es.sense.rm.driver.nsi.dds.messages.SubscriptionQuery;
import net.es.sense.rm.driver.nsi.dds.messages.SubscriptionQueryResult;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringApplicationContext;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * The DdsProvider class provides access to all documents stored in the NSI-DDS peers.
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class DdsProvider implements DdsProviderI {

  // Injected runtime properties.
  private final NsiProperties nsiProperties;
  private final DocumentService documentService;
  private final SpringExtension springExtension;
  private final NsiActorSystem nsiActorSystem;
  private final DdsClientProvider ddsClientProvider;

  private ActorRef localDocumentActor;
  private ActorRef documentExpiryActor;
  private ActorRef registrationRouter;


  /**
   * Constructor injection to initialize the DdsProvider.
   *
   * @param nsiProperties
   * @param documentService
   * @param springExtension
   * @param nsiActorSystem
   */
  @Autowired
  public DdsProvider(NsiProperties nsiProperties, DocumentService documentService,
                     SpringExtension springExtension, NsiActorSystem nsiActorSystem,
                     DdsClientProvider ddsClientProvider) {
    this.nsiProperties = nsiProperties;
    this.documentService = documentService;
    this.springExtension = springExtension;
    this.nsiActorSystem = nsiActorSystem;
    this.ddsClientProvider = ddsClientProvider;
  }

  /**
   * Get the singleton instance of ddsProvider bean.
   *
   * @return
   */
  public static DdsProvider getInstance() {
    return SpringApplicationContext.getBean("ddsProvider", DdsProvider.class);
  }

  /**
   * Initialize the DDS provider infrastructure.  This includes the akka actor system, the dds
   * client, configuration of peer NSI-DDS systems, and a one time load of documents into the
   * cache.
   *
   * @throws KeyManagementException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchProviderException
   * @throws KeyStoreException
   * @throws IOException
   * @throws CertificateException
   * @throws UnrecoverableKeyException
   */
  public void start() throws KeyManagementException, NoSuchAlgorithmException,NoSuchProviderException,
          KeyStoreException, IOException, CertificateException, UnrecoverableKeyException {
    // Initialize the actors.
    log.info("[DdsProvider] Starting DDS system initialization...");
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();

    try {
      localDocumentActor = actorSystem.actorOf(springExtension.props("localDocumentActor"), "dds-localDocumentActor");
      documentExpiryActor = actorSystem.actorOf(springExtension.props("documentExpiryActor"), "dds-documentExpiryActor");
      registrationRouter = actorSystem.actorOf(springExtension.props("registrationRouter"), "dds-registrationRouter");
    } catch (Exception ex) {
      log.error("[DdsProvider] Failed to initialize actor", ex);
    }

    /**
    // If there is a secure SSL context specified then we process it.
    if (nsiProperties.getSecure() != null) {
      HttpsContext.getInstance().load(nsiProperties.getSecure());
    }

    // We need to initialize the DDS client with specified configuration.
    if (nsiProperties.getClient().isSecure()) {
      ddsClient = new DdsClient(nsiProperties.getClient(), HttpsContext.getInstance());
    } else {
      ddsClient = new DdsClient(nsiProperties.getClient());
    }
     **/

    // Do a one time load of documents from remote DDS service since our web
    // server may not yet be servicing requests.  From this point on it is
    // controlled through the notification process.
    for (String url : nsiProperties.getPeers()) {
      log.debug("[load] loading documents from url = {}", url);
      DocumentsResult results = getDdsClient().getDocuments(url);
      if (results.getStatus() != Status.OK) {
        log.error("[DdsProvider] could not not load documents from peer = {}, status = {}", url, results.getStatus());
        continue;
      }

      results.getDocuments().stream().filter(Objects::nonNull).forEach(d -> {
        String documentId = Document.documentId(d);

        Document entry = documentService.get(documentId);
        if (entry == null) {
          // This must be the first time we have seen the document so add it
          // into our cache.
          log.debug("[load] new documentId = {}", documentId);
          addDocument(d, Source.REMOTE);
        } else {
          // We have seen the document before.
          log.debug("[load] update documentId = {}", documentId);
          try {
            updateDocument(d, Source.REMOTE);
          } catch (InvalidVersionException ex) {
            // This is an old document version so discard.
            log.debug("[load] old document version documentId = {}", documentId);
          }
        }
      });
    }

    log.info("[DdsProvider] Completed DDS system initialization.");
  }

  public ActorRef GetLocalDocumentActor() {
    return localDocumentActor;
  }

  public ActorRef GetDocumentExpiryActor() {
    return documentExpiryActor;
  }

  public ActorRef GetRegistrationRouter() {
    return registrationRouter;
  }

  public long getLastDiscovered() {
    return documentService.getLastDiscovered();
  }

  /**
   * @return the ddsClient
   */
  public DdsClient getDdsClient() {
    return ddsClientProvider.get();
  }

  public void terminate() {
    // Unsubscribe from our peer DDS servers.
    RegistrationEvent event = new RegistrationEvent();
    event.setEvent(RegistrationEvent.Event.Delete);
    registrationRouter.tell(event, registrationRouter);
    try { Thread.sleep(4000); } catch (Exception ignored) {}

    // We need to kill each of our actors but not touch the actorSystem.
    nsiActorSystem.shutdown(localDocumentActor);
    nsiActorSystem.shutdown(documentExpiryActor);
    nsiActorSystem.shutdown(registrationRouter);
  }

  @Override
  public void processNotification(NotificationType notification) throws WebApplicationException {
    log.debug("[processNotification] event = {}, discovered = {}", notification.getEvent(), notification.getDiscovered());

    // TODO: We discard the event type and discovered time, however, the
    // discovered time could be used for an audit.  Perhaps save it?
    // Determine if we have already seen this document event.
    DocumentType document = notification.getDocument();
    if (document == null) {
      log.debug("[processNotification] Document null.");
      return;
    }

    String documentId = Document.documentId(document);

    Document entry = documentService.get(documentId);
    if (entry == null) {
      // This must be the first time we have seen the document so add it
      // into our cache.
      log.debug("[processNotification] new documentId = {}", documentId);
      addDocument(document, Source.REMOTE);
    } else {
      // We have seen the document before.
      log.debug("[processNotification] update documentId = {}", documentId);
      try {
        updateDocument(document, Source.REMOTE);
      } catch (InvalidVersionException ex) {
        // This is an old document version so discard.
        log.debug("[processNotification] recieved old document version documentId = {}, version = {}",
                documentId, document.getVersion());
      }
    }
  }

  @Override
  public Document addDocument(DocumentType request, Source context) throws WebApplicationException {
    log.debug("[addDocument] id = {}", request.getId());

    // Create and populate our internal document.
    Document document = new Document(request, nsiProperties.getDdsUrl());

    // See if we already have a document under this id.
    if (documentService.get(document.getId()) != null) {
      throw Exceptions.resourceExistsException(DiscoveryError.DOCUMENT_EXISTS, "document", document.getId());
    }

    // This is a new document so add it into the document space.
    Document create = documentService.create(document);

    if (create != null) {
      log.debug("[addDocument] added documentId=" + document.getDocumentId());
    } else {
      log.error("[addDocument] failed to add documentId=" + document.getDocumentId());
      throw Exceptions.doesNotExistException(DiscoveryError.INTERNAL_SERVER_ERROR, "document", document.getDocumentId());
    }

    return document;
  }

  /**
   * Delete a document from the DDS document space. The document is deleted from the local repository if stored locally,
   * and a document update is propagated with and expiry time of now.
   *
   * @param nsa
   * @param type
   * @param id
   * @return
   * @throws WebApplicationException
   */
  @Override
  public Document deleteDocument(String nsa, String type, String id) throws WebApplicationException {
    // See if we already have a document under this id.
    String documentId = Document.documentId(nsa, type, id);

    log.debug("[deleteDocument] documentId=" + documentId);

    Document document = documentService.get(documentId);
    if (document == null) {
      throw Exceptions.doesNotExistException(DiscoveryError.DOCUMENT_DOES_NOT_EXIST, "document", documentId);
    }

    // Update this document with an expires time of now.
    long now = System.currentTimeMillis();
    XMLGregorianCalendar currentDate;
    try {
      currentDate = XmlUtilities.longToXMLGregorianCalendar(now);
    } catch (DatatypeConfigurationException ex) {
      throw Exceptions.internalServerErrorException("XMLGregorianCalendar", ex.getMessage());
    }

    document.setExpires(now);

    DocumentType documentT;
    try {
      documentT = document.getDocumentFull();
      documentT.setExpires(currentDate);
      documentT.setVersion(currentDate);
      document.setLastDiscovered(Document.now());
      document.setDocumentType(documentT);
    } catch (JAXBException | IOException ex) {
      throw Exceptions.internalServerErrorException("getDocumentFull", ex.getMessage());
    }

    Document update = documentService.update(document);

    if (update != null) {
      log.debug("[deleteDocument] deleted documentId=" + documentId);
    } else {
      log.error("[deleteDocument] failed to delete documentId=" + documentId);
      throw Exceptions.doesNotExistException(DiscoveryError.INTERNAL_SERVER_ERROR, "document", documentId);
    }

    return document;
  }

  /**
   *
   * @param request
   * @param context
   * @return
   * @throws WebApplicationException
   */
  @Override
  public Document updateDocument(DocumentType request, Source context) throws WebApplicationException {
    // Create a document identifier to look up in our documet table.
    String documentId = Document.documentId(request);

    // See if we have a document under this id.
    Document document = documentService.get(documentId);
    if (document == null) {
      throw Exceptions.doesNotExistException(DiscoveryError.NOT_FOUND, "document", documentId);
    }

    // Validate basic fields.
    long requestVersion;
    if (request.getVersion() == null || !request.getVersion().isValid()) {
      throw Exceptions.missingParameterException(documentId, "version");
    } else {
      try {
        requestVersion = Document.getTime(request.getVersion());
      } catch (DatatypeConfigurationException ex) {
        throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, documentId, "version");
      }
    }

    if (request.getExpires() == null || !request.getExpires().isValid()) {
      throw Exceptions.missingParameterException(documentId, "expires");
    }

    // Make sure this is a new version of the document.
    if (requestVersion == document.getVersion()) {
      log.debug("[updateDocument] received document is a duplicate id=" + documentId);
      throw Exceptions.invalidVersionException(DiscoveryError.DOCUMENT_VERSION, request.getId(), requestVersion, document.getVersion());
    } else if (requestVersion < document.getVersion()) {
      log.debug("[updateDocument] received document is an old version id=" + documentId);
      throw Exceptions.invalidVersionException(DiscoveryError.DOCUMENT_VERSION, request.getId(), requestVersion, document.getVersion());
    }

    // Begin temp debugging.
    log.debug("[updateDocument] updating document from {} to {}", document.getVersion(), requestVersion);
    try {
      log.debug("[updateDocument] original doc version: {}, new doc version {}",
              document.getDocumentSummary().getVersion(),
              request.getVersion());
    } catch (JAXBException | IOException ex) {
      log.error("Failed to decode old document {}", documentId);
    }
    // End temp debugging.

    // Now we update the existing document with the new parameters.
    document.setLastDiscovered(Document.now());
    document.setVersion(requestVersion);
    document.setExpires(Document.expires(request.getExpires()));
    try {
      document.setDocumentType(request);
    } catch (JAXBException | IOException ex) {
      throw Exceptions.illegalArgumentException(DiscoveryError.INVALID_PARAMETER, documentId, "DocumentType");
    }

    // Now we write this out to disk updating the existing document.
    Document update = documentService.update(document);
    if (update != null) {
      log.debug("[updateDocument] updated documentId=" + documentId);
    } else {
      log.error("[updateDocument] failed to update documentId=" + documentId);
      throw Exceptions.doesNotExistException(DiscoveryError.INTERNAL_SERVER_ERROR, "document", documentId);
    }

    // Begin temp debugging.
    Document test = documentService.get(documentId);
    log.debug("[updateDocument] read updated document {}, {}", documentId, test.getVersion());
    try {
      log.debug("[updateDocument] updated doc version: {}",
              test.getDocumentSummary().getVersion());
    } catch (JAXBException | IOException ex) {
      log.error("Failed to decode new document {}", documentId);
    }
    // End temp debugging.

    return update;
  }

  @Override
  public Collection<Document> getNewerDocuments(long lastDiscovered) {
    if (lastDiscovered != 0) {
      return Lists.newArrayList(documentService.get(lastDiscovered));
    }
    else {
       return Lists.newArrayList(documentService.get());
    }
  }

  @Override
  public Collection<Document> getDocuments(String nsa, String type, String id, long lastDiscovered) {
    // We need to search for matching documents using the supplied criteria.
    // We will do this linearly now, but we will need multiple indices later
    // to make this faster.

    // Seed the results.
    Collection<Document> results = Lists.newArrayList(documentService.get());

    // This may be the most often used so filter by this first.
    if (lastDiscovered != 0) {
      results = getDocumentsByDate(lastDiscovered, results);
    }

    if (!Strings.isNullOrEmpty(nsa)) {
      results = getDocumentsByNsa(nsa, results);
    }

    if (!Strings.isNullOrEmpty(type)) {
      results = getDocumentsByType(type, results);
    }

    if (!Strings.isNullOrEmpty(id)) {
      results = getDocumentsById(id, results);
    }

    return results;
  }

  @Override
  public Collection<Document> getDocumentsByNsa(String nsa, String type, String id, long lastDiscovered) throws WebApplicationException {
    // Seed the results.
    Collection<Document> results;

    // This is the primary search value.  Make sure it is present.
    if (!Strings.isNullOrEmpty(nsa)) {
      results = Lists.newArrayList(documentService.getByNsa(nsa));
      if (results.isEmpty()) {
        throw Exceptions.doesNotExistException(DiscoveryError.NOT_FOUND, "nsa", nsa);
      }
    } else {
      throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
    }

    // The rest are additional filters.
    if (lastDiscovered != 0) {
      results = getDocumentsByDate(lastDiscovered, results);
    }

    if (!Strings.isNullOrEmpty(type)) {
      results = getDocumentsByType(type, results);
    }

    if (!Strings.isNullOrEmpty(id)) {
      results = getDocumentsById(id, results);
    }

    return results;
  }

  @Override
  public Collection<Document> getDocumentsByNsaAndType(String nsa, String type, long lastDiscovered) throws WebApplicationException {
    // This is the primary search value.  Make sure it is present.
    if (Strings.isNullOrEmpty(nsa)) {
      throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
    }

    if (Strings.isNullOrEmpty(type)) {
      throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "type");
    }

    Collection<Document> results = Lists.newArrayList(documentService.getByNsaAndType(nsa, type));

    // The rest are additional filters.
    if (lastDiscovered != 0) {
      results = getDocumentsByDate(lastDiscovered, results);
    }

    return results;
  }

  @Override
  public Collection<Document> getDocumentsByNsaAndTypeAndId(String nsa, String type, String id, long lastDiscovered) throws WebApplicationException {
    // This is the primary search value.  Make sure it is present.
    if (Strings.isNullOrEmpty(nsa)) {
      throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
    }

    if (Strings.isNullOrEmpty(type)) {
      throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "type");
    }

    if (Strings.isNullOrEmpty(id)) {
      throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "id");
    }

    Collection<Document> results = Lists.newArrayList(documentService.getByNsaAndTypeAndDocumentId(nsa, type, id));

    // The rest are additional filters.
    if (lastDiscovered != 0) {
      results = getDocumentsByDate(lastDiscovered, results);
    }

    return results;
  }

  @Override
  public Document getDocument(String nsa, String type, String id, long lastDiscovered) throws WebApplicationException {
    String documentId = Document.documentId(nsa, type, id);
    Document document = documentService.get(documentId);

    if (document == null) {
      throw Exceptions.doesNotExistException(DiscoveryError.DOCUMENT_DOES_NOT_EXIST, "document", "nsa=" + nsa + ", type=" + type + ", id=" + id);
    }

    // Check to see if the document was modified after provided date.
    if (lastDiscovered != 0 && lastDiscovered >= document.getLastDiscovered()) {
      // NULL will represent not modified.
      return null;
    }

    return document;
  }

  @Override
  public Document getDocument(String nsa, String type, String id) throws WebApplicationException {
    String documentId = Document.documentId(nsa, type, id);
    Document document = documentService.get(documentId);

    if (document == null) {
      throw Exceptions.doesNotExistException(DiscoveryError.DOCUMENT_DOES_NOT_EXIST, "document", "nsa=" + nsa + ", type=" + type + ", id=" + id);
    }

    return document;
  }

  /*@Override
  public Document getDocument(DocumentType document) throws IllegalArgumentException, NotFoundException {
    String documentId = Document.documentId(document);
    return documentService.get(documentId);
  }*/

  @Override
  public Collection<Document> getLocalDocuments(String type, String id, long lastDiscovered) throws IllegalArgumentException {
    String nsaId = nsiProperties.getNsaId();

    // This is the primary search value.  Make sure it is present.
    if (Strings.isNullOrEmpty(nsaId)) {
      throw Exceptions.illegalArgumentException(DiscoveryError.MISSING_PARAMETER, "document", "nsa");
    }

    // Seed the results.
    Collection<Document> results = Lists.newArrayList(documentService.getByNsa(nsaId));

    // The rest are additional filters.
    if (lastDiscovered != 0) {
      results = getDocumentsByDate(lastDiscovered, results);
    }

    if (!Strings.isNullOrEmpty(type)) {
      results = getDocumentsByType(type, results);
    }

    if (!Strings.isNullOrEmpty(id)) {
      results = getDocumentsById(id, results);
    }

    return results;
  }

  @Override
  public Collection<Document> getLocalDocumentsByType(String type, String id, long lastDiscovered) throws IllegalArgumentException {
    return getDocumentsByNsaAndType(nsiProperties.getNsaId(), type, lastDiscovered);
  }

  @Override
  public Document getLocalDocument(String type, String id, long lastDiscovered) throws IllegalArgumentException, NotFoundException {
    return getDocument(nsiProperties.getNsaId(), type, id, lastDiscovered);
  }

  public Collection<Document> getDocumentsByDate(long lastDiscovered, Collection<Document> input) {
    return Lists.newArrayList(documentService.get(lastDiscovered));
  }

  private Collection<Document> getDocumentsByNsa(String nsa, Collection<Document> input) {
    Collection<Document> output = new ArrayList<>();
    input.stream().filter((document) -> (document.getNsa().equalsIgnoreCase(nsa)))
            .forEach((document) -> {
              output.add(document);
            });

    return output;
  }

  public Iterable<Document> getDocumentsByNsa(String nsa) {
    return documentService.getByNsa(nsa);
  }

  private Collection<Document> getDocumentsByType(String type, Collection<Document> input) {
    Collection<Document> output = new ArrayList<>();
    input.stream().filter((document) -> (document.getType().equalsIgnoreCase(type)))
            .forEach((document) -> {
              output.add(document);
            });

    return output;
  }

  public Iterable<Document> getDocumentsByType(String type) {
    return documentService.getByType(type);
  }

  private Collection<Document> getDocumentsById(String id, Collection<Document> input) {
    Collection<Document> output = new ArrayList<>();
    input.stream().filter((document) -> (document.getId().equalsIgnoreCase(id)))
            .forEach((document) -> {
              output.add(document);
            });

    return output;
  }

  public Iterable<Document> getDocumentsById(String id) {
    return documentService.getByDocumentId(id);
  }

  public Iterable<Document> getDocumentsByTypeAndId(String type, String id) {
    return documentService.getByTypeAndDocumentId(type, id);
  }

  /**
   *
   * @param filter
   * @return
   */
  @Override
  public Collection<Document> getDocuments(FilterType filter) {
    // TODO: Match everything for demo.  Need to fix later.
    return Lists.newArrayList(documentService.get());
  }

  public boolean isSubscription(String url) {
    log.debug("[DdsProvider] isSubscription test for url = {}", url);
    SubscriptionQuery query = new SubscriptionQuery();
    query.setUrl(url);
    Timeout timeout = new Timeout(Duration.create(5, "seconds"));
    Future<Object> future = Patterns.ask(registrationRouter, query, timeout);
    try {
      SubscriptionQueryResult result = (SubscriptionQueryResult) Await.result(future, timeout.duration());
      log.debug("[DdsProvider] isSubscription result for url = {}, value = {}", url, result.getSubscription());
      return result.getSubscription() != null;
    } catch (Exception ex) {
      log.error("[DdsProvider] Failed to sent message to registrationRouter, exception = {}", ex);
    }

    return false;
  }

  /**
   *
   * @param event
   * @return
   */
  /* @Override
  public Collection<Subscription> getSubscriptions(DocumentEvent event) {
    // TODO: Match everything for demo.  Need to fix later.
    return getSubscriptions();
  }*/

/**
    @Override
    public Subscription addSubscription(SubscriptionRequestType request, String encoding) {
        log.debug("DdsProvider.addSubscription: requesterId=" + request.getRequesterId());

        // Populate a subscription object.
        Subscription subscription = new Subscription(request, encoding, configReader.getBaseURL());

        // Save the subscription.
        subscriptions.put(subscription.getId(), subscription);

        // Now we need to schedule the send of the initial set of matching
        // documents in a notification to this subscription.  We delay the
        // send so that the requester has time to return and store the
        // subscription identifier.
        SubscriptionEvent se = new SubscriptionEvent();
        se.setEvent(SubscriptionEvent.Event.New);
        se.setSubscription(subscription);
        Cancellable scheduleOnce = ddsActorController.scheduleNotification(se, 5);
        subscription.setAction(scheduleOnce);

        log.debug("DdsProvider.addSubscription: schedule notification delivery for " + subscription.getId());

        return subscription;
    }

    @Override
    public Subscription deleteSubscription(String id) throws WebApplicationException {
        log.debug("DdsProvider.deleteSubscription: id=" + id);

        if (id == null || id.isEmpty()) {
            throw Exceptions.missingParameterException("subscription", "id");
        }

        Subscription subscription = subscriptions.remove(id);
        if (subscription == null) {
             throw Exceptions.doesNotExistException(DiscoveryError.SUBCRIPTION_DOES_NOT_EXIST, "id", id);
        }

        if (subscription.getAction() != null) {
            subscription.getAction().cancel();
            subscription.setAction(null);
        }

        return subscription;
    }

    @Override
    public Subscription editSubscription(String id, SubscriptionRequestType request, String encoding) throws WebApplicationException {
        log.debug("[DdsProvider] edit subscription id={}", id);

        // Make sure we have all needed parameters.
        if (id == null || id.isEmpty()) {
            throw Exceptions.missingParameterException("subscription", "id");
        }

        Subscription subscription = subscriptions.get(id);
        if (subscription == null) {
            throw Exceptions.doesNotExistException(DiscoveryError.SUBCRIPTION_DOES_NOT_EXIST, "id", id);
        }

        // Get the current time and remove milliseconds.
        Date currentTime = new Date();
        long fixed = currentTime.getTime() / 1000;
        currentTime.setTime(fixed * 1000);

        log.debug("[DdsProvider] changing subscription lastModified time from " + subscription.getLastModified() + " to "+ currentTime);

        subscription.setEncoding(encoding);
        subscription.setLastModified(currentTime);
        SubscriptionType sub = subscription.getSubscription();
        sub.setRequesterId(request.getRequesterId());
        sub.setFilter(request.getFilter());
        sub.setCallback(request.getCallback());
        sub.getAny().addAll(request.getAny());
        sub.getOtherAttributes().putAll(request.getOtherAttributes());

        SubscriptionEvent se = new SubscriptionEvent();
        se.setEvent(SubscriptionEvent.Event.Update);
        se.setSubscription(subscription);
        ddsActorController.sendNotification(se);

        return subscription;
    }
**/
}
