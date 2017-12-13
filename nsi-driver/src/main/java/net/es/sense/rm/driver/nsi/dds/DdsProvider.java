package net.es.sense.rm.driver.nsi.dds;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.dds.lib.jaxb.dds.DocumentType;
import net.es.nsi.dds.lib.jaxb.dds.FilterType;
import net.es.nsi.dds.lib.jaxb.dds.NotificationType;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.dds.api.DiscoveryError;
import net.es.sense.rm.driver.nsi.dds.api.Exceptions;
import net.es.sense.rm.driver.nsi.dds.db.Document;
import net.es.sense.rm.driver.nsi.dds.db.DocumentService;
import net.es.sense.rm.driver.nsi.dds.messages.StartMsg;
import net.es.sense.rm.driver.nsi.dds.messages.SubscriptionQuery;
import net.es.sense.rm.driver.nsi.dds.messages.SubscriptionQueryResult;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class DdsProvider implements DdsProviderI {

  // Runtime properties.
  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private DocumentService documentService;

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiActorSystem nsiActorSystem;

  private ActorRef localDocumentActor;
  private ActorRef documentExpiryActor;
  private ActorRef registrationRouter;

  public void start() {
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

    // Kick off those that need to be started.
    StartMsg msg = new StartMsg();
    nsiActorSystem.getActorSystem().scheduler().scheduleOnce(Duration.create(60, TimeUnit.SECONDS),
            registrationRouter, msg, nsiActorSystem.getActorSystem().dispatcher(), null);

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

  public void terminate() {
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
        log.debug("[processNotification] old document version documentId = {}", documentId);
      }
    }
  }

  @Override
  public Document addDocument(DocumentType request, Source context) throws WebApplicationException {
    log.debug("[addDocument] id = {}", request.getId());

    // Create and populate our internal document.
    Document document = new Document(request, nsiProperties.getDdsUrl());

    // See if we already have a document under this id.
    Optional<Document> get = Optional.fromNullable(documentService.get(document.getId()));
    if (get.isPresent()) {
      throw Exceptions.resourceExistsException(DiscoveryError.DOCUMENT_EXISTS, "document", document.getId());
    }

    // Validate basic fields.
    if (Strings.isNullOrEmpty(request.getNsa())) {
      throw Exceptions.missingParameterException(document.getId(), "nsa");
    }

    if (Strings.isNullOrEmpty(request.getType())) {
      throw Exceptions.missingParameterException(document.getId(), "type");
    }

    if (Strings.isNullOrEmpty(request.getId())) {
      throw Exceptions.missingParameterException(document.getId(), "id");
    }

    if (request.getVersion() == null || !request.getVersion().isValid()) {
      throw Exceptions.missingParameterException(document.getId(), "version");
    }

    if (request.getExpires() == null || !request.getExpires().isValid()) {
      throw Exceptions.missingParameterException(document.getId(), "expires");
    }

    // This is a new document so add it into the document space.
    documentService.create(document);
/**
    // Route a new document event.
    DocumentEvent de = new DocumentEvent();
    de.setEvent(DocumentEventType.NEW);
    de.setDocument(document);
    ddsController.sendNotification(de);
**/
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
      document.setLastDiscovered(now);
      document.setDocumentType(documentT);
    } catch (JAXBException | IOException ex) {
      throw Exceptions.internalServerErrorException("getDocumentFull", ex.getMessage());
    }

    documentService.create(document);
/**
    // Route a update document event.
    DocumentEvent de = new DocumentEvent();
    de.setEvent(DocumentEventType.UPDATED);
    de.setDocument(newDoc);
    ddsController.sendNotification(de);
**/
    return document;
  }

  @Override
  public Document updateDocument(String nsa, String type, String id, DocumentType request, Source context) throws WebApplicationException, InvalidVersionException {
    // Create a document identifier to look up in our documet table.
    String documentId = Document.documentId(nsa, type, id);

    // See if we have a document under this id.
    Document document = documentService.get(documentId);
    if (document == null) {
      String error = DiscoveryError.getErrorString(DiscoveryError.DOCUMENT_DOES_NOT_EXIST, "document", documentId);
      throw Exceptions.doesNotExistException(DiscoveryError.NOT_FOUND, "document", documentId);
    }

    // Validate basic fields.
    if (request.getNsa() == null || request.getNsa().isEmpty() || !request.getNsa().equalsIgnoreCase(document.getNsa())) {
      throw Exceptions.missingParameterException(documentId, "nsa");
    }

    if (request.getType() == null || request.getType().isEmpty() || !request.getType().equalsIgnoreCase(document.getType())) {
      throw Exceptions.missingParameterException(documentId, "type");
    }

    if (request.getId() == null || request.getId().isEmpty() || !request.getId().equalsIgnoreCase(document.getDocumentId())) {
      throw Exceptions.missingParameterException(documentId, "id");
    }

    if (request.getVersion() == null || !request.getVersion().isValid()) {
      throw Exceptions.missingParameterException(documentId, "version");
    }

    if (request.getExpires() == null || !request.getExpires().isValid()) {
      throw Exceptions.missingParameterException(documentId, "expires");
    }

    DocumentType documentT;
    try {
      documentT = document.getDocumentFull();
    } catch (JAXBException | IOException ex) {
      throw Exceptions.internalServerErrorException("getDocumentFull", ex.getMessage());
    }

    // Make sure this is a new version of the document.
    if (request.getVersion().compare(documentT.getVersion()) == DatatypeConstants.EQUAL) {
      log.debug("[updateDocument] received document is a duplicate id=" + documentId);
      throw Exceptions.invalidVersionException(DiscoveryError.DOCUMENT_VERSION, request.getId(), request.getVersion(), documentT.getVersion());
    } else if (request.getVersion().compare(documentT.getVersion()) == DatatypeConstants.LESSER) {
      log.debug("[updateDocument] received document is an old version id=" + documentId);
      throw Exceptions.invalidVersionException(DiscoveryError.DOCUMENT_VERSION, request.getId(), request.getVersion(), documentT.getVersion());
    }

    Document newDoc = new Document(request, nsiProperties.getDdsUrl());
    documentService.create(newDoc);
    log.debug("[updateDocument] updated documentId=" + documentId);

    // Route a update document event.
/**
    DocumentEvent de = new DocumentEvent();
    de.setEvent(DocumentEventType.UPDATED);
    de.setDocument(newDoc);
    ddsController.sendNotification(de);
**/
    return newDoc;
  }

  @Override
  public Document updateDocument(DocumentType request, Source context) throws WebApplicationException, InvalidVersionException {
    return updateDocument(request.getNsa(), request.getType(), request.getId(), request, context);
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
    // We will do this linearly now, but we will need multiple indicies later
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
    if (!!Strings.isNullOrEmpty(nsa)) {
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
