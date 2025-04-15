/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.sense.rm.driver.nsi.cs;

import static java.util.Comparator.comparing;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.common.base.Strings;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.soap.SOAPFaultException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.cs.lib.Client;
import net.es.nsi.cs.lib.ClientUtil;
import net.es.nsi.cs.lib.CsParser;
import net.es.nsi.cs.lib.Helper;
import net.es.nsi.cs.lib.NsiHeader;
import net.es.nsi.cs.lib.SimpleLabel;
import net.es.nsi.cs.lib.SimpleStp;
import net.es.nsi.dds.lib.jaxb.nsa.InterfaceType;
import net.es.nsi.dds.lib.jaxb.nsa.NsaType;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
import net.es.sense.rm.driver.nsi.cs.api.CsUtils;
import net.es.sense.rm.driver.nsi.cs.api.QuerySummary;
import net.es.sense.rm.driver.nsi.cs.db.ConnectionMap;
import net.es.sense.rm.driver.nsi.cs.db.ConnectionMapService;
import net.es.sense.rm.driver.nsi.cs.db.DeltaConnection;
import net.es.sense.rm.driver.nsi.cs.db.DeltaMapRepository;
import net.es.sense.rm.driver.nsi.cs.db.Operation;
import net.es.sense.rm.driver.nsi.cs.db.OperationMapRepository;
import net.es.sense.rm.driver.nsi.cs.db.OperationType;
import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import net.es.sense.rm.driver.nsi.cs.db.StateType;
import net.es.sense.rm.driver.nsi.cs.db.StpMapping;
import net.es.sense.rm.driver.nsi.dds.api.DocumentReader;
import net.es.sense.rm.driver.nsi.mrml.IllegalArgumentExceptionSupplier;
import net.es.sense.rm.driver.nsi.mrml.MrsBandwidthService;
import net.es.sense.rm.driver.nsi.mrml.MrsBandwidthType;
import net.es.sense.rm.driver.nsi.mrml.MrsUnits;
import net.es.sense.rm.driver.nsi.mrml.NmlExistsDuring;
import net.es.sense.rm.driver.nsi.mrml.NmlLabel;
import net.es.sense.rm.driver.nsi.mrml.NotFoundExceptionSupplier;
import net.es.sense.rm.driver.nsi.mrml.StpHolder;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import net.es.sense.rm.driver.schema.Mrs;
import net.es.sense.rm.driver.schema.Nml;
import net.es.sense.rm.driver.schema.Sd;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.ogf.schemas.nsi._2013._12.connection.provider.Error;
import org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.GenericRequestType;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ProvisionStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.QuerySummaryConfirmedType;
import org.ogf.schemas.nsi._2013._12.connection.types.QueryType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationRequestCriteriaType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveResponseType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveType;
import org.ogf.schemas.nsi._2013._12.connection.types.ScheduleType;
import org.ogf.schemas.nsi._2013._12.framework.headers.CommonHeaderType;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;
import org.ogf.schemas.nsi._2013._12.services.types.DirectionalityType;
import org.ogf.schemas.nsi._2013._12.services.types.TypeValueType;
import org.springframework.stereotype.Component;

