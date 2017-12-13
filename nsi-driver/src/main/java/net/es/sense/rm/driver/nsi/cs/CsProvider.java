package net.es.sense.rm.driver.nsi.cs;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.cs.lib.ClientUtil;
import net.es.nsi.cs.lib.Helper;
import net.es.nsi.cs.lib.NsiHeader;
import net.es.nsi.cs.lib.SimpleLabel;
import net.es.nsi.cs.lib.SimpleStp;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.nsi.actors.NsiActorSystem;
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
import net.es.sense.rm.driver.nsi.mrml.MrsUnits;
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
import org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.GenericRequestType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationRequestCriteriaType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveResponseType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveType;
import org.ogf.schemas.nsi._2013._12.connection.types.ScheduleType;
import org.ogf.schemas.nsi._2013._12.framework.headers.CommonHeaderType;
import org.ogf.schemas.nsi._2013._12.services.point2point.P2PServiceBaseType;
import org.ogf.schemas.nsi._2013._12.services.types.DirectionalityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
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
  private static final org.ogf.schemas.nsi._2013._12.framework.types.ObjectFactory FWK_FACTORY
          = new org.ogf.schemas.nsi._2013._12.framework.types.ObjectFactory();

  public void start() {
    // Initialize the actors.
    log.info("[CsProvider] Starting NSI CS system initialization...");
    ActorSystem actorSystem = nsiActorSystem.getActorSystem();

    try {
      connectionActor = actorSystem.actorOf(springExtension.props("connectionActor"), "nsi-connectionActor");
    } catch (Exception ex) {
      log.error("[CsProvider] Failed to initialize actor", ex);
    }

    log.info("[CsProvider] Completed NSI CS system initialization.");
  }

  public ActorRef GetConnectionActor() {
    return connectionActor;
  }

  public void terminate() {
    nsiActorSystem.shutdown(connectionActor);
  }

  public void processDelta(
          net.es.sense.rm.driver.nsi.db.Model model,
          String deltaId,
          Optional<Model> reduction,
          Optional<Model> addition) throws Exception {

    Optional<Exception> exception = Optional.empty();

    // We process the reduction first, then the addition.
    if (reduction.isPresent()) {
      List<String> correlationIds = processDeltaReduction(model, deltaId, reduction.get());

      // We have to wait for reserve operation perfomed.
      for (String id : correlationIds) {
        log.info("[CsProvider] waiting for terminate of correlationId = {}", id);
        if (operationMap.wait(id)) {
          Operation op = operationMap.delete(id);

          log.info("[CsProvider] operation {} completed, correlationId = {}", op.getOperation(), id);

          if (op.getOperation() == OperationType.terminate && op.getState() != StateType.terminated) {
            if (op.getException() != null) {
              log.error("[CsProvider] operation failed to terminate, correlationId = {}, state = {}",
                    id, op.getState(), op.getException());
              exception = Optional.of(new ServiceException("Operation failed to terminate", op.getException()));
            } else {
              exception = Optional.of(new IllegalArgumentException("Operation failed to reserve, correlationId = "
                    + id + ", state = " + op.getState()));
            }
          }
        } else {
          log.error("[CsProvider] timeout, failed to get response for correlationId = {}", id);
          Operation op = operationMap.delete(id);
          exception = Optional.of(new TimeoutException("Operation failed to terminate, correlationId = "
                    + id + ", state = " + op.getState()));
        }
      }
    }

    // Parse the addition operation.
    if (addition.isPresent()) {
      List<String> correlationIds = processDeltaAddition(model, deltaId, addition.get());

      // We have to wait for reserve operation perfomed.
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
    }

    if (exception.isPresent()) {
      throw exception.get();
    }
  }

  public List<String> processDeltaReduction(net.es.sense.rm.driver.nsi.db.Model m, String deltaId, Model reduction)
          throws Exception {

    // CorrelationId from NSI termination request go in here.
    List<String> correlationIds = new ArrayList<>();

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
      Collection<Reservation> reservations = reservationService.getByGlobalReservationId(ssid);

      for (Reservation reservation : reservations) {
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
        terminate.setConnectionId(reservation.getConnectionId());

        // Add this to the operation map to track progress.
        Operation op = new Operation();
        op.setState(StateType.terminating);
        op.setCorrelationId(requestHeader.getCorrelationId());
        operationMap.store(op);
        correlationIds.add(requestHeader.getCorrelationId());

        // Issue the NSI reservation request.
        try {
          ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
          nsiClient.getProxy().terminate(terminate, header);
          log.debug("[processDelta] issued terminate operation correlationId = {}, connectionId = {}",
                  op.getCorrelationId(), terminate.getConnectionId());
        } catch (ServiceException ex) {
          // Continue on this error as we might as well clean up as much as
          // possible.
          operationMap.delete(correlationIds);

          log.error("Failed to send NSI CS terminate message, correlationId = {}, errorId = {}, text = {}",
                  requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
        }
      }
    }

    return correlationIds;
  }

  public List<String> processDeltaAddition(net.es.sense.rm.driver.nsi.db.Model m, String deltaId, Model addition)
          throws Exception {
    // This is a list of connectionId associated with reservations created
    // as part of the delta.
    DeltaConnection connectionIds = new DeltaConnection();
    connectionIds.setDeltaId(deltaId);

    // CorrelationId from NSI these reservation requests go in here.
    List<String> correlationIds = new ArrayList<>();

    // Get the associated model.
    Model model = ModelUtil.unmarshalModel(m.getBase());

    // Apply the delta to our reference model so we can search with proposed changes.
    ModelUtil.applyDeltaAddition(model, addition);

    // We model connections as mrs:SwitchingSubnet objects so query the
    // addition model for all those provided.
    ResultSet ssSet = ModelUtil.getResourcesOfType(addition, Mrs.SwitchingSubnet);

    while (ssSet.hasNext()) {
      QuerySolution querySolution = ssSet.next();

      // Get the SwitchingSubnet resource.
      Resource switchingSubnet = querySolution.get("resource").asResource();
      log.debug("SwitchingSubnet: " + switchingSubnet.getURI());

      //Statement tag = switchingSubnet.getProperty(Mrs.tag);
      //String description = tag.getString();
      //log.debug("description: " + description);
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

      if (Nsi.NSI_SERVICETYPE_EVTS.equalsIgnoreCase(serviceTypeRef.getString().trim())) {
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

          // Get the MrsBandwidthService assocated with this BidirectionalPort.
          Statement bwServiceRef = biChild.getProperty(Nml.hasService);
          Resource bwService = ModelUtil.getResourceOfType(addition, bwServiceRef.getResource(), Mrs.BandwidthService);
          MrsBandwidthService bws = new MrsBandwidthService(bwService);

          log.debug("BandwidthService: {}", bws.getId());
          log.debug("maximumCapacity: {} {}", bws.getMaximumCapacity(), bws.getUnit());
          log.debug("maximumCapacity: {} mbps", MrsUnits.normalize(bws.getMaximumCapacity(), bws.getUnit(), MrsUnits.mbps));

          // Now get the label for this port.
          Statement labelRef = biChild.getProperty(Nml.hasLabel);
          Resource label = ModelUtil.getResourceOfType(addition, labelRef.getResource(), Nml.Label);

          NmlLabel nmlLabel = new NmlLabel(label);
          SimpleLabel simpleLabel = nmlLabel.getSimpleLabel();

          Resource parentBi = ModelUtil.getParentBidirectionalPort(model, biChild);
          log.debug("parentBi: " + parentBi.getURI());

          SimpleStp stp = new SimpleStp(parentBi.getURI(), simpleLabel);
          log.debug("stpId: {}", stp.getStpId());

          stps.add(new StpHolder(biChild.getURI(), stp, bws, label.getURI()));
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

        ScheduleType sch = CS_FACTORY.createScheduleType();
        //sch.setStartTime(CS_FACTORY.createScheduleTypeStartTime(XmlUtilities.xmlGregorianCalendar()));
        XMLGregorianCalendar endTime = XmlUtilities.xmlGregorianCalendar();
        endTime.setYear(endTime.getYear() + 1);
        sch.setEndTime(CS_FACTORY.createScheduleTypeEndTime(endTime));

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
        StpMapping smSrc = new StpMapping(src.getStp().getStpId(), src.getMrsPortId(),
                src.getmrsLabelId(), src.getBw().getId());
        cm.getMap().add(smSrc);
        StpMapping smDst = new StpMapping(dst.getStp().getStpId(), dst.getMrsPortId(),
                dst.getmrsLabelId(), dst.getBw().getId());
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
        op.setState(StateType.reserving);
        op.setCorrelationId(requestHeader.getCorrelationId());
        operationMap.store(op);

        correlationIds.add(requestHeader.getCorrelationId());

        // Issue the NSI reservation request.
        try {
          ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
          ReserveResponseType response = nsiClient.getProxy().reserve(r, header);

          // Update the operation map with the new connectionId.
          connectionIds.getConnections().add(response.getConnectionId());

          log.debug("[processDelta] issued reserve operation correlationId = {}, connectionId = {}",
                  op.getCorrelationId(), response.getConnectionId());
        } catch (ServiceException ex) {
          //TODO: Consider whether we should unwrap any NSI reservations that were successful.
          // For now just delete the correlationId we added.
          operationMap.delete(correlationIds);

          log.error("Failed to send NSI CS reserve message, correlationId = {}, errorId = {}, text = {}",
                  requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
          throw ex;
        }
      } else {
        log.error("serviceType not supported {}", serviceTypeRef.getString());
        throw new IllegalArgumentException("serviceType not supported " + serviceTypeRef.getString());
      }
    }

    if (!connectionIds.getConnections().isEmpty()) {
      deltaMap.store(connectionIds);
    }

    return correlationIds;
  }

  public void commitDelta(String deltaId) throws ServiceException, IllegalArgumentException, TimeoutException,
          SOAPFaultException {
    // We store our outstanding operations here.
    List<String> correlationIds = new ArrayList<>();

    // Look up the connection identifiers assoicated with this deltaId.
    DeltaConnection connectionIds = deltaMap.delete(deltaId);
    if (connectionIds == null) {
      log.debug("[csProvider] commitDelta could not find connectionIds associated with deltaId = {}", deltaId);
      throw new IllegalArgumentException("Could not find connections for deltaId = " + deltaId);
    }

    for (String cid : connectionIds.getConnections()) {
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
      op.setState(StateType.committing);
      op.setCorrelationId(requestHeader.getCorrelationId());
      operationMap.store(op);
      correlationIds.add(requestHeader.getCorrelationId());

      try {
        ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
        nsiClient.getProxy().reserveCommit(commitBody, header);

        log.debug("[csProvider] issued commitDelta operation correlationId = {}, connectionId = {}",
                op.getCorrelationId(), cid);
      } catch (ServiceException ex) {
        //TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        operationMap.delete(correlationIds);

        log.error("[csProvider] commitDelta failed to send NSI CS reserveCommit message, correlationId = {}, errorId = {}, text = {}",
                requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
        throw ex;
      } catch (SOAPFaultException soap) {
        log.error("[csProvider] commitDelta encountered a SOAP Fault", soap);
        throw soap;
      } catch (Exception ex) {
        log.error("[csProvider] commitDelta encountered unexpected error = {}", ex);
        throw ex;
      }
    }

    // We have to wait for reserve operation perfomed.
    for (String id : correlationIds) {
      log.info("[CsProvider] reserveCommit waiting for completion of correlationId = {}", id);
      if (operationMap.wait(id)) {
        log.info("[CsProvider] reserveCommit operation completed, correlationId = {}", id);
        Operation op = operationMap.delete(id);
        if (op.getState() != StateType.committed) {
          log.error("[CsProvider] operation failed to reserveCommit, correlationId = {}, state = {}",
                  id, op.getState(), op.getException());
          operationMap.delete(correlationIds);
          if (op.getException() != null) {
            throw new ServiceException("Operation failed to reserveCommit", op.getException());
          }

          throw new IllegalArgumentException("Operation failed to reserveCommit, correlationId = "
                  + id + ", state = " + op.getState());
        }
      } else {
        log.info("[CsProvider] timeout, failed to get response for correlationId = {}", id);
        operationMap.delete(correlationIds);
        throw new TimeoutException("Failed to get response for correlationId = " + id);
      }
    }

    // Now we go through and provision each of these connectionIds.
    correlationIds.clear();
    for (String cid : connectionIds.getConnections()) {
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

        log.error("Failed to send NSI CS provision message, correlationId = {}, errorId = {}, text = {}",
                requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
        throw ex;
      }
    }

    // We have to wait for reserve operation perfomed.
    for (String id : correlationIds) {
      log.info("[CsProvider] provision waiting for completion of correlationId = {}", id);
      if (operationMap.wait(id)) {
        log.info("[CsProvider] provision operation completed, correlationId = {}", id);
        Operation op = operationMap.delete(id);
        if (op.getState() != StateType.provisioned) {
          log.error("[CsProvider] operation failed to provision, correlationId = {}, state = {}",
                  id, op.getState(), op.getException());
          operationMap.delete(correlationIds);
          if (op.getException() != null) {
            throw new ServiceException("Operation failed to provision", op.getException());
          }

          throw new IllegalArgumentException("Operation failed to provision, correlationId = "
                  + id + ", state = " + op.getState());
        }
      } else {
        log.info("[CsProvider] timeout, failed to get provision response for correlationId = {}", id);
        operationMap.delete(correlationIds);
        throw new TimeoutException("Failed to get provision response for correlationId = " + id);
      }
    }
  }
}
