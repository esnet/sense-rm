package net.es.sense.rm.driver.nsi.cs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Holder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.cs.lib.ClientUtil;
import net.es.nsi.cs.lib.Helper;
import net.es.nsi.cs.lib.NsiHeader;
import net.es.nsi.cs.lib.SimpleLabel;
import net.es.nsi.cs.lib.SimpleStp;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.nsi.cs.db.ConnectionMap;
import net.es.sense.rm.driver.nsi.cs.db.ConnectionMapService;
import net.es.sense.rm.driver.nsi.cs.db.Operation;
import net.es.sense.rm.driver.nsi.cs.db.OperationMap;
import net.es.sense.rm.driver.nsi.cs.db.StateType;
import net.es.sense.rm.driver.nsi.cs.db.StpMapping;
import net.es.sense.rm.driver.nsi.mrml.DeltaHolder;
import net.es.sense.rm.driver.nsi.mrml.MrsBandwidthService;
import net.es.sense.rm.driver.nsi.mrml.MrsUnits;
import net.es.sense.rm.driver.nsi.mrml.NmlLabel;
import net.es.sense.rm.driver.nsi.mrml.ReserveHolder;
import net.es.sense.rm.driver.nsi.mrml.StpHolder;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
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
  // Runtime properties.

  @Autowired
  private NsiProperties nsiProperties;

  // The actor system used to send notifications.
  @Autowired
  private CsController csController;

  @Autowired
  private ConnectionMapService connectionMapService;

  @Autowired
  private OperationMap operationMap;

  private static final org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory CS_FACTORY
          = new org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory();
  private static final org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory P2PS_FACTORY
          = new org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory();

  public void init() {
    log.debug("[CsProvider] Initializing CS provider with database contents:");
  }

  public void start() {
    log.info("[CsProvider] starting...");
    csController.start();
    log.info("[CsProvider] start complete.");
  }

  public List<String> processDelta(
          net.es.sense.rm.driver.nsi.db.Model model,
          String deltaId,
          Optional<Model> reduction,
          Optional<Model> addition) throws Exception {

    List<String> connectionIds = new ArrayList<>();

    // We process the reduction first, then the addition.
    // TODO: process reduction.
    if (reduction.isPresent()) {

    }

    // Parse the addition operation.
    if (addition.isPresent()) {
      // We store our outstanding operations here.
      List<String> correlationIds = new ArrayList<>();

      DeltaHolder dh = processDeltaAddition(model, deltaId, addition.get());

      // If we have some changes apply them into the network.
      for (ReserveHolder rh : dh.getReserveList()) {
        CommonHeaderType requestHeader = NsiHeader.builder()
                .correlationId(Helper.getUUID())
                .providerNSA(nsiProperties.getProviderNsaId())
                .requesterNSA(nsiProperties.getNsaId())
                .replyTo(nsiProperties.getRequesterConnectionURL())
                .build()
                .getRequestHeaderType();

        Holder<CommonHeaderType> header = new Holder<>();
        header.value = requestHeader;

        ReserveType reserve = rh.getReserve();

        // Add this to the operation map to track progress.
        Operation op = new Operation();
        op.setState(StateType.reserving);
        op.setCorrelationId(requestHeader.getCorrelationId());
        operationMap.put(requestHeader.getCorrelationId(), op);
        correlationIds.add(requestHeader.getCorrelationId());

        // Add our NSI to MRML identifier mappings.  We will need some trickery
        // for the SwitchingSubnet as we will need to the input name to the
        // child connectionId which we do not yet know.


        try {
          ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
          ReserveResponseType response = nsiClient.getProxy().reserve(reserve, header);

          // Update the operation map with the new connectionId.
          connectionIds.add(response.getConnectionId());

          log.debug("[processDelta] issued reserve operation correlationId = {}, connectionId = {}",
                  op.getCorrelationId(), response.getConnectionId());
        } catch (ServiceException ex) {
          //TODO: Consider whether we should unwrap any NSI reservations that were successful.
          // For now just delete the correlationId we added.
          operationMap.removeAll(correlationIds);

          log.error("Failed to send NSI CS reserve message, correlationId = {}, errorId = {}, text = {}",
                  requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
          throw ex;
        }
      }

      // We have to wait for reserve operation perfomed.
      for (String id : correlationIds) {
        log.info("[CsProvider] waiting for completion of correlationId = {}", id);
        if (operationMap.wait(id)) {
          log.info("[CsProvider] operation completed, correlationId = {}", id);
          Operation op = operationMap.remove(id);
          if (op.getState() != StateType.reserved) {
            log.error("[CsProvider] operation failed to reserve, correlationId = {}, state = {}",
                    id, op.getState(), op.getException());
            operationMap.removeAll(correlationIds);
            if (op.getException() != null) {
              throw new ServiceException("Operation failed to reserve", op.getException());
            }

            throw new IllegalArgumentException("Operation failed to reserve, correlationId = "
                    + id + ", state = " + op.getState());
          }
        } else {
          log.info("[CsProvider] timeout, failed to get response for correlationId = {}", id);
          operationMap.removeAll(correlationIds);
          throw new TimeoutException("Failed to get response for correlationId = " + id);
        }
      }
    }

    return connectionIds;
  }

  public DeltaHolder processDeltaAddition(net.es.sense.rm.driver.nsi.db.Model m, String deltaId, Model addition)
          throws Exception {
    // Get the associated model.
    Model model = ModelUtil.unmarshalModel(m.getBase());

    // Populate the delta context holder.
    DeltaHolder holder = new DeltaHolder(deltaId);

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

      Statement tag = switchingSubnet.getProperty(Mrs.tag);
      String description = tag.getString();
      log.debug("description: " + description);

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
        r.setDescription(description);
        r.setCriteria(rrc);

        ReserveHolder rh = new ReserveHolder();
        rh.setReserve(r);
        rh.setSwitchingSubnetId(switchingSubnet.getURI());
        rh.getPorts().put(src.getMrsPortId(), src);
        rh.getPorts().put(dst.getMrsPortId(), dst);

        holder.addReserve(rh);

        // Now store the mapping for this SwitchingSubnet.
        ConnectionMap cm = new ConnectionMap();
        cm.setGlobalReservationId(switchingSubnet.getURI());
        cm.setSwitchingSubnetId(switchingSubnet.getURI());
        cm.setTag(description);
        StpMapping smSrc = new StpMapping(src.getStp().getStpId(), src.getMrsPortId(),
                src.getmrsLabelId(), src.getBw().getId());
        cm.getMap().add(smSrc);
        StpMapping smDst = new StpMapping(dst.getStp().getStpId(), dst.getMrsPortId(),
                dst.getmrsLabelId(), dst.getBw().getId());
        cm.getMap().add(smDst);
        connectionMapService.store(cm);
      } else {
        log.error("serviceType not supported {}", serviceTypeRef.getString());
        throw new IllegalArgumentException("serviceType not supported " + serviceTypeRef.getString());
      }
    }

    return holder;
  }

  public List<String> commitDelta(String deltaId, List<String> connectionId) throws ServiceException, IllegalArgumentException, TimeoutException {
    // We store our outstanding operations here.
    List<String> correlationIds = new ArrayList<>();

    for (String cid : connectionId) {
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
      operationMap.put(requestHeader.getCorrelationId(), op);
      correlationIds.add(requestHeader.getCorrelationId());

      try {
        ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
        nsiClient.getProxy().reserveCommit(commitBody, header);

        log.debug("[csProvider] issued commitDelta operation correlationId = {}, connectionId = {}",
                op.getCorrelationId(), cid);
      } catch (ServiceException ex) {
        //TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        operationMap.removeAll(correlationIds);

        log.error("Failed to send NSI CS reserveCommit message, correlationId = {}, errorId = {}, text = {}",
                requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
        throw ex;
      }
    }

    // We have to wait for reserve operation perfomed.
    for (String id : correlationIds) {
      log.info("[CsProvider] reserveCommit waiting for completion of correlationId = {}", id);
      if (operationMap.wait(id)) {
        log.info("[CsProvider] reserveCommit operation completed, correlationId = {}", id);
        Operation op = operationMap.remove(id);
        if (op.getState() != StateType.committed) {
          log.error("[CsProvider] operation failed to reserveCommit, correlationId = {}, state = {}",
                  id, op.getState(), op.getException());
          operationMap.removeAll(correlationIds);
          if (op.getException() != null) {
            throw new ServiceException("Operation failed to reserveCommit", op.getException());
          }

          throw new IllegalArgumentException("Operation failed to reserveCommit, correlationId = "
                  + id + ", state = " + op.getState());
        }
      } else {
        log.info("[CsProvider] timeout, failed to get response for correlationId = {}", id);
        operationMap.removeAll(correlationIds);
        throw new TimeoutException("Failed to get response for correlationId = " + id);
      }
    }

    // Now we go through and provision each of these connectionIds.
    correlationIds.clear();
    for (String cid : connectionId) {
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
      operationMap.put(requestHeader.getCorrelationId(), op);
      correlationIds.add(requestHeader.getCorrelationId());

      try {
        ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
        nsiClient.getProxy().provision(commitBody, header);

        log.debug("[csProvider] issued provision operation correlationId = {}, connectionId = {}",
                op.getCorrelationId(), cid);
      } catch (ServiceException ex) {
        //TODO: Consider whether we should unwrap any NSI reservations that were successful.
        // For now just delete the correlationId we added.
        operationMap.removeAll(correlationIds);

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
        Operation op = operationMap.remove(id);
        if (op.getState() != StateType.provisioned) {
          log.error("[CsProvider] operation failed to provision, correlationId = {}, state = {}",
                  id, op.getState(), op.getException());
          operationMap.removeAll(correlationIds);
          if (op.getException() != null) {
            throw new ServiceException("Operation failed to provision", op.getException());
          }

          throw new IllegalArgumentException("Operation failed to provision, correlationId = "
                  + id + ", state = " + op.getState());
        }
      } else {
        log.info("[CsProvider] timeout, failed to get provision response for correlationId = {}", id);
        operationMap.removeAll(correlationIds);
        throw new TimeoutException("Failed to get provision response for correlationId = " + id);
      }
    }

    return connectionId;
  }
}
