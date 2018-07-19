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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.cs.lib.Client;
import net.es.nsi.cs.lib.ClientUtil;
import net.es.nsi.cs.lib.Helper;
import net.es.nsi.cs.lib.NsiHeader;
import net.es.nsi.cs.lib.SimpleLabel;
import net.es.nsi.cs.lib.SimpleStp;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
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
import net.es.sense.rm.driver.nsi.mrml.MrsBandwidthService;
import net.es.sense.rm.driver.nsi.mrml.MrsBandwidthType;
import net.es.sense.rm.driver.nsi.mrml.MrsUnits;
import net.es.sense.rm.driver.nsi.mrml.NmlExistsDuring;
import net.es.sense.rm.driver.nsi.mrml.NmlLabel;
import net.es.sense.rm.driver.nsi.mrml.StpHolder;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import net.es.sense.rm.driver.nsi.spring.SpringExtension;
import net.es.sense.rm.driver.schema.Mrs;
import net.es.sense.rm.driver.schema.Nml;
import net.es.sense.rm.driver.schema.Sd;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.ogf.schemas.nsi._2013._12.connection.provider.Error;
import org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.GenericRequestType;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A provider implementing MRML delta operations using the NSI Connection Service.
 *
 * @author hacksaw
 */
@Slf4j
@Component
public class CsProvider {

  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private ConnectionMapService connectionMapService;

  @Autowired
  private OperationMapRepository operationMap;

  @Autowired
  private DeltaMapRepository deltaMap;

  @Autowired
  private ReservationService reservationService;

  @Autowired
  private SpringExtension springExtension;

  @Autowired
  private NsiActorSystem nsiActorSystem;

  private ActorRef connectionActor;

  private static final org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory CS_FACTORY
          = new org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory();
  private static final org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory P2PS_FACTORY
          = new org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory();