/**
 * A provider implementing MRML delta operations using the NSI Connection Service.
 * This class maps MRML topologies changes to NSI connection service operations.
 * This class acts in the ConnectionRequester role, initiating NSI-CS operations
 * to the remote NSA, then blocking on a semaphore until the asynchronous response
 * is returned to the receiving NSI-CS SOAP endpoint.
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class CsProvider {
  private final NsiProperties nsiProperties; // List of application properties loaded from YAML file.
  private final ConnectionMapService connectionMapService;
  private final OperationMapRepository operationMap;
  private final DeltaMapRepository deltaMap;
  private final ReservationService reservationService;
  private final SpringExtension springExtension;
  private final NsiActorSystem nsiActorSystem;
  private final DocumentReader documentReader;

  private ActorRef connectionActor;

  /**
   * Initialize the connection service provider with autowired parameters.
   *
   * @param nsiProperties
   * @param connectionMapService
   * @param operationMap
   * @param deltaMap
   * @param reservationService
   * @param springExtension
   * @param nsiActorSystem
   */
  public CsProvider(NsiProperties nsiProperties, ConnectionMapService connectionMapService,
                    OperationMapRepository operationMap, DeltaMapRepository deltaMap,
                    ReservationService reservationService, SpringExtension springExtension,
                    NsiActorSystem nsiActorSystem, DocumentReader documentReader) {
    this.nsiProperties = nsiProperties;
    this.connectionMapService = connectionMapService;
    this.operationMap = operationMap;
    this.deltaMap = deltaMap;
    this.reservationService = reservationService;
    this.springExtension = springExtension;
    this.nsiActorSystem = nsiActorSystem;
    this.documentReader = documentReader;
  }

  // Object factories for creating NSI objects for JAXB manipulation.
  private static final org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory CS_FACTORY
          = new org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory();
  private static final org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory P2PS_FACTORY
          = new org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory();

  /**
   * Initialize the NSI connection service system and load the remote NSA's
   * connection list to build initial topology model.
   */
  public void start() {
    // Initialize the actors.
    log.info("[CsProvider] Starting NSI CS system...");
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();

    try {
      connectionActor = actorSystem.actorOf(springExtension.props("connectionActor"), "nsi-connectionActor");
    } catch (Exception ex) {
      log.error("[CsProvider] Failed to initialize actor", ex);
    }

    // Do a one time load of connections from remote NSA since the web
    // server may not yet be servicing requests.  From this point on it is
    // controlled through the asynchronous process.
    load();

    log.info("[CsProvider] Completed NSI CS system initialization.");
  }

  /**
   * Configure the NSI-CS client based on either static URL or dynamic data from NSI-DDS.
   *
   * @return The new NSI-CS client.
   */
  private ClientUtil getNsiClient() {
    String url = nsiProperties.getProviderConnectionURL();
    if (Strings.isNullOrEmpty(url)) {
      // If a manual override for the URL was not specified then we
      // will need to learn the URL from the NSI-DDS.
      NsaType doc = documentReader.getNsa(nsiProperties.getProviderNsaId());
      url = doc.getInterface().stream()
          .filter(f -> f.getType().equalsIgnoreCase(Nsi.NSI_CS_PROVIDER_V2))
          .map(InterfaceType::getHref)
          .findFirst()
          .orElse(null);
    }
    return new ClientUtil(url);
  }

  /**
   * Method to determine if the providerNSA supports modification of reservations.
   *
   * @return True if the providerNSA supports modification, false otherwise.
   */
  private boolean supportModify() {
    return documentReader.getNsa(nsiProperties.getProviderNsaId()).getFeature().stream()
        .anyMatch(f -> f.getType().equalsIgnoreCase(Nsi.NSI_CS_MODIFY));
  }

  /**
   * Returns the connection actor.
   *
   * @return
   */
  public ActorRef GetConnectionActor() {
    return connectionActor;
  }

  /**
   * Terminate the connection actor.
   */
  public void terminate() {
    nsiActorSystem.shutdown(connectionActor);
  }

  /**
   * Load all NSA connections into the database using the synchronous querySummarySync
   * operation.
   */
  public void load() {
    Client nsiClient = new Client(nsiProperties.getProviderConnectionURL());
    Holder<CommonHeaderType> header = getNsiCsHeader();
    QueryType query = CS_FACTORY.createQueryType();
    try {
      log.info("[load] Sending querySummarySync: providerNSA = {}, correlationId = {}",
              header.value.getProviderNSA(), header.value.getCorrelationId());
      QuerySummaryConfirmedType querySummarySync = nsiClient.getProxy().querySummarySync(query, header);
      log.info("[load] QuerySummaryConfirmed received, providerNSA = {}, correlationId = {}",
              header.value.getProviderNSA(), header.value.getCorrelationId());

      QuerySummary q = new QuerySummary(nsiProperties.getNetworkId(), reservationService);
      q.process(querySummarySync, header);
    } catch (Error ex) {
      log.error("[load] querySummarySync exception on operation - {} {}",
              ex.getFaultInfo().getServiceException().getErrorId(),
              ex.getFaultInfo().getServiceException().getText());
    } catch (org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException ex) {
      log.error("[load] querySummarySync exception processing - {} {}",
              ex.getFaultInfo().getErrorId(),
              ex.getFaultInfo().getText());
    } catch (SOAPFaultException ex) {
      log.error("[load] querySummarySync SOAPFaultException exception", ex);
    } catch (Exception ex) {
      log.error("[load] querySummarySync exception processing results", ex);
    }
  }

  /**
   * Process delta reduction and addition requests up to the pre-commit state,
   * storing connection information for commit processing.  We are forgiving
   * of reduction elements not being present, but strict about the creation
   * for addition elements.
   *
   * @param originalModel The original model referred to by the delta request.
   * @param updatedModel The updated model containing the delta modifications.
   * @param deltaId The delta identifier being processed.
   * @param reduction The delta reduction model.
   * @param addition The delta addition model.
   * @throws Exception A failure to process the delta has occurred.
   */
  public void processDelta(OntModel originalModel, OntModel updatedModel, String deltaId,
          Optional<OntModel> reduction, Optional<OntModel> addition) throws Exception {
    log.debug("[processDelta] start deltaId = {}", deltaId);

    // We will need to keep a map of the NSI-CS connection we make for this delta.
    DeltaConnection connectionIds = new DeltaConnection();
    connectionIds.setDeltaId(deltaId);

    // The SENSE-N-RM maps delta requests onto NSI-CS reservations, so a reduction is either
    // terminating an NSI-CS reservation by removing it from the model, modifying reservation
    // parameters, or modifying locally stored metadata associated with the reservation.  A
    // delta addition can result in a new NSI-CS reservation, a modification to an existing
    // reservation, or a modification to locally stored metadata associated with the reservation.

    // First up we handle the mrs:SwitchingSubnet deletions that result in NSI-CS reservation
    // terminations.
    if (reduction.isPresent()) {
      log.debug("[processDelta] processing reduction, deltaId = {}", deltaId);

      // Get the URI for any mrs:SwitchingSubnet that are to be terminated by this reduction.
      List<String> terminates = getReductionTerminates(originalModel, updatedModel, reduction.get());

      // Now we terminate the NSI-CS reservations associated with the mrs:SwitchingSubnet URI.
      List<String> terminateIds = processTerminates(terminates);

      // Add the connectionIds we need to terminate for processing on the delta commit.
      connectionIds.getTerminates().addAll(terminateIds);
      terminateIds.forEach(t -> log.debug("[processDelta] deltaId = {} will terminate uri = {}", deltaId, t));
      log.debug("[processDelta] done processing reduction, deltaId = {}", deltaId);
    }

    // Now handle the mrs:SwitchingSubnet additions that result in new NSI-CS reservations.
    if (addition.isPresent()) {
      log.debug("[processDelta] processing addition, deltaId = {}", deltaId);
      // This is a list of cid associated with reservations created
      // as part of the delta addition.
      List<String> commits = new ArrayList<>();

      // Get the list of mrs:SwitchingSubnet URI that need to be added to the model.
      List<String> creates = getAdditionCreates(originalModel, updatedModel, addition.get());
      creates.forEach(c -> log.debug("[processDelta] creating SwitchingSubnet = {}", c));

      // Now we convert these new mrs:SwitchingSubnet to NSI-CS reservations.
      List<String> newCid = processNewSwitchingSubnet(deltaId, updatedModel, creates, commits);

      // The list of NSI operation correlationId for which we will need to block on
      // results.
      List<String> correlationIds = new ArrayList<>(newCid);
      log.debug("[processDelta] done processing addition, deltaId = {}", deltaId);

      // Find the SwitchingSubnet being modified based on the BandwidthService and existDuring changes.
      Set<Resource> modifies =
          CsProvider.getModifiedSwitchingSubnets(originalModel, updatedModel, addition.get());
      modifies.forEach(c -> log.debug("[processDelta] modified SwitchingSubnet = {}", c.getURI()));

      // Now we process the modified SwitchingSubnet by sending NSI-CS reservation modifications.
      List<String> modCid = processModifiedSwitchingSubnet(deltaId, updatedModel, modifies, commits);
      correlationIds.addAll(modCid);
      log.debug("[processDelta] done processing modification, deltaId = {}", deltaId);

      // We track the connectionId for the NSI-CS reserve operations we will need to commit.
      connectionIds.getCommits().addAll(commits);

      // Wait for our outstanding reserve operations to complete (or fail).
      // We are expecting an asynchronous reserveConfirm in response.
      waitForOperations(correlationIds);
    }

    // Store the list of connection ids we will need to handle during commit phase.
    deltaMap.store(connectionIds);
    log.debug("[processDelta] done processing deltaId = {}", deltaId);
  }

  /**
   * This method returns the list of mrs:SwitchingSubnet URI that need to be removed
   * from the model.
   *
   * The SENSE-N-RM maps delta requests onto NSI-CS reservations, so a reduction is either
   * terminating an NSI-CS reservation by removing it from the model, modifying reservation
   * parameters, or modifying locally stored metadata associated with the reservation.
   *
   * @param original The original ontology corresponding to the modelId.
   * @param updated The ontology resulting from the delta being applied.
   * @param reduction The reduction model from the delta request.
   * @return List of mrs:SwitchingSubnet URI to terminate.
   */
  public static List<String> getReductionTerminates(OntModel original, OntModel updated, OntModel reduction) {
    // Handle mrs:SwitchingSubnet removal by getting each subject of type mrs:SwitchingSubnet
    // from the reduction and determine if it is in the updated model.  If it is not then we
    // must terminate the associated reservation, otherwise leave it for the modification
    // phase.
    List<String> terminate = new ArrayList<>();
    for (Resource subject : ModelUtil.getSubjects(reduction)) {
      // Verify that the subject of the reduction is in the target model, if not we have an error.
      Resource target = Optional.ofNullable(original.getResource(subject.getURI())).orElseThrow(
          new NotFoundExceptionSupplier(String.format("subject %s not found in ontology model", subject.getURI())));

      // This subject is a mrs:SwitchingSubnet so see if it needs to be terminated.
      if (ModelUtil.isSwitchingSubnet(target) &&
          ModelUtil.getResourceOfSubjectAndType(updated, Mrs.SwitchingSubnet, target.getURI()) == null) {
        // The mrs:SwitchingSubnet is not present in the new model so add it for termination.
        terminate.add(target.getURI());
      }
    }

    return terminate;
  }

  /**
   * This method returns the list of mrs:SwitchingSubnet URI that need to be added
   * to the model.
   *
   * @param original The original MRML model to which the delta addition is to be applied.
   * @param updated The updated MRML model that has had both the reduction and addition applied.
   * @param addition The delta addition.
   * @return List of strings representing the URI of mrs:SwitchingSubnet that must be added.
   */
  public static List<String> getAdditionCreates(OntModel original, OntModel updated, OntModel addition) {
    // Handle mrs:SwitchingSubnet addition by getting each subject of type mrs:SwitchingSubnet
    // from the addition and determine if it is in the updated model but not in the original
    // model indicating a new NSI-CS reservation must be made.
    List<String> creates = new ArrayList<>();
    for (Resource subject : ModelUtil.getSubjects(addition)) {
      // Verify that the subject of the addition is not in the original model but is in the
      // updated model.
      if (ModelUtil.getResourceOfSubjectAndType(original, Mrs.SwitchingSubnet, subject.getURI()) == null) {
        // Looks like this is a new subject so check to see if it is a mrs:SwitchingSubnet.
        if (ModelUtil.getResourceOfSubjectAndType(updated, Mrs.SwitchingSubnet, subject.getURI()) != null) {
          // The mrs:SwitchingSubnet is present in the new model so add it for creation.
          creates.add(subject.getURI());
        }
      }
    }

    return creates;
  }

  /**
   * This method returns the list of resources of specified type that are modifications
   * to existing model resources. It calculates this by determining of the referenced resource
   * is in both the original and updated models.  Added or terminated resources would not meet
   * this condition.
   *
   * @param original The original model.
   * @param updated The updated model.
   * @param addition The addition model.
   * @param type The type of resource to match.
   * @return The list of resources being modified of the specified type.
   */
  public static Set<Resource> getModifiedResource(OntModel original, OntModel updated,
                                                      OntModel addition, Resource type) {
    Set<Resource> results = new LinkedHashSet<>();
    for (Resource subject : ModelUtil.getSubjects(addition)) {
      // Verify that the subject of the addition is in both the original model and updated model.
      if (original.containsResource(subject) && updated.containsResource(subject)) {
        // This resource exists in both the original and new models.  Now we filter
        // based on resource type we support for modification.
        Resource bs = ModelUtil.getResourceOfSubjectAndType(updated, type, subject);
        if (bs != null) {
          results.add(bs);
        }
      }
    }

    return results;
  }

  /**
   * This class determines the list of mrs:SwitchingService that need to be
   * modified as part of the delta request.
   *
   * @param original The original model.
   * @param updated The original model updated with the delta.
   * @param addition The delta addition.
   * @return The list of modified mrs:SwitchingService resources from the updated model.
   */
  public static Set<Resource> getModifiedSwitchingSubnets(OntModel original, OntModel updated, OntModel addition) {
    Set<Resource> switchingSubnets = new LinkedHashSet<>();

    // Look for modified Mrs.BandwidthService resources.
    Set<Resource> bandwidthServices = getModifiedResource(original, updated, addition, Mrs.BandwidthService);
    for (Resource resource : bandwidthServices) {
      if (ModelUtil.isBandwidthService(resource)) {
        //Get the port this bandwidth service is associated with.
        Resource belongsTo = ModelUtil.getNmlBelongsTo(resource);
        if (belongsTo != null && ModelUtil.isBidirectionalPort(belongsTo)) {
          // Now we need to find the SwitchingSubnet this port is associated with.  We do not have
          // a direct relationship modelled in the resource.
          List<Resource> subjects = ModelUtil.getSubjectsOfPredicateRelationship(updated, Nml.hasBidirectionalPort, belongsTo);
          for (Resource switchingSubnet: subjects) {
            if (ModelUtil.isSwitchingSubnet(switchingSubnet)) {
              switchingSubnets.add(switchingSubnet);
            }
          }
        }
      }
    }

    // Look for modified Nml.existsDuring resources.
    Set<Resource> existsDuring = getModifiedResource(original, updated, addition, Nml.existsDuring);
    for (Resource resource : existsDuring) {
      if (ModelUtil.isExistsDuring(resource)) {
        List<Resource> ss = ModelUtil.getAllSwitchingSubnetWithExistsDuring(updated, resource);
        switchingSubnets.addAll(ss);
      }
    }

    return switchingSubnets;
  }

  /**
   * Determine the set of NSI connections that must be removed as part of this
   * delta, leaving the termination operation for the commit.  We only process
   * reductions that result in NSI-CS connection termination here.
   *
   * @param terminates Contains the list of mrs:SwitchingSubnet URI to terminate.
   * @return A list of connections that need to be terminated.
   */
  private List<String> processTerminates(List<String> terminates) {
    log.debug("[processTerminates] start");

    // The list of connection ids we will need terminate in the delta commit.
    List<String> terminateIds = new ArrayList<>();
    terminates.forEach(ssid -> {
      // The SwitchingSubnet identifier is the global reservation identifier in
      // associated NSI connections.  For now, we only support the removal of
      // a complete SwitchingSubnet, and not individual ports/vlans.
      log.debug("[processTerminates] terminating SwitchingSubnet: " + ssid);

      // Look up all the reservation segments associated with this SwitchingSubnet.
      // TODO: We can ignore any in the TERMINATED but due to an OpenNSA bug we let
      // them try a reduction on a reservation in the TERMINATING state.
      List<Reservation> reservations = reservationService.getByGlobalReservationId(ssid).stream()
              .filter(r -> (LifecycleStateEnumType.TERMINATED != r.getLifecycleState()))
              .toList();

      // Terminate using the parent connectionId if one exists.  The NSI-CS terminate
      // message will be sent on the delta commit.
      for (Reservation reservation : reservations) {
        if (Strings.isNullOrEmpty(reservation.getParentConnectionId())) {
          terminateIds.add(reservation.getConnectionId());
        } else {
          terminateIds.add(reservation.getParentConnectionId());
        }
      }

      log.debug("[processTerminates] done");
    });

    return terminateIds;
  }

  /**
   * Process the MRML delta addition elements that are requesting a new SwitchingSubnet.
   *
   * @param deltaId The deltaId value for this delta request.
   * @param model The current MRML model with the addition applied.
   * @param uris The list of mrsSwitchingSubnet URI to create NSI-CS reservations.
   * @param commits The list of NSI-CS correlationIds that will need to be committed after reserveConfirmed received.
   * @return List of correlationIds for NSI-CS operations issued to uPA.
   * @throws DatatypeConfigurationException
   * @throws ServiceException
   * @throws IllegalArgumentException deltaId, model, uris, commits
   */
  private List<String> processNewSwitchingSubnet(String deltaId, OntModel model, List<String> uris, List<String> commits)
      throws DatatypeConfigurationException, ServiceException, IllegalArgumentException {

    log.info("[processNewSwitchingSubnet] processing addition for delta {}:\n{}",
        deltaId, model.getBaseModel().toString());

    // We return list of correlationId from the NSI-CS reservation requests created.
    List<String> correlationIds = new ArrayList<>();

    // We will treat each mrs:SwitchingSubnet as an independent reservation in NSI.
    for (String uri : uris) {
      log.debug("[processNewSwitchingSubnet] adding SwitchingSubnet: {}", uri);

      Resource switchingSubnet = ModelUtil.getResourceOfSubjectAndType(model, Mrs.SwitchingSubnet, uri);
      if (switchingSubnet == null) {
        log.error("[processNewSwitchingSubnet] switchingSubnet {} not found in model.", uri);
        continue;
      }

      // Get the existDuring lifetime object if it exists, so we can model a schedule.
      NmlExistsDuring ssExistsDuring = getNmlExistsDuring(switchingSubnet);;

      // We need the associated parent SwitchingService resource to determine
      // the ServiceDefinition that holds the serviceType.
      Statement belongsTo = switchingSubnet.getProperty(Nml.belongsTo);
      Resource switchingServiceRef = belongsTo.getResource();
      log.debug("[processNewSwitchingSubnet] SwitchingServiceRef: {}", switchingServiceRef.getURI());

      // Get the full SwitchingService definition from the merged model.
      Resource switchingService = ModelUtil.getResourceOfSubjectAndType(model, Nml.SwitchingService, switchingServiceRef);
      Optional.ofNullable(switchingService)
          .orElseThrow(new NotFoundExceptionSupplier("Could not find referenced switching service "
              + switchingServiceRef.getURI()));
      log.debug("[processNewSwitchingSubnet] found SwitchingService: {}", switchingService.getURI());

      // Now we need the ServiceDefinition associated with this SwitchingService.
      Statement hasServiceDefinition = switchingService.getProperty(Sd.hasServiceDefinition);
      Resource serviceDefinitionRef = hasServiceDefinition.getResource();
      log.debug("[processNewSwitchingSubnet] looking for serviceDefinitionRef: " + serviceDefinitionRef.getURI());

      // Get the full ServiceDefinition definition from the merged model.
      Resource serviceDefinition = ModelUtil.getResourceOfSubjectAndType(model, Sd.ServiceDefinition, serviceDefinitionRef);
      Optional.ofNullable(serviceDefinition)
          .orElseThrow(new IllegalArgumentExceptionSupplier("Could not find service definition in model for "
              + serviceDefinitionRef.getURI()));
      log.debug("[processNewSwitchingSubnet] found ServiceDefinition: {}", serviceDefinition.getURI());

      // Find the serviceType associated with this serviceDefinition.
      Statement serviceTypeRef = serviceDefinition.getProperty(Sd.serviceType);
      log.debug("[processNewSwitchingSubnet] requesting serviceType: {}", serviceTypeRef.getString());

      // Verify we support the requested serviceType mapping.
      String serviceType = serviceTypeRef.getString();
      if (!Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(serviceType) &&
          !Nsi.NSI_SERVICETYPE_L2_LB_ES.equalsIgnoreCase(serviceType)) {
        log.error("[processNewSwitchingSubnet] serviceType not supported {}", serviceTypeRef.getString());
        throw new IllegalArgumentException("serviceType not supported " + serviceTypeRef.getString());
      }

      // Now we process the NSI reservation request if it is for an EVTS p2p service.
      List<StpHolder> stps = new ArrayList<>();

      // Find the ports that are part of this SwitchingSubnet and build NSI STP
      // identifiers for the service.  We loop through each of the (two) bidirectional
      // ports associated with the SwitchingSubnet.
      StmtIterator listProperties = switchingSubnet.listProperties(Nml.hasBidirectionalPort);
      while (listProperties.hasNext()) {
        Statement hasBidirectionalPort = listProperties.next();
        Resource biRef = hasBidirectionalPort.getResource();
        log.debug("[processNewSwitchingSubnet] bi member: {}", biRef.getURI());

        // Lookup in the addition model the bidirectional port from the hasBidirectionalPort reference.
        Resource biChild = ModelUtil.getResourceOfSubjectAndType(model, Nml.BidirectionalPort, biRef);
        if (biChild == null) {
          log.error("[processNewSwitchingSubnet] Requested BidirectionalPort does not exist {}", biRef.getURI());
          throw new IllegalArgumentException("Requested BidirectionalPort does not exist " + biRef.getURI());
        }

        log.debug("[processNewSwitchingSubnet] biChild: {}", biChild.getURI());

        MrsBandwidthService bws = new MrsBandwidthService(biChild, model);

        log.debug("[processNewSwitchingSubnet] BandwidthService: {}", bws.getId());
        log.debug("[processNewSwitchingSubnet] type: {}", bws.getBandwidthType());
        log.debug("[processNewSwitchingSubnet] maximumCapacity: {} {}", bws.getMaximumCapacity(), bws.getUnit());
        log.debug("[processNewSwitchingSubnet] maximumCapacity: {} mbps", MrsUnits.normalize(bws.getMaximumCapacity(),
            bws.getUnit(), MrsUnits.mbps));

        // The "guaranteedCapped" BandwidthService maps to the NSI_SERVICETYPE_EVTS service, so
        // we need to verify this is a valid request.  We will map bestEffort to this as well right
        // now but should be changed to a dedicated service in the future.
        if (MrsBandwidthType.guaranteedCapped != bws.getBandwidthType() &&
            MrsBandwidthType.bestEffort != bws.getBandwidthType()) {
          String error = "Requested BandwidthService type = " + bws.getBandwidthType() +
              " not supported by SwitchingService = " + switchingService.getURI() +
              " on portId = " + biRef.getURI();
          log.error("[processNewSwitchingSubnet] {}.", error);
          throw new IllegalArgumentException(error);
        }

        // Now determine if there is an independent existsDuring object.
        String childExistsDuringId = ssExistsDuring.getId();
        Optional<Statement> existsDuring = Optional.ofNullable(biChild.getProperty(Nml.existsDuring));
        if (existsDuring.isPresent()) {
          Resource childExistsDuringRef = existsDuring.get().getResource();
          log.debug("[processNewSwitchingSubnet] childExistsDuringRef: " + childExistsDuringRef.getURI());
          if (!ssExistsDuring.getId().contentEquals(childExistsDuringRef.getURI())) {
            // We have a different existsDuring reference than our SwitchingSubnet.
            childExistsDuringId = childExistsDuringRef.getURI();
          }
        }

        // Now get the label for this port.
        Statement labelRef = biChild.getProperty(Nml.hasLabel);
        Resource label = ModelUtil.getResourceOfSubjectAndType(model, Nml.Label, labelRef.getResource());

        // Make sure we have a valid label.
        if (label == null) {
          log.error("[processNewSwitchingSubnet] biChild labelRef missing label: {}", labelRef);

          // Do some debugging.
          Resource resource = model.getBaseModel().getResource(labelRef.getResource().getURI());
          log.error("[processNewSwitchingSubnet] did we find the label object?: {}", resource);
          log.error("[processNewSwitchingSubnet] Label dump: {}", Nml.Label);
          ModelUtil.findInstancesByType(model,Nml.Label)
              .forEach(r -> log.error("[processNewSwitchingSubnet] {}", r.getURI()));
          throw new IllegalArgumentException("[processNewSwitchingSubnet] biChild labelRef missing label: " + labelRef);
        }

        NmlLabel nmlLabel = new NmlLabel(label);
        SimpleLabel simpleLabel = nmlLabel.getSimpleLabel();

        // Locate the parent biDirectional port to the requested child port.
        Resource parentBi = ModelUtil.getParentBidirectionalPort(model, biChild);
        if (parentBi == null) {
          log.error("[processNewSwitchingSubnet] parentBi resource missing for biChild " + biChild);
          throw new IllegalArgumentException("[processNewSwitchingSubnet] parentBi resource missing: " + biChild);
        }

        log.debug("[processNewSwitchingSubnet] parentBi: " + parentBi.getURI());

        // Construct the target STP for this port and label.
        SimpleStp stp = new SimpleStp(parentBi.getURI(), simpleLabel);
        log.debug("[processNewSwitchingSubnet] stpId: {}", stp.getStpId());

        // Get the mrs:tag property if one exists against the nml:BidirectionalPort object.
        String biChildTag = ModelUtil.getMrsTag(biChild);
        if (Strings.isNullOrEmpty(biChildTag)) {
          // If the mrs:tag does not exist in the model then add the NSI STP value.
          biChildTag = stp.getStpId();
        }

        // Store this STP in the list as we will want two of them.
        stps.add(new StpHolder(biChild.getURI(), stp, biChildTag, bws, childExistsDuringId, label.getURI()));
      }

      // We need exactly two ports for our point-to-point connection.
      if (stps.size() != 2) {
        log.error("[processNewSwitchingSubnet] SwitchingSubnet contained {} ports.", stps.size());
        throw new IllegalArgumentException("SwitchingSubnet contained incorrect number of ports (" + stps.size() + ").");
      }

      // Populate the NSI CS message with p2ps service.
      StpHolder src = stps.get(0);
      StpHolder dst = stps.get(1);

      // Normalize bandwidth to mbps for the P2PS request.
      long srcBw = MrsUnits.normalize(src.getBw().getMaximumCapacity(), src.getBw().getUnit(), MrsUnits.mbps);
      long dstBw = MrsUnits.normalize(dst.getBw().getMaximumCapacity(), dst.getBw().getUnit(), MrsUnits.mbps);
      long capacity = Math.min(srcBw, dstBw);

      // Create and populate a P2PS service structure for the NSI-CS request.
      P2PServiceBaseType p2ps = P2PS_FACTORY.createP2PServiceBaseType();
      p2ps.setCapacity(capacity);
      p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
      p2ps.setSymmetricPath(Boolean.TRUE);
      p2ps.setSourceSTP(src.getStp().getStpId());
      p2ps.setDestSTP(dst.getStp().getStpId());

      // Base the reservation off of the specified existsDuring criteria.
      ScheduleType sch = CS_FACTORY.createScheduleType();
      XMLGregorianCalendar paddedStart = ssExistsDuring.getPaddedStart();

      // If the start time is not present then we start now.
      if (paddedStart != null) {
        sch.setStartTime(CS_FACTORY.createScheduleTypeStartTime(paddedStart));
      }

      // If the end time does not exist then it is an infinite reservation, otherwise use the specified time.
      if (ssExistsDuring.getEnd() != null) {
        sch.setEndTime(CS_FACTORY.createScheduleTypeEndTime(ssExistsDuring.getEnd()));
      }

      // Populate the NSI-CS reservation request criteria and make this version 0 of the reservation.
      ReservationRequestCriteriaType rrc = CS_FACTORY.createReservationRequestCriteriaType();
      rrc.setVersion(0);
      rrc.setSchedule(sch);
      rrc.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
      rrc.getAny().add(P2PS_FACTORY.createP2Ps(p2ps));

      // Populate the reserve structure with the mrs:switchingSubnet ID as the global reservation ID,
      // and use the associated optional mrs:tag as the reservation description.
      String uniqueId = "deltaId+" + deltaId + ":uuid+" + UUID.randomUUID();
      String tag = ModelUtil.getMrsTag(switchingSubnet);
      if (Strings.isNullOrEmpty(tag)) {
        // No mrs:tag provided so compose our own.
        tag = uniqueId;
      }

      ReserveType r = CS_FACTORY.createReserveType();
      r.setGlobalReservationId(switchingSubnet.getURI());
      r.setDescription(uniqueId);
      r.setCriteria(rrc);

      // TODO: Create and store a reservation in the database with the information we have.
      // TODO: Make sure to consider race conditions with the ACK and Confirmed messages.

      // Now create and store the mapping for this SwitchingSubnet in our local
      // database.  We will need it later to correlate between NSI-CS messages
      // and SENSE MRML.
      ConnectionMap cm = new ConnectionMap();
      cm.setUniqueId(uniqueId);
      cm.setVersion(rrc.getVersion());
      cm.setDeltaId(deltaId);
      cm.setSwitchingSubnetId(switchingSubnet.getURI());
      cm.setExistsDuringId(ssExistsDuring.getId());
      cm.setServiceType(serviceType);
      cm.setTag(tag);

      // Add the MRML port to STP mapping information to the connection map.
      StpMapping smSrc = new StpMapping(src.getStp().getStpId(), src.getTag(), src.getMrsPortId(),
          src.getMrsLabelId(), src.getBw().getId(), src.getNmlExistsDuringId());
      cm.getMap().add(smSrc);

      StpMapping smDst = new StpMapping(dst.getStp().getStpId(), dst.getTag(), dst.getMrsPortId(),
          dst.getMrsLabelId(), dst.getBw().getId(), dst.getNmlExistsDuringId());
      cm.getMap().add(smDst);

      // Store the connection map to the database.
      ConnectionMap stored = connectionMapService.store(cm);

      log.debug("[processNewSwitchingSubnet] stored connectionMap = {}", stored);

      // Now complete the remaining NSI-CS fields and send the message to the uPA.
      Holder<CommonHeaderType> header = getNsiCsHeader();
      String correlationId = header.value.getCorrelationId();

      // Add this to the operation map to track progress asynchronously.  All
      // asynchronously messaging uses the operation map to record progress.
      Operation op = new Operation();
      op.setOperation(OperationType.reserve);
      op.setState(StateType.reserving);
      op.setUniqueId(uniqueId);
      op.setCorrelationId(correlationId);
      operationMap.store(op);

      // Add to the list of correlationIds we will wait to receive a reserveConfirmed.
      correlationIds.add(correlationId);

      // TODO: Create and store a new reservation object in database.
      Reservation newres = createReservation(r);
      newres.setUniqueId(uniqueId);

      // Store this reservation for later reference, and get the new handle for the object.
      newres = reservationService.store(newres);
      if (newres == null) {
        log.error("[processNewSwitchingSubnet] storage of new reservation uniqueId = {} failed", uniqueId);
        throw new IllegalArgumentException("[processNewSwitchingSubnet] storage of new reservation uniqueId "
            + uniqueId + " failed.");
      }

      // Issue the NSI reservation request.
      try {
        log.debug("[processNewSwitchingSubnet] issuing reserve operation correlationId = {}", correlationId);
        ClientUtil nsiClient = getNsiClient();
        ReserveResponseType response = nsiClient.getProxy().reserve(r, header);

        // Update the reservation in the database with returned connectionId.
        if (reservationService.setConnectionId(newres.getId(), response.getConnectionId()) != 1) {
          log.info("[processNewSwitchingSubnet] error updating reservation uniqueId = {}, with cid = {}",
              newres.getId(), response.getConnectionId());
        }

        // Add connectionId to the list we need to commit.
        commits.add(response.getConnectionId());

        log.debug("[processNewSwitchingSubnet] issued reserve operation correlationId = {}, connectionId = {}",
            correlationId, response.getConnectionId());
      } catch (SOAPFaultException ex) {
        //TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        operationMap.delete(correlationIds);
        log.error("[processNewSwitchingSubnet] Failed to send NSI CS reserve message, correlationId = {}, SOAP Fault = {}",
            correlationId, ex.getFault().toString());
        throw ex;
      } catch (ServiceException ex) {
        //TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        operationMap.delete(correlationIds);
        log.error("[processNewSwitchingSubnet] Failed to send NSI CS reserve message, correlationId = {}, errorId = {}, text = {}",
            correlationId, ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
        throw ex;
      }
    }

    log.info("[processNewSwitchingSubnet] processing {} additions for delta {}",
        correlationIds.size(), deltaId);

    // Return the list of connectionId for which we will expect a ReserveConfirmed.
    return correlationIds;
  }

  /**
   * This method returns a fully populated NSI-CS header for inclusion in a SOAP message.
   *
   * @return NSI-CS header.
   */
  private Holder<CommonHeaderType> getNsiCsHeader() {
    Holder<CommonHeaderType> header = new Holder<>();
    header.value = NsiHeader.builder()
        .correlationId(Helper.getUUID())
        .providerNSA(nsiProperties.getProviderNsaId())
        .requesterNSA(nsiProperties.getNsaId())
        .replyTo(nsiProperties.getRequesterConnectionURL())
        .build()
        .getRequestHeaderType();
    return header;
  }

  /**
   *
   * @param r
   * @return
   */
  private Reservation createReservation(ReserveType r) {
    // Build a reservation database object for this reservation.
    Reservation reservation = new Reservation();
    reservation.setProviderNsa(nsiProperties.getProviderNsaId());
    reservation.setGlobalReservationId(r.getGlobalReservationId());
    reservation.setDescription(r.getDescription());
    reservation.setTopologyId(nsiProperties.getNetworkId());
    reservation.setServiceType(r.getCriteria().getServiceType());
    reservation.setStartTime(CsUtils.getStartTime(r.getCriteria().getSchedule().getStartTime()));
    reservation.setEndTime(CsUtils.getEndTime(r.getCriteria().getSchedule().getEndTime()));
    reservation.setReservationState(ReservationStateEnumType.RESERVE_CHECKING);
    reservation.setProvisionState(ProvisionStateEnumType.RELEASED);
    reservation.setLifecycleState(LifecycleStateEnumType.INITIAL);
    reservation.setDataPlaneActive(false);
    reservation.setVersion(0);
    reservation.setDiscovered(System.currentTimeMillis());

    // Now we need to determine the network based on the STP used in the service.
    try {
      CsUtils.serializeP2PS(r.getCriteria().getServiceType(), r.getCriteria().getAny(), reservation);

      if (Strings.isNullOrEmpty(reservation.getTopologyId())) {
        reservation.setTopologyId(nsiProperties.getNetworkId());
      }
    } catch (JAXBException ex) {
      log.error("[CsProvider] createReservation failed for connectionId = {}",
              reservation.getConnectionId(), ex);
    }

    return reservation;
  }

  /**
   * Process the MRML delta addition elements that are requesting a modified SwitchingSubnet.
   *
   * @param deltaId The deltaId value for this delta request.
   * @param model The current MRML model with the addition applied.
   * @param switchingSubnets The list of mrsSwitchingSubnet to modify NSI-CS reservations.
   * @param commits The list of NSI-CS correlationIds that will need to be committed after reserveConfirmed received.
   * @return List of correlationIds for NSI-CS operations issued to uPA.
   * @throws DatatypeConfigurationException
   * @throws ServiceException
   * @throws IllegalArgumentException
   */
  private List<String> processModifiedSwitchingSubnet(
      String deltaId,
      OntModel model,
      Set<Resource> switchingSubnets,
      List<String> commits)
      throws DatatypeConfigurationException, ServiceException, IllegalArgumentException {

    log.info("[processModifiedSwitchingSubnet] processing modification for delta {}", deltaId);

    // We return list of correlationId from the NSI-CS reservation modification requests created.
    List<String> correlationIds = new ArrayList<>();

    // We originally created each mrs:SwitchingSubnet as an independent reservation in NSI.
    for (Resource switchingSubnet : switchingSubnets) {
      log.info("[processModifiedSwitchingSubnet] processing modification for {}", switchingSubnet.getURI());

      // Look up NSI-CS reservation corresponding the mrs:SwitchingSubnet identifier.
      // There could be multiple if we have been modifying the reservation, so we want
      // the newest version.
      ConnectionMap connection = connectionMapService.getNewestBySwitchingSubnetId(switchingSubnet.getURI());
      if (connection == null) {
        // Looks like we encountered an error of some type since there should only be one
        // matching mrs:SwitchingSubnet stored on the connection map.
        log.error("[processModifiedSwitchingSubnet] did not find SwitchingSubnet connection map for {}",
            switchingSubnet.getURI());
        throw new IllegalArgumentException("Multiple connections found for SwitchingSubnet id " + switchingSubnet.getURI() + ".");
      }

      log.debug("[processModifiedSwitchingSubnet] found connection map version {} of {}", connection.getVersion(),
          connection.getSwitchingSubnetId());

      // The mrs:SwitchingSubnet URN is mapped into the global reservation identifier.
      Reservation reservation = reservationService.getByUniqueId(connection.getUniqueId());
      if (reservation == null) {
        // Looks like we encountered an error of some type since there should only be one
        // matching reservation identifier using this mrs:SwitchingSubnet URN.
        log.error("[processModifiedSwitchingSubnet] no reservation found for uniqueId {} for {}",
            connection.getUniqueId(), switchingSubnet.getURI());
        throw new IllegalArgumentException("No reservation found for SwitchingSubnet id " + switchingSubnet.getURI() + ".");
      }

      // Verify version of reservation aligns with connection map.
      if (reservation.getVersion() != connection.getVersion()) {
        log.error("[processModifiedSwitchingSubnet] reservation version {} and connection map version {} for {}",
            reservation.getVersion(), connection.getVersion(), switchingSubnet.getURI());
        throw new IllegalArgumentException("Incompatible versions for " + switchingSubnet.getURI() + ".");
      }

      // Make sure we are in the correct state for a modify operation.
      if (reservation.getReservationState() != ReservationStateEnumType.RESERVE_START) {
        log.error("[processModifiedSwitchingSubnet] ReservationState invalid for modify {} for {}",
            reservation.getReservationState(), switchingSubnet.getURI());
        throw new IllegalArgumentException("ReservationState invalid for modify "
            + reservation.getReservationState() + " for " + switchingSubnet.getURI() + ".");
      } else if (reservation.getLifecycleState() != LifecycleStateEnumType.CREATED) {
        log.error("[processModifiedSwitchingSubnet] LifecycleState invalid for modify {} for {}",
            reservation.getLifecycleState(), switchingSubnet.getURI());
        throw new IllegalArgumentException("LifecycleState invalid for modify "
            + reservation.getLifecycleState() + " for " + switchingSubnet.getURI() + ".");
      }

      // Now we format a modification to the existing NSI-CS reservation.  At the moment
      // we support modification of capacity, startTime, and endTime.

      // Get the existDuring lifetime object if it exists, so we can model a schedule.
      NmlExistsDuring ssExistsDuring = getNmlExistsDuring(switchingSubnet);

      // We need the associated parent SwitchingService resource to determine
      // the ServiceDefinition that holds the serviceType.
      Statement belongsTo = switchingSubnet.getProperty(Nml.belongsTo);
      Resource switchingServiceRef = belongsTo.getResource();
      log.debug("[processModifiedSwitchingSubnet] SwitchingServiceRef: {}", switchingServiceRef.getURI());

      // Get the full SwitchingService definition from the merged model.
      Resource switchingService = ModelUtil.getResourceOfSubjectAndType(model, Nml.SwitchingService, switchingServiceRef);
      Optional.ofNullable(switchingService)
          .orElseThrow(new NotFoundExceptionSupplier("Could not find referenced switching service "
              + switchingServiceRef.getURI()));
      log.debug("[processModifiedSwitchingSubnet] found SwitchingService: {}", switchingService.getURI());

      // Now we need the ServiceDefinition associated with this SwitchingService.
      Statement hasServiceDefinition = switchingService.getProperty(Sd.hasServiceDefinition);
      Resource serviceDefinitionRef = hasServiceDefinition.getResource();
      log.debug("[processModifiedSwitchingSubnet] looking for serviceDefinitionRef: " + serviceDefinitionRef.getURI());

      // Get the full ServiceDefinition definition from the merged model.
      Resource serviceDefinition = ModelUtil.getResourceOfSubjectAndType(model, Sd.ServiceDefinition, serviceDefinitionRef);
      Optional.ofNullable(serviceDefinition)
          .orElseThrow(new IllegalArgumentExceptionSupplier("Could not find service definition in model for "
              + serviceDefinitionRef.getURI()));
      log.debug("[processModifiedSwitchingSubnet] found ServiceDefinition: {}", serviceDefinition.getURI());

      // Find the serviceType associated with this serviceDefinition.
      Statement serviceTypeRef = serviceDefinition.getProperty(Sd.serviceType);
      log.debug("[processModifiedSwitchingSubnet] requesting serviceType: {}", serviceTypeRef.getString());

      // Now we process the NSI reservation request if it is for an EVTS p2p service.
      List<StpHolder> stps = new ArrayList<>();

      // Make sure this is a supported NSI-CS serviceType.
      String serviceType = serviceTypeRef.getString();
      if (!Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(serviceType) &&
          !Nsi.NSI_SERVICETYPE_L2_LB_ES.equalsIgnoreCase(serviceType)) {
        log.error("[processModifiedSwitchingSubnet] serviceType not supported {}", serviceTypeRef.getString());
        throw new IllegalArgumentException("serviceType not supported " + serviceTypeRef.getString());
      }

      // Find the ports that are part of this SwitchingSubnet and build NSI STP
      // identifiers for the service.  We loop through each of the (two) bidirectional
      // ports associated with the SwitchingSubnet.
      StmtIterator listProperties = switchingSubnet.listProperties(Nml.hasBidirectionalPort);
      while (listProperties.hasNext()) {
        Statement hasBidirectionalPort = listProperties.next();
        Resource biRef = hasBidirectionalPort.getResource();
        log.debug("[processModifiedSwitchingSubnet] bi member: {}", biRef.getURI());

        // Lookup in the addition model the bidirectional port from the hasBidirectionalPort reference.
        Resource biChild = ModelUtil.getResourceOfSubjectAndType(model, Nml.BidirectionalPort, biRef);
        if (biChild == null) {
          log.error("[processModifiedSwitchingSubnet] Requested BidirectionalPort does not exist {}", biRef.getURI());
          throw new IllegalArgumentException("Requested BidirectionalPort does not exist " + biRef.getURI());
        }

        log.debug("[processModifiedSwitchingSubnet] biChild: {}", biChild.getURI());

        MrsBandwidthService bws = new MrsBandwidthService(biChild, model);

        log.debug("[processModifiedSwitchingSubnet] BandwidthService: {}", bws.getId());
        log.debug("[processModifiedSwitchingSubnet] type: {}", bws.getBandwidthType());
        log.debug("[processModifiedSwitchingSubnet] maximumCapacity: {} {}", bws.getMaximumCapacity(), bws.getUnit());
        log.debug("[processModifiedSwitchingSubnet] maximumCapacity: {} mbps",
            MrsUnits.normalize(bws.getMaximumCapacity(), bws.getUnit(), MrsUnits.mbps));

        // The "guaranteedCapped" BandwidthService maps to the NSI_SERVICETYPE_EVTS service, so
        // we need to verify this is a valid request.  We will map bestEffort to this as well right
        // now but should be changed to a dedicated service in the future.
        if (MrsBandwidthType.guaranteedCapped != bws.getBandwidthType() &&
            MrsBandwidthType.bestEffort != bws.getBandwidthType()) {
          String error = "Requested BandwidthService type = " + bws.getBandwidthType() +
              " not supported by SwitchingService = " + switchingService.getURI() +
              " on portId = " + biRef.getURI();
          log.error("[processModifiedSwitchingSubnet] {}.", error);
          throw new IllegalArgumentException(error);
        }

        // Now determine if there is an independent existsDuring object.
        String childExistsDuringId = ssExistsDuring.getId();
        Optional<Statement> existsDuring = Optional.ofNullable(biChild.getProperty(Nml.existsDuring));
        if (existsDuring.isPresent()) {
          Resource childExistsDuringRef = existsDuring.get().getResource();
          log.debug("[processModifiedSwitchingSubnet] childExistsDuringRef: " + childExistsDuringRef.getURI());
          if (!ssExistsDuring.getId().contentEquals(childExistsDuringRef.getURI())) {
            // We have a different existsDuring reference than our SwitchingSubnet.
            childExistsDuringId = childExistsDuringRef.getURI();
          }
        }

        // Now get the label for this port.
        Statement labelRef = biChild.getProperty(Nml.hasLabel);
        Resource label = ModelUtil.getResourceOfSubjectAndType(model, Nml.Label, labelRef.getResource());

        // Make sure we have a valid label.
        if (label == null) {
          log.error("[processModifiedSwitchingSubnet] biChild labelRef missing label: " + labelRef);
          throw new IllegalArgumentException("[processModifiedSwitchingSubnet] biChild labelRef missing label: " + labelRef);
        }

        NmlLabel nmlLabel = new NmlLabel(label);
        SimpleLabel simpleLabel = nmlLabel.getSimpleLabel();

        // Locate the parent biDirectional port to the requested child port.
        Resource parentBi = ModelUtil.getParentBidirectionalPort(model, biChild);
        if (parentBi == null) {
          log.error("[processModifiedSwitchingSubnet] parentBi resource missing for biChild " + biChild);
          throw new IllegalArgumentException("[processModifiedSwitchingSubnet] parentBi resource missing: " + biChild);
        }
        log.debug("[processModifiedSwitchingSubnet] parentBi: " + parentBi.getURI());

        // Construct the target STP for this port and label.
        SimpleStp stp = new SimpleStp(parentBi.getURI(), simpleLabel);
        log.debug("[processModifiedSwitchingSubnet] stpId: {}", stp.getStpId());

        // Get the mrs:tag property if one exists against the nml:BidirectionalPort object.
        String biChildTag = ModelUtil.getMrsTag(biChild);
        if (Strings.isNullOrEmpty(biChildTag)) {
          // If the mrs:tag does not exist in the model then add the NSI STP value.
          biChildTag = stp.getStpId();
        }

        // Store this STP in the list as we will want two of them.
        stps.add(new StpHolder(biChild.getURI(), stp, biChildTag, bws, childExistsDuringId, label.getURI()));
      }

      // We need exactly two ports for our point-to-point connection.
      if (stps.size() != 2) {
        log.error("[processModifiedSwitchingSubnet] SwitchingSubnet contained {} ports.", stps.size());
        throw new IllegalArgumentException("SwitchingSubnet contained incorrect number of ports (" + stps.size() + ").");
      }

      // Populate the NSI CS message with p2ps service.
      StpHolder src = stps.get(0);
      StpHolder dst = stps.get(1);

      // Normalize bandwidth to mbps for the P2PS request.
      long srcBw = MrsUnits.normalize(src.getBw().getMaximumCapacity(), src.getBw().getUnit(), MrsUnits.mbps);
      long dstBw = MrsUnits.normalize(dst.getBw().getMaximumCapacity(), dst.getBw().getUnit(), MrsUnits.mbps);
      long capacity = Math.min(srcBw, dstBw);

      // We are going to need the old P2P structure associated with this reservation.
      P2PServiceBaseType originalP2P;
      try {
        originalP2P = CsParser.getInstance().xml2p2ps(reservation.getService());
      } catch (JAXBException ex) {
        log.error("[processModifiedSwitchingSubnet] Could not parse stored P2P structure\n{}",
            reservation.getService(), ex);
        throw new InternalServerErrorException("Could not parse stored P2P structure\n" + reservation.getService());
      }

      // Make sure we are not trying to modify the serviceType.  Not sure if this would
      // ever happen but ...
      if (!serviceType.equalsIgnoreCase(reservation.getServiceType())) {
        log.error("[processModifiedSwitchingSubnet] Attempt to modify serviceType from {} to {}",
            reservation.getServiceType(), serviceType);
        throw new IllegalArgumentException("Attempt to modify serviceType");
      }

      // Make sure the endpoints are the same as well.
      if ((src.getStp().getStpId().equalsIgnoreCase(originalP2P.getSourceSTP()) &&
          dst.getStp().getStpId().equalsIgnoreCase(originalP2P.getDestSTP())) ||
          (src.getStp().getStpId().equalsIgnoreCase(originalP2P.getDestSTP())) &&
              dst.getStp().getStpId().equalsIgnoreCase(originalP2P.getSourceSTP())) {
        log.debug("[processModifiedSwitchingSubnet] endpoint match.");
      } else {
        log.error("[processModifiedSwitchingSubnet] Attempt to modify endpoints src {} to dst {}",
            src.getStp().getStpId(), dst.getStp().getStpId());
        throw new IllegalArgumentException("Attempt to endpoints");
      }

      // Now we determine what parameters have changed.
      // Populate the NSI-CS reservation request criteria and make this version +1 of the reservation.
      ReservationRequestCriteriaType rrc = CS_FACTORY.createReservationRequestCriteriaType();
      rrc.setVersion(reservation.getVersion() + 1);

      // We add the changed P2P elements directly to the <any/> and not embedded in the P2P structure.
      if (originalP2P.getCapacity() != capacity) {
        log.info("[processModifiedSwitchingSubnet] modified capacity value from {} to {}",
            originalP2P.getCapacity(), capacity);
        rrc.getAny().add(P2PS_FACTORY.createCapacity(capacity));
        // <p2p:parameter type=”disruption-hitless”>false</p2p:parameter>
        TypeValueType disruption = new TypeValueType();
        disruption.setType("disruption-hitless");
        disruption.setValue("false");
        rrc.getAny().add(P2PS_FACTORY.createParameter(disruption));

        // Update original P2P elements with new values as well.
        originalP2P.setCapacity(capacity);
      } else {
        log.debug("[processModifiedSwitchingSubnet] capacity match.");
      }

      // Populate schedule information if modified.
      ScheduleType sch = CS_FACTORY.createScheduleType();

      // Handle a change in schedule startTime.
      if (ssExistsDuring.getStart() == null) {
        // Two cases to check for.
        if (reservation.getStartTime() == 0) {
          log.debug("[processModifiedSwitchingSubnet] startTime remains the same.");
        } else {
          // Change in startTime to now.
          log.debug("[processModifiedSwitchingSubnet] we have a modification of startTime to now.");
          JAXBElement<XMLGregorianCalendar> scheduleTypeStartTime = CS_FACTORY.createScheduleTypeStartTime(null);
          scheduleTypeStartTime.setNil(true);
          sch.setStartTime(scheduleTypeStartTime);
        }
      } else {
        if (reservation.getStartTime() != ssExistsDuring.getStart().toGregorianCalendar().getTimeInMillis()) {
          // We have had a change in startTime.
          log.debug("[processModifiedSwitchingSubnet] we have a modification of startTime {}.",
              ssExistsDuring.getStart().toXMLFormat());
          sch.setStartTime(CS_FACTORY.createScheduleTypeStartTime(ssExistsDuring.getPaddedStart()));
        } else {
          log.debug("[processModifiedSwitchingSubnet] startTime remains the same.");
        }
      }

      // Handle a change in schedule endTime.
      if (ssExistsDuring.getEnd() == null) {
        // Two cases to check for.
        if (reservation.getEndTime() == Long.MAX_VALUE) {
          log.debug("[processModifiedSwitchingSubnet] endTime remains the same.");
        } else {
          // Change in endTime to an infinite value.
          JAXBElement<XMLGregorianCalendar> scheduleTypeEndTime = CS_FACTORY.createScheduleTypeEndTime(null);
          scheduleTypeEndTime.setNil(true);
          sch.setEndTime(scheduleTypeEndTime);
        }
      } else {
        if (reservation.getEndTime() == ssExistsDuring.getEnd().toGregorianCalendar().getTimeInMillis()) {
          log.debug("[processModifiedSwitchingSubnet] endTime remains the same.");
        } else {
          log.debug("[processModifiedSwitchingSubnet] change in endTime to {}.",
              ssExistsDuring.getEnd().toXMLFormat());
          sch.setEndTime(CS_FACTORY.createScheduleTypeEndTime(ssExistsDuring.getEnd()));
        }
      }

      rrc.setServiceType(serviceType);
      rrc.setSchedule(sch);

      // Populate the reserve structure with the mrs:switchingSubnet ID as the global reservation ID,
      // and use the associated optional mrs:tag as the reservation description.
      String uniqueId = "deltaId+" + deltaId + ":uuid+" + UUID.randomUUID();
      String tag = ModelUtil.getMrsTag(switchingSubnet);
      if (Strings.isNullOrEmpty(tag)) {
        // No mrs:tag provided so compose our own.
        log.debug("[processModifiedSwitchingSubnet] mrs:tag not provided.");
        tag = uniqueId;
      }

      if (tag.equalsIgnoreCase(reservation.getDescription())) {
        log.debug("[processModifiedSwitchingSubnet] description remains the same.");
      }

      ReserveType r = CS_FACTORY.createReserveType();
      r.setCriteria(rrc);
      r.setConnectionId(reservation.getConnectionId());
      r.setDescription(uniqueId); // OSCARS supports modification of description.

      // Now store the mapping for this SwitchingSubnet in our local database.  We will
      // need it later to correlate between NSI-CS messages and SENSE MRML.  We ignore
      // the existing connection map for this reservation just in case the MRML
      // identifiers have changed.
      ConnectionMap cm = new ConnectionMap();
      cm.setUniqueId(uniqueId);
      cm.setVersion(rrc.getVersion());
      cm.setDeltaId(deltaId);
      cm.setSwitchingSubnetId(switchingSubnet.getURI());
      cm.setExistsDuringId(ssExistsDuring.getId());
      cm.setServiceType(serviceType);
      cm.setTag(tag);

      // Add the MRML port to STP mapping information to the connection map.
      StpMapping smSrc = new StpMapping(src.getStp().getStpId(), src.getTag(), src.getMrsPortId(),
          src.getMrsLabelId(), src.getBw().getId(), src.getNmlExistsDuringId());
      cm.getMap().add(smSrc);

      StpMapping smDst = new StpMapping(dst.getStp().getStpId(), dst.getTag(), dst.getMrsPortId(),
          dst.getMrsLabelId(), dst.getBw().getId(), dst.getNmlExistsDuringId());
      cm.getMap().add(smDst);

      // Store the connection map to the database.
      ConnectionMap stored = connectionMapService.store(cm);

      log.debug("[processModifiedSwitchingSubnet] stored connectionMap = {}", stored);

      // Now complete the remaining NSI-CS fields and send the message to the uPA.
      Holder<CommonHeaderType> header = getNsiCsHeader();
      String correlationId = header.value.getCorrelationId();

      // Add this to the operation map to track progress asynchronously.  All
      // asynchronous messaging uses the operation map to record progress.
      Operation op = new Operation();
      op.setOperation(OperationType.reserve);
      op.setState(StateType.reserving);
      op.setUniqueId(uniqueId);
      op.setCorrelationId(correlationId);
      operationMap.store(op);

      // TODO: Create and store a reservation object in database.  Note: the
      //  variable reservation contains the original reservation db entry.
      Reservation newres = createModifiedReservation(reservation, r, originalP2P);
      newres.setUniqueId(uniqueId);

      // Store this reservation for later reference.
      reservationService.store(newres);

      // Add to the list of correlationIds we will wait to receive a reserveConfirmed.
      correlationIds.add(correlationId);

      // Issue the NSI reservation request.  Asynchronous NSI-CS messaging will be
      // occurring for the reservation.
      try {
        log.debug("[processModifiedSwitchingSubnet] issuing reserve operation correlationId = {}, connectionId = {}",
            correlationId, r.getConnectionId());
        ClientUtil nsiClient = getNsiClient();
        ReserveResponseType response = nsiClient.getProxy().reserve(r, header);

        // Add connectionId to the list we need to commit.
        commits.add(response.getConnectionId());

        log.debug("[processModifiedSwitchingSubnet] reserve response received correlationId = {}, connectionId = {}",
            correlationId, response.getConnectionId());
      } catch (SOAPFaultException ex) {
        //TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        correlationIds.stream().map(operationMap::get).forEach(o -> {
          reservationService.deleteByUniqueId(o.getUniqueId());
          connectionMapService.deleteByUniqueId(o.getUniqueId());
          operationMap.delete(o.getCorrelationId());
        });

        log.error("[processModifiedSwitchingSubnet] Failed to send NSI CS reserve message, correlationId = {}, SOAP Fault = {}",
            correlationId, ex.getFault().toString());
        throw ex;
      } catch (ServiceException ex) {
        //TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        correlationIds.stream().map(operationMap::get).forEach(o -> {
          reservationService.deleteByUniqueId(o.getUniqueId());
          connectionMapService.deleteByUniqueId(o.getUniqueId());
          operationMap.delete(o.getCorrelationId());
        });

        log.error("[processModifiedSwitchingSubnet] Failed to send NSI CS reserve message, correlationId = {}, errorId = {}, text = {}",
            correlationId, ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
        throw ex;
      }
    }

    log.info("[processModifiedSwitchingSubnet] processing total of {} modifications for delta {}",
        correlationIds.size(), deltaId);

    return correlationIds;
  }

  /**
   * This method populates a new reservation object with the new service information
   * resulting from the modification.
   *
   * @param old The old reservation.
   * @param r The new reservation criteria.
   * @param p2ps The NSI-CS p2ps structure containing the new version of the service.
   * @return The nre reservation object.
   */
  private Reservation createModifiedReservation(Reservation old, ReserveType r, P2PServiceBaseType p2ps) {
    // Build a reservation database object for this reservation.
    Reservation reservation = new Reservation();

    // These values will not change between reservation versions.
    reservation.setProviderNsa(old.getProviderNsa());
    reservation.setGlobalReservationId(old.getGlobalReservationId());
    reservation.setTopologyId(old.getTopologyId());
    reservation.setConnectionId(old.getConnectionId());
    reservation.setParentConnectionId(old.getParentConnectionId());
    reservation.setServiceType(old.getServiceType());

    // These are possibly new/changed attributes.
    reservation.setVersion(r.getCriteria().getVersion());
    reservation.setDescription(r.getDescription());
    reservation.setStartTime(CsUtils.getStartTime(r.getCriteria().getSchedule().getStartTime()));
    reservation.setEndTime(CsUtils.getEndTime(r.getCriteria().getSchedule().getEndTime()));

    // The state machine states are transitioned.
    reservation.setReservationState(ReservationStateEnumType.RESERVE_CHECKING);
    reservation.setProvisionState(old.getProvisionState());
    reservation.setLifecycleState(old.getLifecycleState());
    reservation.setDataPlaneActive(false);

    // We discovered this new reservation now.
    reservation.setDiscovered(System.currentTimeMillis());

    // Now we encode the modified P2P service element.
    try {
      reservation.setService(CsParser.getInstance().p2ps2xml(p2ps));
    } catch (JAXBException ex) {
      log.error("[CsProvider] createModifiedReservation failed for connectionId = {}",
          reservation.getConnectionId(), ex);
    }

    return reservation;
  }

  /**
   * Get the existDuring lifetime object if it exists, so we can model a schedule.
   *
   * @param switchingSubnet
   * @return
   * @throws DatatypeConfigurationException
   */
  private static NmlExistsDuring getNmlExistsDuring(Resource switchingSubnet) throws DatatypeConfigurationException {
    Optional<Statement> existsDuring = Optional.ofNullable(switchingSubnet.getProperty(Nml.existsDuring));
    NmlExistsDuring ssExistsDuring;
    if (existsDuring.isPresent()) {
      // We have an existsDuring resource specifying the schedule time.
      Resource existsDuringRef = existsDuring.get().getResource();
      log.debug("[getNmlExistsDuring] existsDuringRef: " + existsDuringRef.getURI());

      ssExistsDuring = new NmlExistsDuring(existsDuringRef);
    } else {
      // We need to create our own schedule using the defaults.
      ssExistsDuring = new NmlExistsDuring(switchingSubnet.getURI() + ":existsDuring");
    }

    log.debug("[getNmlExistsDuring] NmlExistsDuring: " + ssExistsDuring.getId());
    return ssExistsDuring;
  }

  /**
   * commitDelta
   *
   * @param deltaId
   * @throws ServiceException
   * @throws IllegalArgumentException
   * @throws TimeoutException
   */
  public void commitDelta(String deltaId)
          throws ServiceException, IllegalArgumentException, TimeoutException {

    // Look up the connection identifiers associated with this deltaId.
    DeltaConnection connectionIds = deltaMap.delete(deltaId);
    if (connectionIds == null) {
      log.debug("[commitDelta] commitDelta could not find connectionIds associated with deltaId = {}", deltaId);
      throw new IllegalArgumentException("Could not find connections for deltaId = " + deltaId);
    }

    // First we process the reduction associated connection ids.
    commitDeltaReduction(connectionIds.getTerminates());

    // Lastly we process the addition associated connection ids.
    commitDeltaAddition(connectionIds.getCommits());
  }

  /**
   * Create and send NSI Terminate operations for each connectionId provided,
   * blocking until all response have been received.
   *
   * @return
   */
  private void commitDeltaReduction(Set<String> connectionIds)
          throws ServiceException, IllegalArgumentException, TimeoutException {

    // CorrelationId from NSI termination request go in here.
    List<String> correlationIds = new ArrayList<>();

    // For each cid we must create and send an NSI terminate request.
    for (String cid : connectionIds) {
      // Build the NSI-CS header.
      Holder<CommonHeaderType> header = getNsiCsHeader();
      String correlationId = header.value.getCorrelationId();

      // Build an NSI-CS terminate request.
      GenericRequestType terminate = CS_FACTORY.createGenericRequestType();
      terminate.setConnectionId(cid);

      // Look up the reservation corresponding to the connectionId.
      // TODO: Consider returning only one reservation or null.
      Reservation reservation = reservationService.getByAnyConnectionId(header.value.getProviderNSA(), cid)
          .stream().max(comparing(Reservation::getVersion)).orElse(null);
      if (reservation == null) {
        log.error("[commitDeltaReduction] could not find reservation for connectionId = {}", cid);
        continue;
      }

      log.error("[commitDeltaReduction] terminating connectionId = {}, version = {}, uniqueId = {}",
          cid, reservation.getVersion(), reservation.getUniqueId());

      // Add this to the operation map to track progress.
      Operation op = new Operation();
      op.setUniqueId(reservation.getUniqueId());
      op.setOperation(OperationType.terminate);
      op.setState(StateType.terminating);
      op.setCorrelationId(correlationId);
      operationMap.store(op);

      // Save the correlationId for later semaphore blocking.
      correlationIds.add(correlationId);

      // Issue the NSI terminate request for the specified connection.
      boolean error = true;
      String errorMessage = null;
      try {
        ClientUtil nsiClient = getNsiClient();
        nsiClient.getProxy().terminate(terminate, header);
        log.debug("[commitDeltaReduction] issued terminate operation correlationId = {}, connectionId = {}",
                correlationId, terminate.getConnectionId());
        error = false;
      } catch (SOAPFaultException ex) {
        // Continue on this error but clean up this correlationId.
        operationMap.delete(correlationId);
        correlationIds.remove(correlationId);

        log.error("[commitDeltaReduction] Failed to send NSI CS terminate message, correlationId = {}, SOAP Fault = {}",
                correlationId, ex.getFault().toString());

        errorMessage = String.format("Terminate failed, cid = %s, SOAP Fault = %s",
                cid, ex.getFault().toString());
      } catch (ServiceException ex) {
        // Continue on this error but clean up this correlationId.
        operationMap.delete(correlationId);
        correlationIds.remove(correlationId);

        log.error("[commitDeltaReduction] Failed to send NSI CS terminate message, correlationId = {}, errorId = {}, text = {}",
                correlationId, ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());

        errorMessage = String.format("Terminate failed, cid = %s, errorId = %s, text = %s",
                cid, ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      } catch (Exception ex) {
        operationMap.delete(correlationId);
        correlationIds.remove(correlationId);

        log.error("[commitDeltaReduction] Failed to send NSI CS terminate message, correlationId = {}",
                correlationId, ex);

        errorMessage = String.format("Terminate failed, cid = %s, ex = %s",
                cid, ex.getLocalizedMessage());
        throw ex;
      } finally {
        // Handle an error in reduction
        if (error) {
          Collection<Reservation> reservations =
                  reservationService.getByAnyConnectionId(nsiProperties.getProviderNsaId(), cid);
          if (reservations == null || reservations.isEmpty()) {
            // We have not seen this reservation before so ignore it.
            log.info("[commitDeltaReduction] commitDeltaReduction: no reference to reservation, cid = {}", cid);
          } else {
            for (Reservation r : reservations) {
              // We tried to terminate this reservation and it failed.  Should
              // we leave it in the current state or manually set it to terminated?
              log.error("[commitDeltaReduction] commitDeltaReduction: current errored reservation state:\n{}", r.toString());
              r.setErrorState(Reservation.ErrorState.NSITERMINATE);
              r.setErrorMessage(errorMessage);
              r.setDiscovered(System.currentTimeMillis());
              reservationService.store(r);
            }
          }
        }
      }
    }

    // Now we need to wait for the asynchronous NSI response.
    waitForOperations(correlationIds);
  }

  /**
   * commitDeltaAddition
   *
   * @param connectionIds
   * @throws ServiceException
   * @throws IllegalArgumentException
   * @throws TimeoutException
   */
  private void commitDeltaAddition(Set<String> connectionIds)
          throws ServiceException, IllegalArgumentException, TimeoutException {
    // We store our outstanding operations here.
    List<String> correlationIds = new ArrayList<>();

    // Commit the NSI reservation first.
    for (String cid : connectionIds) {
      Holder<CommonHeaderType> header = getNsiCsHeader();
      String correlationId = header.value.getCorrelationId();
      GenericRequestType commitBody = CS_FACTORY.createGenericRequestType();
      commitBody.setConnectionId(cid);

      // Look up the reservation corresponding to the connectionId.
      // TODO: Consider returning only one reservation or null.
      Reservation reservation = reservationService.getByAnyConnectionId(header.value.getProviderNSA(), cid)
          .stream().max(comparing(Reservation::getVersion)).orElse(null);
      if (reservation == null) {
        log.error("[commitDeltaAddition] could not find reservation for connectionId = {}", cid);
        continue;
      }

      log.error("[commitDeltaAddition] committing connectionId = {}, version = {}, uniqueId = {}",
          cid, reservation.getVersion(), reservation.getUniqueId());

      // Add this to the operation map to track progress.
      Operation op = new Operation();
      op.setUniqueId(reservation.getUniqueId());
      op.setOperation(OperationType.reserveCommit);
      op.setState(StateType.committing);
      op.setCorrelationId(correlationId);
      operationMap.store(op);

      // Track the correlationId for this operation.
      correlationIds.add(correlationId);

      boolean error = true;
      String errorMessage = null;
      try {
        ClientUtil nsiClient = getNsiClient();
        nsiClient.getProxy().reserveCommit(commitBody, header);

        log.debug("[commitDeltaAddition] issued commitDeltaAddition operation correlationId = {}, connectionId = {}",
                op.getCorrelationId(), cid);
        error = false;
      } catch (SOAPFaultException soap) {
        operationMap.delete(correlationIds);

        log.error("[commitDeltaAddition] commitDeltaAddition failed to send NSI CS reserveCommit message, correlationId = {}, SOAP Fault {}",
            correlationId, soap.getFault().toString());

        errorMessage = String.format("reserveCommit failed, cid = %s, SOAP Fault = %s",
                cid, soap.getFault().toString());
        throw soap;
      } catch (ServiceException ex) {
        // TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        operationMap.delete(correlationIds);

        log.error("[csProvider] commitDeltaAddition failed to send NSI CS reserveCommit message, correlationId = {}, errorId = {}, text = {}",
            correlationId, ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());

        errorMessage = String.format("reserveCommit failed, cid = %s, errorId = %s, text = %s",
                cid, ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
        throw ex;
      } catch (Exception ex) {
        operationMap.delete(correlationIds);

        log.error("[commitDeltaAddition] commitDeltaAddition failed to send NSI CS reserveCommit message, correlationId = {}",
            correlationId, ex);

        errorMessage = String.format("reserveCommit failed, cid = %s, ex = %s",
                cid, ex.getLocalizedMessage());
        throw ex;
      } finally {
        if (error) {
          // Reload the reservation in case it has changed asynchronously.
          reservation = reservationService.getByAnyConnectionId(header.value.getProviderNSA(), cid)
              .stream().max(comparing(Reservation::getVersion)).orElse(null);
          if (reservation == null) {
            // We have not seen this reservation before so ignore it.
            log.info("[commitDeltaAddition] commitDeltaAddition: no reference to reservation, cid = {}", cid);
          } else {
            // The reservation commit failed so put it in the RESERVE_HELD.
            // Maybe RESERVE_FAILED would have been better? At least they can
            // terminate it this way.
            log.error("[commitDeltaAddition] commitDeltaAddition: current errored during reserveCommit reservation state:\n{}",
                  reservation);
            reservation.setReservationState(ReservationStateEnumType.RESERVE_HELD);
            reservation.setErrorState(Reservation.ErrorState.NSIRESERVECOMMIT);
            reservation.setErrorMessage(errorMessage);
            reservation.setDiscovered(System.currentTimeMillis());
            reservationService.store(reservation);
          }
        }
      }
    }

    // Now we need to wait for the asynchronous NSI commit response.
    waitForOperations(correlationIds);

    // Now we go through and provision each of these connectionIds.
    correlationIds.clear();
    for (String cid : connectionIds) {
      Holder<CommonHeaderType> header = getNsiCsHeader();
      String correlationId = header.value.getCorrelationId();
      GenericRequestType commitBody = CS_FACTORY.createGenericRequestType();
      commitBody.setConnectionId(cid);

      // Look up the reservation corresponding to the connectionId.
      // TODO: Consider returning only one reservation or null.
      Reservation reservation = reservationService.getByAnyConnectionId(header.value.getProviderNSA(), cid)
          .stream().max(comparing(Reservation::getVersion)).orElse(null);
      if (reservation == null) {
        log.error("[commitDeltaAddition] could not find reservation for connectionId = {}", cid);
        continue;
      }

      // Add this to the operation map to track progress.
      Operation op = new Operation();
      op.setUniqueId(reservation.getUniqueId());
      op.setOperation(OperationType.provision);
      op.setState(StateType.provisioning);
      op.setCorrelationId(correlationId);
      operationMap.store(op);
      correlationIds.add(correlationId);

      boolean error = true;
      String errorMessage = null;
      try {
        ClientUtil nsiClient = getNsiClient();
        nsiClient.getProxy().provision(commitBody, header);

        log.debug("[commitDeltaAddition] issued provision operation correlationId = {}, connectionId = {}",
                correlationId, cid);

        error = false;
      } catch (SOAPFaultException soap) {
        operationMap.delete(correlationIds);

        log.error("[commitDeltaAddition] Failed to send NSI CS provision message, correlationId = {}, SOAP Fault {}",
                correlationId, soap.getFault().toString());

        errorMessage = String.format("Provision failed, cid = %s, SOAP Fault = %s",
                cid, soap.getFault().toString());
        throw soap;
      } catch (ServiceException ex) {
        //TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        operationMap.delete(correlationIds);

        log.error("[commitDeltaAddition] Failed to send NSI CS provision message, correlationId = {}, errorId = {}, text = {}",
                correlationId, ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());

        errorMessage = String.format("Provision failed, cid = %s, errorId = %s, text = %s",
                cid, ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
        throw ex;
      } catch (Exception ex) {
        operationMap.delete(correlationIds);

        log.error("[commitDeltaAddition] Failed to send NSI CS provision message, correlationId = {}", correlationId, ex);

        errorMessage = String.format("Provision failed, cid = %s, ex = %s",
                cid, ex.getLocalizedMessage());
        throw ex;
      } finally {
        if (error) {
          // Reload the reservation in case it has changed asynchronously.
          reservation = reservationService.getByAnyConnectionId(header.value.getProviderNSA(), cid)
              .stream().max(comparing(Reservation::getVersion)).orElse(null);
          if (reservation == null) {
            // We have not seen this reservation before so ignore it.
            log.info("[commitDeltaAddition] commitDeltaAddition: no reference to reservation, cid = {}", cid);
          } else {
            // The reservation commit failed so put it in the RESERVE_HELD.
            // Maybe RESERVE_FAILED would have been better? At least they can
            // terminate it this way.
            log.error("[commitDeltaAddition] commitDeltaAddition: current errored during provision reservation state:\n{}",
                reservation);
            reservation.setReservationState(ReservationStateEnumType.RESERVE_START);
            reservation.setErrorState(Reservation.ErrorState.NSIPROVISION);
            reservation.setErrorMessage(errorMessage);
            reservation.setDiscovered(System.currentTimeMillis());
            reservationService.store(reservation);
          }
        }
      }
    }

    // Now we need to wait for the asynchronous NSI provision response.
    waitForOperations(correlationIds);
  }

  /**
   * Wait for queued NSI operations to complete by waiting on a shared semaphore
   * between this thread and the NSI CS callback thread.
   *
   * @param correlationIds List of the outstanding correlationIds the will need to complete.
   *
   * @return Will return an exception (the last encountered) if one has occurred.
   */
  private void waitForOperations(List<String> correlationIds)
          throws ServiceException, IllegalArgumentException, TimeoutException {
    Optional<Exception> exception = Optional.empty();
    for (String id : correlationIds) {
      log.info("[waitForOperations] waiting {} seconds for completion of correlationId = {}",
          nsiProperties.getOperationWaitTimer(), id);

      if (operationMap.wait(id, nsiProperties.getOperationWaitTimer())) {
        Operation op = operationMap.delete(id);

        log.info("[waitForOperations] operation {} completed, correlationId = {}", op.getOperation(), id);

        if (op.getOperation() == OperationType.reserve && op.getState() != StateType.reserved) {
          log.error("[waitForOperations] operation failed to reserve, correlationId = {}, state = {}, exception = {}",
                  id, op.getState(), op.getException());

          if (op.getException() != null) {
            exception = Optional.of(new ServiceException("Operation failed to reserve", op.getException()));
          }
          else {
            exception = Optional.of(new IllegalArgumentException("Operation failed to reserve, correlationId = "
                  + id + ", state = " + op.getState()));
          }
        }
      } else {
        log.error("[waitForOperations] timeout, failed to get response for correlationId = {}", id);
        Operation op = operationMap.delete(id);
        StateType state = StateType.unknown;
        if (op != null) {
          state = op.getState();
        }
        exception = Optional.of(new TimeoutException("Operation failed to reserve, correlationId = "
                  + id + ", state = " + state));
      }
    }

    // Do I really care about the specific exception?
    if (exception.isPresent()) {
      Exception ex = exception.get();
      if (ex instanceof ServiceException) {
        throw (ServiceException) ex;
      } else if (ex instanceof IllegalArgumentException) {
        throw (IllegalArgumentException) ex;
      } else {
        throw (TimeoutException) ex;
      }
    }
  }
}
