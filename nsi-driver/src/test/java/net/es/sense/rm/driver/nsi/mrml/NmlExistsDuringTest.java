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
package net.es.sense.rm.driver.nsi.mrml;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlDate;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.schema.Mrs;
import net.es.sense.rm.driver.schema.Nml;
import net.es.sense.rm.driver.schema.Rdf;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.ValidityReport;
import org.junit.*;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class NmlExistsDuringTest {

  static final String URN = NmlExistsDuring.id("urn:ogf:network:snvaca.pacificwave.net:2016:topology:EVTS.A-GOLE:conn+ef8019ef-5d19-4b4b-9460-570a9cdfc1de");
  static final String START = "2017-06-05T12:16:00.000-07:00";
  static final String END = "2018-06-05T12:31:00.000-07:00";
  static OntModel model;

  public NmlExistsDuringTest() {
  }

  @BeforeClass
  public static void setUpClass() throws IOException {
    log.debug("NmlExistsDuringTest: Setting up model...");

    model = ModelUtil.newMrmlModel();
    model.read("src/test/resources/lifetime.ttl", "TURTLE") ;

    log.debug("NmlExistsDuringTest: Loaded model:\n{}", ModelUtil.marshalOntModel(model));

    ValidityReport validate = model.validate();
    log.debug("Is model valid = {}", validate.isValid());
    validate.getReports().forEachRemaining(r -> {
      log.debug("{}: {}", r.getType(), r.getDescription());
    });

    log.debug("NmlExistsDuringTest: Setup complete...");
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  private ResultSet localSparqlQuery(OntModel model, String sparqlStringWithoutPrefix) {
    String sparqlString
        = "prefix rdf:   <" + Rdf.getURI() + ">\n"
        + "prefix mrs:   <" + Mrs.getURI() + ">\n"
        + sparqlStringWithoutPrefix;

    log.debug("[getResourceOfType] sparql query:\n{}", sparqlString);

    Query query = QueryFactory.create(sparqlString);
    ResultSet rs;
    try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
      rs = qexec.execSelect();
    } catch (Exception ex) {
      log.error("[sparqlQuery] exception encountered", ex);
      throw ex;
    }
    log.debug("[sparqlQuery] hasResult = {}", rs.hasNext());
    ResultSetFormatter.out(System.out, rs, query);
    return rs;
  }


  private Resource getExistsDuring(String object, OntClass predicate) throws IOException {
    Resource switchingSubnet = ModelUtil.getResourceOfSubjectAndType(model, predicate, object);
    log.debug("[getExistsDuring]  switchingSubnet2 {}", switchingSubnet);
    assert switchingSubnet != null;
    Statement property = switchingSubnet.getProperty(Nml.existsDuring);
    assert property != null;
    return property.getResource();
  }

  /**
   * Test of getId method, of class NmlExistsDuring.
   *
   * @throws javax.xml.datatype.DatatypeConfigurationException
   */
  @Test
  public void testGetId() throws DatatypeConfigurationException, IOException {
    // Create a new NmlExistsDuring and verify proper creation.
    NmlExistsDuring ed = new NmlExistsDuring(URN);
    assertEquals(URN, ed.getId());
    assertNull(ed.getStart());
    assertNotNull(ed.getEnd());

    log.debug("[testGetId] NmlExistsDuring {}", ed);

    List<Resource> resourcesOfType = ModelUtil.getResourcesOfType(model, Mrs.SwitchingSubnet);
    assert !resourcesOfType.isEmpty();

    // Verify correct behavior for embedded ExistsDuring resource.
    Resource existsDuring = getExistsDuring(
        "urn:ogf:network:es.net:2013::switchingSubnet+8bfe48cd-1b26-43ed-950c-a623e021a5d0",
        Mrs.SwitchingSubnet);

    ed = new NmlExistsDuring(existsDuring);
    assertNull(ed.getId());
    assertEquals(ed.getStart(), XmlDate.xmlGregorianCalendar(START));
    assertEquals(ed.getEnd(), XmlDate.xmlGregorianCalendar(END));

    log.debug("[testGetId] NmlExistsDuring {}", ed);
  }

  /**
   * Test of getStart method, of class NmlExistsDuring.
   * @throws javax.xml.datatype.DatatypeConfigurationException
   */
  @Test
  public void testGetStart() throws DatatypeConfigurationException {
    NmlExistsDuring ed = new NmlExistsDuring(URN);
    assertNull(ed.getStart());
  }

  /**
   * Test of getPaddedStart method, of class NmlExistsDuring.
   * @throws javax.xml.datatype.DatatypeConfigurationException
   */
  @Test
  public void testGetPaddedStart() throws DatatypeConfigurationException {
    NmlExistsDuring ed = new NmlExistsDuring(URN);
    assertNull(ed.getPaddedStart());
  }

  /**
   * Test of getEnd method, of class NmlExistsDuring.
   * @throws javax.xml.datatype.DatatypeConfigurationException
   */
  @Test
  public void testGetEnd() throws DatatypeConfigurationException {
    NmlExistsDuring ed = new NmlExistsDuring(URN);
    assertEquals(URN, ed.getId());
    assertNotNull(ed.getEnd());
  }
}
