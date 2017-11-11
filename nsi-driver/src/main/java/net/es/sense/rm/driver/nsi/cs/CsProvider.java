package net.es.sense.rm.driver.nsi.cs;

import java.util.ArrayList;
import java.util.List;
import javax.xml.ws.Holder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.cs.lib.Client;
import net.es.nsi.cs.lib.Helper;
import net.es.nsi.cs.lib.NsiHeader;
import net.es.nsi.cs.lib.SimpleLabel;
import net.es.nsi.cs.lib.SimpleStp;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.nsi.cs.db.Operation;
import net.es.sense.rm.driver.nsi.cs.db.OperationService;
import net.es.sense.rm.driver.nsi.cs.db.OperationType;
import net.es.sense.rm.driver.nsi.db.ModelService;
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
import net.es.sense.rm.model.DeltaRequest;
import net.es.sense.rm.model.DeltaResource;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException;
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
  private ModelService modelService;

  @Autowired
  private OperationService operationService;

  private static final org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory CS_FACTORY
          = new org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory();
  private static final org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory P2PS_FACTORY
          = new org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory();

  private Client nsiClient;

  public void init() {
    log.debug("[CsProvider] Initializing CS provider with database contents:");
    nsiClient = new Client(nsiProperties.getProviderConnectionURL());
  }

  public void start() {
    log.info("[CsProvider] starting...");
    csController.start();
    log.info("[CsProvider] start complete.");
  }

  public DeltaResource processDelta(DeltaRequest request) throws Exception {
    net.es.sense.rm.driver.nsi.db.Model model = modelService.get(request.getModelId());

    // Parse the addition operation.
    if (!Strings.isNullOrEmpty(request.getAddition())) {
      DeltaHolder dh = processDeltaAddition(model, request);

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

        // Add this to the operation database to track progress.
        Operation op = new Operation();
        op.setOp(OperationType.reserve);
        op.setGlobalReservationId(reserve.getGlobalReservationId());
        op.setCorrelationId(requestHeader.getCorrelationId());
        op = operationService.store(op);

        try {
          ReserveResponseType response = nsiClient.getProxy().reserve(reserve, header);

          op = operationService.getByCorrelationId(op.getCorrelationId());
          op.setConnectionId(response.getConnectionId());
          op = operationService.store(op);
          log.debug("[processDelta] issued reserve operation correlationId = {}, globalReservationId = {}, connectionId = {}",
                  op.getCorrelationId(), op.getGlobalReservationId(), op.getConnectionId());
        } catch (ServiceException ex) {
          log.error("Failed to send NSI CS reserve message, correlationId = {}, errorId = {}, text = {}",
                  requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
          throw ex;

        }
      }

      //TODO: Johnny is here.

      // We need to store the id mapping information.

      // Now we formulate a deltaResponse.

    }

    return new DeltaResource();
  }

  public DeltaHolder processDeltaAddition(net.es.sense.rm.driver.nsi.db.Model m, DeltaRequest request) throws Exception {
    // Get the associated model.


    Model model = ModelUtil.unmarshalModel(m.getBase());

    // Parse the delta addition.
    Model addition = ModelUtil.unmarshalModel(request.getAddition());

    // Populate the delta context holder.
    DeltaHolder holder = new DeltaHolder(request);

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

          stps.add(new StpHolder(biChild.getURI(), stp, bws));
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
        sch.setStartTime(CS_FACTORY.createScheduleTypeStartTime(XmlUtilities.xmlGregorianCalendar()));

        ReservationRequestCriteriaType rrc = CS_FACTORY.createReservationRequestCriteriaType();
        rrc.setVersion(0);
        rrc.setSchedule(sch);
        rrc.setServiceType(Nsi.NSI_SERVICETYPE_EVTS);
        rrc.getAny().add(P2PS_FACTORY.createP2Ps(p2ps));

        ReserveType r = CS_FACTORY.createReserveType();
        r.setGlobalReservationId(Helper.getUUID());
        r.setDescription(description);
        r.setCriteria(rrc);

        ReserveHolder rh = new ReserveHolder();
        rh.setReserve(r);
        rh.setSwitchingSubnetId(switchingSubnet.getURI());
        rh.getPorts().put(src.getMrsPortId(), src);
        rh.getPorts().put(dst.getMrsPortId(), dst);

        holder.addReserve(rh);
      } else {
        log.error("serviceType not supported {}", serviceTypeRef.getString());
        throw new IllegalArgumentException("serviceType not supported " + serviceTypeRef.getString());
      }
    }

    return holder;
  }
}