  public void start() {
    // Initialize the actors.
    log.info("[CsProvider] Starting NSI CS system initialization...");
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

  public ActorRef GetConnectionActor() {
    return connectionActor;
  }

  public void terminate() {
    nsiActorSystem.shutdown(connectionActor);
  }

  public void load() {
    Client nsiClient = new Client(nsiProperties.getProviderConnectionURL());

    CommonHeaderType requestHeader = NsiHeader.builder()
            .correlationId(Helper.getUUID())
            .providerNSA(nsiProperties.getProviderNsaId())
            .requesterNSA(nsiProperties.getNsaId())
            .replyTo(nsiProperties.getRequesterConnectionURL())
            .build()
            .getRequestHeaderType();

    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;

    QueryType query = CS_FACTORY.createQueryType();
    try {
      log.info("[CsProvider] Sending querySummarySync: providerNSA = {}, correlationId = {}",
              header.value.getProviderNSA(), header.value.getCorrelationId());
      QuerySummaryConfirmedType querySummarySync = nsiClient.getProxy().querySummarySync(query, header);
      log.info("[CsProvider] QuerySummaryConfirmed recieved, providerNSA = {}, correlationId = {}",
              header.value.getProviderNSA(), header.value.getCorrelationId());

      QuerySummary q = new QuerySummary(reservationService);
      q.process(querySummarySync, header);

    } catch (Error ex) {
      log.error("[CsProvider] querySummarySync exception on operation - {} {}",
              ex.getFaultInfo().getServiceException().getErrorId(),
              ex.getFaultInfo().getServiceException().getText());
    } catch (org.ogf.schemas.nsi._2013._12.connection.requester.ServiceException ex) {
      log.error("[CsProvider] querySummarySync exception processing results - {} {}",
              ex.getFaultInfo().getErrorId(),
              ex.getFaultInfo().getText());
    }
  }

  /**
   * Process delta reduction and addition requests up to the pre-commit state,
   * storing connection information for commit processing.  We are forgiving
   * of reduction elements not being present, but strict about the creation
   * for addition elements.
   *
   * @param model
   * @param deltaId
   * @param reduction
   * @param addition
   * @throws Exception A
   */
  public void processDelta(
          net.es.sense.rm.driver.nsi.db.Model model,
          String deltaId,
          Optional<Model> reduction,
          Optional<Model> addition) throws Exception {

    DeltaConnection connectionIds = new DeltaConnection();
    connectionIds.setDeltaId(deltaId);

    // We process the reduction first.
    if (reduction.isPresent()) {
      connectionIds.getTerminates().addAll(processDeltaReduction(reduction.get()));
    }

    // Now the addition.
    if (addition.isPresent()) {
      connectionIds.getCommits().addAll(processDeltaAddition(model, deltaId, addition.get()));
    }

    // Store the list of connection ids we will need to handle during commit phase.
    deltaMap.store(connectionIds);
  }

  /**
   * Determine the set of NSI connections that must be removed as part of this
   * delta, leaving the termination operation for the commit.
   *
   * @param m
   * @param deltaId
   * @param reduction
   * @return
   */
  private List<String> processDeltaReduction(Model reduction) {

    // The list of connection ids we will need terminate in the delta commit.
    List<String> terminates = new ArrayList<>();

    // We model connections as mrs:SwitchingSubnet objects so query the
    // reduction model for all those provided.  We will just delete these.
    ResultSet ssSet = ModelUtil.getResourcesOfType(reduction, Mrs.SwitchingSubnet);
    while (ssSet.hasNext()) {
      QuerySolution querySolution = ssSet.next();

      // Get the SwitchingSubnet resource.
      Resource switchingSubnet = querySolution.get("resource").asResource();

      // The SwitchingSubnet identifier is the global reservation identifier in
      // associated NSI connections.  For now we only support the removal of
      // a complete SwitchingSubnet, and not individual ports/vlans.
      String ssid = switchingSubnet.getURI();

      log.debug("SwitchingSubnet: " + ssid);

      // Look up all the reservation segments associated with this SwitchingSubnet.
      terminates.addAll(reservationService.getByGlobalReservationId(ssid).stream()
              .filter(r -> (LifecycleStateEnumType.TERMINATED != r.getLifecycleState()
                      && LifecycleStateEnumType.TERMINATING != r.getLifecycleState()))
              .map(Reservation::getConnectionId).collect(Collectors.toList()));
    }

    return terminates;
  }

  private List<String> processDeltaAddition(net.es.sense.rm.driver.nsi.db.Model m, String deltaId, Model addition)
          throws DatatypeConfigurationException, ServiceException, IllegalArgumentException, TimeoutException {
    // This is a list of cid associated with reservations created
    // as part of the delta addition.
    List<String> commits = new ArrayList<>();

    // CorrelationId from NSI these reservation requests go in here.
    List<String> correlationIds = new ArrayList<>();

    // Get the associated model.
    Model model = ModelUtil.unmarshalModel(m.getBase());

    // Apply the delta to our reference model so we can search with proposed changes.
    ModelUtil.applyDeltaAddition(model, addition);

    // We model connections as mrs:SwitchingSubnet objects so query the
    // addition model for all those provided.
    ResultSet ssSet = ModelUtil.getResourcesOfType(addition, Mrs.SwitchingSubnet);

    // We will treat each mrs:SwitchingSubnet as an independent reservation in NSI.
    while (ssSet.hasNext()) {
      QuerySolution querySolution = ssSet.next();

      // Get the SwitchingSubnet resource.
      Resource switchingSubnet = querySolution.get("resource").asResource();
      log.debug("SwitchingSubnet: " + switchingSubnet.getURI());

      // Get the existDruing lifetime object if it exists so we can model a schedule.
      Optional<Statement> existsDuring = Optional.ofNullable(switchingSubnet.getProperty(Nml.existsDuring));
      NmlExistsDuring ssExistsDuring;
      if (existsDuring.isPresent()) {
        // We have an existsDuring resource specifying the schedule time.
        Resource existsDuringRef = existsDuring.get().getResource();
        log.debug("existsDuringRef: " + existsDuringRef.getURI());

        ssExistsDuring = new NmlExistsDuring(existsDuringRef);
      } else {
        // We need to create our own schedule using the defaults.
        ssExistsDuring = new NmlExistsDuring(switchingSubnet.getURI() + ":existsDuring");
      }

      // We need the associated parent SwitchingService resource to determine
      // the ServiceDefinition that holds the serviceType.
      Statement belongsTo = switchingSubnet.getProperty(Nml.belongsTo);
      Resource switchingServiceRef = belongsTo.getResource();
      log.debug("SwitchingServiceRef: " + switchingServiceRef.getURI());

      // Get the full SwitchingService definition from the merged model.
      Resource switchingService = ModelUtil.getResourceOfType(model, switchingServiceRef, Nml.SwitchingService);
      log.debug("SwitchingService: " + switchingService.getURI());

      // Now we need the ServiceDefinition associated with this SwitchingService.
      Statement hasServiceDefinition = switchingService.getProperty(Sd.hasServiceDefinition);
      Resource serviceDefinitionRef = hasServiceDefinition.getResource();
      log.debug("serviceDefinitionRef: " + serviceDefinitionRef.getURI());

      // Get the full ServiceDefinition definition from the merged model.
      Resource serviceDefinition = ModelUtil.getResourceOfType(model, serviceDefinitionRef, Sd.ServiceDefinition);
      log.debug("ServiceDefinition: " + serviceDefinition.getURI());

      Statement serviceTypeRef = serviceDefinition.getProperty(Sd.serviceType);
      log.debug("serviceType: " + serviceTypeRef.getString());

      // We currently know about the EVTS p2p service.
      List<StpHolder> stps = new ArrayList<>();

      String serviceType = serviceTypeRef.getString().trim();
      if (Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(serviceType) ||
              Nsi.NSI_SERVICETYPE_L2_LB_ES.equalsIgnoreCase(serviceType)) {
        // Find the ports that ate part of this SwitchSubnet and build NSI STP
        // identifiers for the service.
        StmtIterator listProperties = switchingSubnet.listProperties(Nml.hasBidirectionalPort);
        while (listProperties.hasNext()) {
          Statement hasBidirectionalPort = listProperties.next();
          Resource biRef = hasBidirectionalPort.getResource();
          log.debug("bi member: " + biRef.getURI());

          Resource biChild = ModelUtil.getResourceOfType(addition, biRef, Nml.BidirectionalPort);
          if (biChild == null) {
            log.error("Requested BidirectionalPort does not exist {}", biRef.getURI());
            throw new IllegalArgumentException("Requested BidirectionalPort does not exist " + biRef.getURI());
          }

          log.debug("biChild: " + biChild.getURI());

          MrsBandwidthService bws = new MrsBandwidthService(biChild, addition);

          log.debug("BandwidthService: {}", bws.getId());
          log.debug("type: {}", bws.getBandwidthType());
          log.debug("maximumCapacity: {} {}", bws.getMaximumCapacity(), bws.getUnit());
          log.debug("maximumCapacity: {} mbps", MrsUnits.normalize(bws.getMaximumCapacity(),
                  bws.getUnit(), MrsUnits.mbps));

          // The "guaranteedCapped" BandwidthService maps to the NSI_SERVICETYPE_EVTS service so
          // we need to verify this is a valid request.
          if (MrsBandwidthType.guaranteedCapped != bws.getBandwidthType()) {
            String error = "Requested BandwidthService type = " + bws.getBandwidthType() +
                    " not supported by SwitchingService = " + switchingService.getURI() +
                    " on portId = " + biRef.getURI();
            log.error("[CsProvider] {}.", error);
            throw new IllegalArgumentException(error);
          }

          // Now determine if there is an independent existsDuring object.
          String childExistsDuringId = ssExistsDuring.getId();
          existsDuring = Optional.ofNullable(biChild.getProperty(Nml.existsDuring));
          if (existsDuring.isPresent()) {
            Resource childExistsDuringRef = existsDuring.get().getResource();
            log.debug("childExistsDuringRef: " + childExistsDuringRef.getURI());
            if (!ssExistsDuring.getId().contentEquals(childExistsDuringRef.getURI())) {
              // We have a different existsDuring reference than our SwitchingSubnet.
              childExistsDuringId = childExistsDuringRef.getURI();
            }
          }

          // Now get the label for this port.
          Statement labelRef = biChild.getProperty(Nml.hasLabel);
          Resource label = ModelUtil.getResourceOfType(addition, labelRef.getResource(), Nml.Label);

          NmlLabel nmlLabel = new NmlLabel(label);
          SimpleLabel simpleLabel = nmlLabel.getSimpleLabel();

          Resource parentBi = ModelUtil.getParentBidirectionalPort(model, biChild);
          log.debug("parentBi: " + parentBi.getURI());

          SimpleStp stp = new SimpleStp(parentBi.getURI(), simpleLabel);
          log.debug("stpId: {}", stp.getStpId());

          stps.add(new StpHolder(biChild.getURI(), stp, bws, childExistsDuringId, label.getURI()));
        }

        // We need exactly two ports for our point-to-point connection.
        if (stps.size() != 2) {
          log.error("SwitchingSubnet contained {} ports.", stps.size());
          throw new IllegalArgumentException("SwitchingSubnet contained incorrect number of ports (" + stps.size() + ").");
        }

        // Populate the NSI CS message with p2ps service.
        StpHolder src = stps.get(0);
        StpHolder dst = stps.get(1);

        // Normalize bandidth to mbps for the P2PS request.
        long srcBw = MrsUnits.normalize(src.getBw().getMaximumCapacity(), src.getBw().getUnit(), MrsUnits.mbps);
        long dstBw = MrsUnits.normalize(dst.getBw().getMaximumCapacity(), dst.getBw().getUnit(), MrsUnits.mbps);

        P2PServiceBaseType p2ps = P2PS_FACTORY.createP2PServiceBaseType();
        p2ps.setCapacity((srcBw > dstBw ? dstBw : srcBw));
        p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        p2ps.setSymmetricPath(Boolean.TRUE);
        p2ps.setSourceSTP(src.getStp().getStpId());
        p2ps.setDestSTP(dst.getStp().getStpId());

        // Base the reservation off of the specified existsDuring criteria.
        ScheduleType sch = CS_FACTORY.createScheduleType();
        if (ssExistsDuring.getStart() != null) {
          sch.setStartTime(CS_FACTORY.createScheduleTypeStartTime(ssExistsDuring.getStart()));
        }

        if (ssExistsDuring.getEnd() != null) {
          sch.setEndTime(CS_FACTORY.createScheduleTypeEndTime(ssExistsDuring.getEnd()));
        }

        ReservationRequestCriteriaType rrc = CS_FACTORY.createReservationRequestCriteriaType();
        rrc.setVersion(0);
        rrc.setSchedule(sch);
        rrc.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
        rrc.getAny().add(P2PS_FACTORY.createP2Ps(p2ps));

        ReserveType r = CS_FACTORY.createReserveType();
        r.setGlobalReservationId(switchingSubnet.getURI());
        r.setDescription("deltaId+" + deltaId + ":uuid+" + UUID.randomUUID().toString());
        r.setCriteria(rrc);

        // Now store the mapping for this SwitchingSubnet.
        ConnectionMap cm = new ConnectionMap();
        cm.setDescription(r.getDescription());
        cm.setDeltaId(deltaId);
        cm.setSwitchingSubnetId(switchingSubnet.getURI());
        cm.setExistsDuringId(ssExistsDuring.getId());
        cm.setServiceType(serviceType);

        StpMapping smSrc = new StpMapping(src.getStp().getStpId(), src.getMrsPortId(),
                src.getMrsLabelId(), src.getBw().getId(), src.getNmlExistsDuringId());
        cm.getMap().add(smSrc);

        StpMapping smDst = new StpMapping(dst.getStp().getStpId(), dst.getMrsPortId(),
                dst.getMrsLabelId(), dst.getBw().getId(), dst.getNmlExistsDuringId());
        cm.getMap().add(smDst);

        ConnectionMap stored = connectionMapService.store(cm);

        log.debug("[SwitchingSubnet] storing connectionMap = {}", stored);

        CommonHeaderType requestHeader = NsiHeader.builder()
                .correlationId(Helper.getUUID())
                .providerNSA(nsiProperties.getProviderNsaId())
                .requesterNSA(nsiProperties.getNsaId())
                .replyTo(nsiProperties.getRequesterConnectionURL())
                .build()
                .getRequestHeaderType();

        Holder<CommonHeaderType> header = new Holder<>();
        header.value = requestHeader;

        // Add this to the operation map to track progress.
        Operation op = new Operation();
        op.setOperation(OperationType.reserve);
        op.setState(StateType.reserving);
        op.setCorrelationId(requestHeader.getCorrelationId());
        operationMap.store(op);

        correlationIds.add(requestHeader.getCorrelationId());

        // Issue the NSI reservation request.
        try {
          ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
          ReserveResponseType response = nsiClient.getProxy().reserve(r, header);

          // Update the operation map with the new cid.
          commits.add(response.getConnectionId());

          log.debug("[csProvider] issued reserve operation correlationId = {}, connectionId = {}",
                  op.getCorrelationId(), response.getConnectionId());
        } catch (ServiceException ex) {
          //TODO: Consider whether we should unwrap any NSI reservations that were successful.
          // For now just delete the correlationId we added.
          operationMap.delete(correlationIds);
          log.error("[csProvider] Failed to send NSI CS reserve message, correlationId = {}, errorId = {}, text = {}",
                  requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
          throw ex;
        } catch (SOAPFaultException ex) {
          //TODO: Consider whether we should unwrap any NSI reservations that were successful.
          // For now just delete the correlationId we added.
          operationMap.delete(correlationIds);
          log.error("[csProvider] Failed to send NSI CS reserve message, correlationId = {}, SOAP Fault = {}",
                  requestHeader.getCorrelationId(), ex.getFault().toString());
          throw ex;
        }
      } else {
        log.error("[csProvider] serviceType not supported {}", serviceTypeRef.getString());
        throw new IllegalArgumentException("serviceType not supported " + serviceTypeRef.getString());
      }
    }

    // Wait for our outstanding reserve operations to complete (or fail).
    waitForOperations(correlationIds);

    // Return the list of connections ids that will need to be commited.
    return commits;
  }

  public void commitDelta(String deltaId)
          throws ServiceException, IllegalArgumentException, TimeoutException {

    // Look up the connection identifiers assoicated with this deltaId.
    DeltaConnection connectionIds = deltaMap.delete(deltaId);
    if (connectionIds == null) {
      log.debug("[csProvider] commitDelta could not find connectionIds associated with deltaId = {}", deltaId);
      throw new IllegalArgumentException("Could not find connections for deltaId = " + deltaId);
    }

    // First we process the reduction associated connection ids.
    commitDeltaReduction(connectionIds.getTerminates());

    // Lastly we process the addition associated connection ids.
    commitDeltaAddition(connectionIds.getCommits());
  }

  /**
   * Determine the set of NSI connections that must be removed as part of this
   * delta, leaving the termination operation for the commit.
   *
   * @param m
   * @param deltaId
   * @param reduction
   * @return
   */
  private void commitDeltaReduction(List<String> connectionIds)
          throws ServiceException, IllegalArgumentException, TimeoutException {

    // CorrelationId from NSI termination request go in here.
    List<String> correlationIds = new ArrayList<>();

    // For each cid we must create and send an NSI terminate request.
    for (String cid : connectionIds) {
      // If we have some changes apply them into the network.
      CommonHeaderType requestHeader = NsiHeader.builder()
              .correlationId(Helper.getUUID())
              .providerNSA(nsiProperties.getProviderNsaId())
              .requesterNSA(nsiProperties.getNsaId())
              .replyTo(nsiProperties.getRequesterConnectionURL())
              .build()
              .getRequestHeaderType();

      Holder<CommonHeaderType> header = new Holder<>();
      header.value = requestHeader;

      GenericRequestType terminate = CS_FACTORY.createGenericRequestType();
      terminate.setConnectionId(cid);

      // Add this to the operation map to track progress.
      Operation op = new Operation();
      op.setOperation(OperationType.terminate);
      op.setState(StateType.terminating);
      op.setCorrelationId(requestHeader.getCorrelationId());
      operationMap.store(op);
      correlationIds.add(requestHeader.getCorrelationId());

      // Issue the NSI reservation request.
      boolean error = true;
      try {
        ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
        nsiClient.getProxy().terminate(terminate, header);
        log.debug("[csProvider] issued terminate operation correlationId = {}, connectionId = {}",
                op.getCorrelationId(), terminate.getConnectionId());
        error = false;
      } catch (ServiceException ex) {
        // Continue on this error but clean up this correlationId.
        operationMap.delete(requestHeader.getCorrelationId());
        log.error("[csProvider] Failed to send NSI CS terminate message, correlationId = {}, errorId = {}, text = {}",
                requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      } catch (SOAPFaultException ex) {
        // Continue on this error but clean up this correlationId.
        operationMap.delete(requestHeader.getCorrelationId());
        log.error("[csProvider] Failed to send NSI CS terminate message, correlationId = {}, SOAP Fault = {}",
                requestHeader.getCorrelationId(), ex.getFault().toString());
      } catch (Exception ex) {
        operationMap.delete(requestHeader.getCorrelationId());
        log.error("[csProvider] Failed to send NSI CS terminate message, correlationId = {}",
                requestHeader.getCorrelationId(), ex);
        throw ex;
      } finally {
        if (error) {
          Reservation r = reservationService.get(nsiProperties.getProviderNsaId(), cid);
          if (r == null) {
            // We have not seen this reservation before so ignore it.
            log.info("[CsProvider] commitDeltaReduction: no reference to reservation, cid = {}", cid);
          } else {
            // We have to determine if the stored reservation needs to be updated.
            log.info("[CsProvider] commitDeltaReduction: storing reservation update, cid = {}", cid);
            r.setReservationState(ReservationStateEnumType.RESERVE_START);
            r.setDiscovered(System.currentTimeMillis());
            reservationService.store(r);
          }
        }
      }
    }

    // Now we need to wait for the asynchronous NSI response.
    waitForOperations(correlationIds);
  }

  private void commitDeltaAddition(List<String> connectionIds)
          throws ServiceException, IllegalArgumentException, TimeoutException {
    // We store our outstanding operations here.
    List<String> correlationIds = new ArrayList<>();

    // Commit the NSI reservation first.
    for (String cid : connectionIds) {
      CommonHeaderType requestHeader = NsiHeader.builder()
              .correlationId(Helper.getUUID())
              .providerNSA(nsiProperties.getProviderNsaId())
              .requesterNSA(nsiProperties.getNsaId())
              .replyTo(nsiProperties.getRequesterConnectionURL())
              .build()
              .getRequestHeaderType();
      Holder<CommonHeaderType> header = new Holder<>();
      header.value = requestHeader;
      GenericRequestType commitBody = CS_FACTORY.createGenericRequestType();
      commitBody.setConnectionId(cid);

      // Add this to the operation map to track progress.
      Operation op = new Operation();
      op.setOperation(OperationType.reserveCommit);
      op.setState(StateType.committing);
      op.setCorrelationId(requestHeader.getCorrelationId());
      operationMap.store(op);
      correlationIds.add(requestHeader.getCorrelationId());

      boolean error = true;
      try {
        ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
        nsiClient.getProxy().reserveCommit(commitBody, header);

        log.debug("[csProvider] issued commitDelta operation correlationId = {}, connectionId = {}",
                op.getCorrelationId(), cid);
        error = false;
      } catch (ServiceException ex) {
        //TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        operationMap.delete(correlationIds);
        log.error("[csProvider] commitDelta failed to send NSI CS reserveCommit message, correlationId = {}, errorId = {}, text = {}",
                requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
        throw ex;
      } catch (SOAPFaultException soap) {
        operationMap.delete(correlationIds);
        log.error("[csProvider] commitDelta failed to send NSI CS reserveCommit message, correlationId = {}, SOAP Fault {}",
                requestHeader.getCorrelationId(), soap.getFault().toString());
        throw soap;
      } catch (Exception ex) {
        operationMap.delete(correlationIds);
        log.error("[csProvider] commitDelta failed to send NSI CS reserveCommit message, correlationId = {}",
                requestHeader.getCorrelationId(), ex);
        throw ex;
      } finally {
        if (error) {
          Reservation r = reservationService.get(nsiProperties.getProviderNsaId(), cid);
          if (r == null) {
            // We have not seen this reservation before so ignore it.
            log.info("[CsProvider] commitDeltaReduction: no reference to reservation, cid = {}", cid);
          } else {
            // We have to determine if the stored reservation needs to be updated.
            log.info("[CsProvider] commitDeltaReduction: storing reservation update, cid = {}", cid);
            r.setReservationState(ReservationStateEnumType.RESERVE_START);
            r.setDiscovered(System.currentTimeMillis());
            reservationService.store(r);
          }
        }
      }
    }

    // Now we need to wait for the asynchronous NSI commit response.
    waitForOperations(correlationIds);

    // Now we go through and provision each of these connectionIds.
    correlationIds.clear();
    for (String cid : connectionIds) {
      CommonHeaderType requestHeader = NsiHeader.builder()
              .correlationId(Helper.getUUID())
              .providerNSA(nsiProperties.getProviderNsaId())
              .requesterNSA(nsiProperties.getNsaId())
              .replyTo(nsiProperties.getRequesterConnectionURL())
              .build()
              .getRequestHeaderType();
      Holder<CommonHeaderType> header = new Holder<>();
      header.value = requestHeader;
      GenericRequestType commitBody = CS_FACTORY.createGenericRequestType();
      commitBody.setConnectionId(cid);

      // Add this to the operation map to track progress.
      Operation op = new Operation();
      op.setOperation(OperationType.provision);
      op.setState(StateType.provisioning);
      op.setCorrelationId(requestHeader.getCorrelationId());
      operationMap.store(op);
      correlationIds.add(requestHeader.getCorrelationId());

      try {
        ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
        nsiClient.getProxy().provision(commitBody, header);

        log.debug("[csProvider] issued provision operation correlationId = {}, connectionId = {}",
                op.getCorrelationId(), cid);
      } catch (ServiceException ex) {
        //TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        operationMap.delete(correlationIds);
        log.error("[csProvider] Failed to send NSI CS provision message, correlationId = {}, errorId = {}, text = {}",
                requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
        throw ex;
      } catch (SOAPFaultException soap) {
        operationMap.delete(correlationIds);
        log.error("[csProvider] Failed to send NSI CS provision message, correlationId = {}, SOAP Fault {}",
                requestHeader.getCorrelationId(), soap.getFault().toString());
        throw soap;
      } catch (Exception ex) {
        operationMap.delete(correlationIds);
        log.error("[csProvider] Failed to send NSI CS provision message, correlationId = {}",
                requestHeader.getCorrelationId(), ex);
        throw ex;
      }
    }

    // Now we need to wait for the asynchronous NSI provision response.
    waitForOperations(correlationIds);
  }

  /**
   * Wait for queued NSI operations to complete by waiting on a shared semaphore
   * between this thread and the NSI CS callback thread.
   *
   * @param correlationIds List of the outstand correlationIds the will need to complete.
   *
   * @return Will return an exception (the last encountered) if one has occurred.
   */
  private void waitForOperations(List<String> correlationIds)
          throws ServiceException, IllegalArgumentException, TimeoutException {
    Optional<Exception> exception = Optional.empty();
    for (String id : correlationIds) {
      log.info("[CsProvider] waiting for completion of correlationId = {}", id);
      if (operationMap.wait(id)) {
        Operation op = operationMap.delete(id);

        log.info("[CsProvider] operation {} completed, correlationId = {}", op.getOperation(), id);

        if (op.getOperation() == OperationType.reserve && op.getState() != StateType.reserved) {
          log.error("[CsProvider] operation failed to reserve, correlationId = {}, state = {}",
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
        log.error("[CsProvider] timeout, failed to get response for correlationId = {}", id);
        Operation op = operationMap.delete(id);
        exception = Optional.of(new TimeoutException("Operation failed to reserve, correlationId = "
                  + id + ", state = " + op.getState()));
      }
    }

    // Do I really care about the specific exception?
    if (exception.isPresent()) {
      Exception ex = exception.get();
      if (ex instanceof ServiceException) {
        throw ServiceException.class.cast(ex);
      } else if (ex instanceof IllegalArgumentException) {
        throw IllegalArgumentException.class.cast(ex);
      } else if (ex instanceof TimeoutException) {
        throw TimeoutException.class.cast(ex);
      }
    }
  }
}
