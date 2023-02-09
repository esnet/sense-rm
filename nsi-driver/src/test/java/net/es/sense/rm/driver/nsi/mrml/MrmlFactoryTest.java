package net.es.sense.rm.driver.nsi.mrml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.dds.lib.jaxb.nml.NmlTopologyType;
import net.es.sense.rm.driver.nsi.cs.db.ConnectionMapService;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import net.es.sense.rm.driver.nsi.dds.api.DocumentReader;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class MrmlFactoryTest {

  private final static String DOCUMENT_FULL = "src/test/resources/topology_full.xml";
  private final static String DOCUMENT_WILDCARD = "src/test/resources/topology_ss_wildcard.xml";
  private final static String DOCUMENT_ESNET = "src/test/resources/topology_esnet.xml";
  private final static String DOCUMENT_MANLAN = "src/test/resources/topology_manlan.xml";
  private final static String DOCUMENT_WIX = "src/test/resources/topology_wix.xml";
  private final static String DOCUMENT_CALTECH = "src/test/resources/topology_caltech.xml";
  private final static String DOCUMENT_NETHERLIGHT = "src/test/resources/topology_netherlight.xml";
  private final static String DOCUMENT_PACWAVE = "src/test/resources/topology_pacwave.xml";
  private final static String NEWORK_ID = "urn:ogf:network:es.net:2013:";

  public NmlTopologyType load(String path) throws IOException, JAXBException {
    File file = new File(path);
    File absoluteFile = file.getAbsoluteFile();
    try (InputStream is = new FileInputStream(absoluteFile)) {
      return XmlUtilities.xmlToJaxb(NmlTopologyType.class, is);
    }
  }

  @Test
  public void testCreateOntologyModel() throws IOException, JAXBException {
    Collection<NmlTopologyType> nml = Lists.newArrayList(load(DOCUMENT_FULL));
    DocumentReader drMock = Mockito.mock(DocumentReader.class);
    Mockito.when(drMock.getNmlTopologyAll()).thenReturn(Lists.newArrayList(nml));
    Mockito.when(drMock.getTopologyById(NEWORK_ID)).thenReturn(Lists.newArrayList(nml));

    ConnectionMapService cmMock = Mockito.mock(ConnectionMapService.class);
    Mockito.when(cmMock.getByDeltaId(ArgumentMatchers.anyString())).thenReturn(null);

    log.info("[testCreateOntologyModel] building NML model");
    NmlModel model = new NmlModel(drMock);
    model.setDefaultServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");
    model.setDefaultGranularity(1L);
    model.setDefaultType(MrsBandwidthType.guaranteed);
    model.setDefaultUnits("bps");

    log.info("[testCreateOntologyModel] building SwitchingSubnet");
    ReservationService rsMock = Mockito.mock(ReservationService.class);
    SwitchingSubnetModel ssm = new SwitchingSubnetModel(rsMock, cmMock, model, NEWORK_ID);

    log.info("[testCreateOntologyModel] building MRML model");
    MrmlFactory mrmlFactory = new MrmlFactory(model, ssm, NEWORK_ID);
    OntModel mrml = mrmlFactory.getOntologyModel();
    RDFDataMgr.write(System.out, mrml.getBaseModel(), Lang.TURTLE);
  }

  @Test
  public void testWidlcardSwitchingService() throws IOException, JAXBException {
    Collection<NmlTopologyType> nml = Lists.newArrayList(load(DOCUMENT_WILDCARD));
    DocumentReader drMock = Mockito.mock(DocumentReader.class);
    Mockito.when(drMock.getNmlTopologyAll()).thenReturn(Lists.newArrayList(nml));
    Mockito.when(drMock.getTopologyById(NEWORK_ID)).thenReturn(Lists.newArrayList(nml));

    ConnectionMapService cmMock = Mockito.mock(ConnectionMapService.class);
    Mockito.when(cmMock.getByDeltaId(ArgumentMatchers.anyString())).thenReturn(null);

    log.info("[testWidlcardSwitchingService] building NML model");
    NmlModel model = new NmlModel(drMock);
    model.setDefaultServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");
    model.setDefaultGranularity(1L);
    model.setDefaultType(MrsBandwidthType.guaranteed);
    model.setDefaultUnits("bps");

    log.info("[testCreateOntologyModel] building SwitchingSubnet");
    ReservationService rsMock = Mockito.mock(ReservationService.class);
    SwitchingSubnetModel ssm = new SwitchingSubnetModel(rsMock, cmMock, model, NEWORK_ID);

    log.info("[testWidlcardSwitchingService] building MRML model");
    MrmlFactory mrmlFactory = new MrmlFactory(model, ssm, NEWORK_ID);
    OntModel mrml = mrmlFactory.getOntologyModel();
    RDFDataMgr.write(System.out, mrml.getBaseModel(), RDFFormat.TURTLE_PRETTY);
  }

  @Test
  public void testLargeTopology() throws IOException, JAXBException {
    Collection<NmlTopologyType> nml = Lists.newArrayList(
            load(DOCUMENT_ESNET), load(DOCUMENT_WIX), load(DOCUMENT_MANLAN), load(DOCUMENT_PACWAVE),
            load(DOCUMENT_CALTECH), load(DOCUMENT_NETHERLIGHT));
    DocumentReader drMock = Mockito.mock(DocumentReader.class);
    Mockito.when(drMock.getNmlTopologyAll()).thenReturn(Lists.newArrayList(nml));
    Mockito.when(drMock.getTopologyById(NEWORK_ID)).thenReturn(Lists.newArrayList(nml));

    ConnectionMapService cmMock = Mockito.mock(ConnectionMapService.class);
    Mockito.when(cmMock.getByDeltaId(ArgumentMatchers.anyString())).thenReturn(null);

    log.info("[testLargeTopology] building NML model");
    NmlModel model = new NmlModel(drMock);
    model.setDefaultServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");
    model.setDefaultGranularity(1L);
    model.setDefaultType(MrsBandwidthType.guaranteed);
    model.setDefaultUnits("bps");

    log.info("[testCreateOntologyModel] building SwitchingSubnet");
    ReservationService rsMock = Mockito.mock(ReservationService.class);
    SwitchingSubnetModel ssm = new SwitchingSubnetModel(rsMock, cmMock, model, NEWORK_ID);

    log.info("[testLargeTopology] building MRML model");
    MrmlFactory mrmlFactory = new MrmlFactory(model, ssm, NEWORK_ID);
    OntModel mrml = mrmlFactory.getOntologyModel();
    RDFDataMgr.write(System.out, mrml.getBaseModel(), Lang.TURTLE);
  }
}
