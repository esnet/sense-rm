package net.es.sense.rm.driver.nsi.dds.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.dds.lib.jaxb.DdsParser;
import net.es.nsi.dds.lib.jaxb.dds.DocumentListType;
import net.es.nsi.dds.lib.jaxb.dds.ErrorType;
import net.es.nsi.dds.lib.jaxb.dds.NotificationListType;
import net.es.nsi.dds.lib.jaxb.dds.NotificationType;
import net.es.nsi.dds.lib.jaxb.dds.ObjectFactory;
import net.es.sense.rm.driver.nsi.dds.DdsProvider;
import net.es.sense.rm.driver.nsi.dds.db.Document;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Service
@RestController
//@EnableWebSecurity
@RequestMapping(value = "/api/dds/v1")
public class DiscoveryService {

  @Autowired
  private DdsProvider ddsProvider;

  private final ObjectFactory FACTORY = new ObjectFactory();

  /**
   * Ping to see if the DDS service is operational.
   *
   * @param activeUser
   * @return
   * @throws Exception
   */
  @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = {MediaType.APPLICATION_XML_VALUE, Nsi.NSI_DDS_V1_XML})
  @ResponseBody
  public ResponseEntity<?> ping(/*@AuthenticationPrincipal User activeUser*/) throws Exception {

    log.error("[ping] PING!");
    /*
    if (activeUser == null) {
      log.debug("[ping] Security Context is null.");
    } else {
      log.debug("[ping] AuthenticationPrincipal = {}.", activeUser.toString());
      if (activeUser.getUsername() != null) {
        log.debug("[ping] User name = {}.", activeUser.getUsername());
      }
    }
     */
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(value = "/error", method = RequestMethod.GET, produces = {MediaType.APPLICATION_XML_VALUE, Nsi.NSI_DDS_V1_XML})
  @ResponseBody
  public ResponseEntity<?> error() throws Exception {
    log.debug("error: Bang!");
    ErrorType errorType = DiscoveryError.getErrorType(DiscoveryError.INTERNAL_SERVER_ERROR, "/error", "Test result");
    errorType.setCode(0);
    JAXBElement<ErrorType> createError = FACTORY.createError(errorType);
    return new ResponseEntity<>(createError, null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Endpoint for incoming DDS document notifications. This endpoint is registered against peer DDS servers.
   *
   * @param host
   * @param encoding
   * @param source
   * @param request
   * @return
   * @throws WebApplicationException
   */
  @RequestMapping(
          value = "/notifications",
          method = RequestMethod.POST,
          consumes = {MediaType.APPLICATION_XML_VALUE, Nsi.NSI_DDS_V1_XML},
          produces = {MediaType.APPLICATION_XML_VALUE, Nsi.NSI_DDS_V1_XML}
  )
  @ResponseBody
  public ResponseEntity<?> notifications(
          @RequestHeader(value = HttpConstants.HOST) String host,
          @RequestHeader(value = HttpConstants.FORWARD, required = false) String source,
          @RequestHeader(
                  value = HttpConstants.ACCEPT_NAME,
                  defaultValue = Nsi.NSI_DDS_V1_XML) String encoding,
          InputStream request) throws WebApplicationException {

    log.info("[notifications] Incoming notification from Host={}, X-Forwarded-For={}, Accept={}", host, source, encoding);

    // Parse the XML into JAXB objects.
    NotificationListType notifications;
    try {
      Object object = XmlUtilities.xmlToJaxb(NotificationListType.class, request);
      if (object instanceof NotificationListType) {
        notifications = (NotificationListType) object;
      } else {
        log.error("[notifications] Unable to parse incoming subscription: {}, {}.", source, encoding);
        WebApplicationException invalidXmlException = Exceptions.invalidXmlException("notifications", "Expected NotificationListType but found " + object.getClass().getCanonicalName());
        log.error("[notifications] Failed to parse incoming notifications.", invalidXmlException);
        throw invalidXmlException;
      }
    } catch (JAXBException | IOException ex) {
      log.error("[notifications] Unable to parse incoming subscription: {}, {}.", source, encoding);
      WebApplicationException invalidXmlException = Exceptions.invalidXmlException("notifications", "Unable to process XML " + ex.getMessage());
      log.error("[notifications] Failed to parse incoming notifications.", invalidXmlException);
      throw invalidXmlException;
    }

    // Make sure this is still a valid subscription otherwise we ignore it
    // and remove the subscriptions on the remote DDS instance via and
    // exception thrown by this lookup.
    if (!ddsProvider.isSubscription(notifications.getHref())) {
      log.error("[notifications] Subscription for incoming notification not found, provider={}, subscriptionId={}, href={}", notifications.getProviderId(), notifications.getId(), notifications.getHref());
      throw Exceptions.doesNotExistException(DiscoveryError.SUBCRIPTION_DOES_NOT_EXIST, "id", notifications.getId());
    }

    log.debug("[notifications] provider={}, subscriptionId={}, href={}", notifications.getProviderId(), notifications.getId(), notifications.getHref());
    for (NotificationType notification : notifications.getNotification()) {
      log.debug("[notifications] processing notification event=" + notification.getEvent() + ", documentId=" + notification.getDocument().getId());
      try {
        ddsProvider.processNotification(notification);
      } catch (WebApplicationException ex) {
        log.error("[notifications] Unable to process incoming subscription, id = {}.", notifications.getId());
        WebApplicationException internalServerErrorException = Exceptions.internalServerErrorException("notifications", "failed to process notification for documentId=" + notification.getDocument().getId());
        log.error("[notifications] Failed to process notification for documentId = {}", notification.getDocument().getId(), ex);
        throw internalServerErrorException;
      }
    }

    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }

  /**
   * Get all documents associated with this DDS instance filtered by nsa, type, or id.
   *
   * @param id
   * @param nsa
   * @param type
   * @param summary
   * @param ifModifiedSince
   * @return
   */
  @RequestMapping(
          value = "/documents",
          method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_XML_VALUE, Nsi.NSI_DDS_V1_XML}
  )
  @ResponseBody
  public ResponseEntity<?> getDocuments(
          @RequestHeader(value = HttpConstants.IF_MODIFIED_SINCE_NAME, required = false) String ifModifiedSince,
          @RequestParam(value = "nsa", required = false) String nsa,
          @RequestParam(value = "type", required = false) String type,
          @RequestParam(value = "id", required = false) String id,
          @RequestParam(value = "summary", defaultValue = "false") boolean summary) {

    log.debug("getDocuments: nsa={}, type{}, id={}, summary={}, If-Modified-Since={}", nsa, type, id, summary, ifModifiedSince);

    long lastDiscovered = 0;
    if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
      lastDiscovered = DateUtils.parseDate(ifModifiedSince).getTime();
    }

    Collection<Document> documents = ddsProvider.getDocuments(nsa, type, id, lastDiscovered);

    long discovered = 0;
    DocumentListType results = FACTORY.createDocumentListType();
    if (documents.size() > 0) {
      for (Document document : documents) {
        if (discovered < document.getLastDiscovered()) {
          discovered = document.getLastDiscovered();
        }

        // Check if only the document meta data is required and not
        // the document contents.
        try {
          if (summary) {
            results.getDocument().add(document.getDocumentSummary());
          } else {
            results.getDocument().add(document.getDocumentFull());
          }
        } catch (IOException | JAXBException ex) {
          WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents", "Unable to format XML response " + ex.getMessage());
          log.error("getDocuments: Failed to format outgoing response.", invalidXmlException);
          throw invalidXmlException;
        }
      }
    } else {
      log.debug("getDocuments: zero results to query nsa={}, type{}, id={}, summary={}, If-Modified-Since={}", nsa, type, id, summary, ifModifiedSince);
    }

    // Now we need to determine what "Last-Modified" date we send back.
    final HttpHeaders headers = new HttpHeaders();
    if (results.getDocument().size() > 0) {
      headers.setLastModified(discovered);
    }

    String encoded;
    try {
      encoded = DdsParser.getInstance().documents2Xml(results);
    } catch (JAXBException | IOException ex) {
      WebApplicationException invalidXmlException = Exceptions.invalidXmlException("/documents", "Unable to format XML response " + ex.getMessage());
      log.error("getDocuments: Failed to format outgoing response.", invalidXmlException);
      throw invalidXmlException;
    }

    return new ResponseEntity<>(encoded, headers, HttpStatus.OK);
  }
}
