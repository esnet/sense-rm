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

import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class ModelOperationsTest {

  private static final String MODEL_FILE_5 = "src/test/resources/model-5.ttl";
  private static final String ADDITION_FILE_5 = "src/test/resources/addition-5.ttl";
  private static final String REDUCTION_FILE_5 = "src/test/resources/reduction-5.ttl";

  @Test
  public void test() throws IOException {
    log.debug("[ModelOperationsTest::test] starting...");
    // Load the base model we will be manipulating.
    OntModel base = ModelUtil.unmarshalOntModelFromFile(MODEL_FILE_5, Lang.TURTLE.getLabel());

    // Load the requested addition model.
    OntModel addition = ModelUtil.unmarshalOntModelFromFile(ADDITION_FILE_5, Lang.TURTLE.getLabel());

    // Apply the addition to the base model.
    base.add(addition);

    // Determine the subjects listed in the addition.
    List<Resource> subjects = ModelUtil.getSubjects(addition);

    // Determine what resource type each of these subjects refer to.
    subjects.forEach(s -> {
      log.debug("[ModelOperationsTest::test] Addition subject: {}", s.getURI());
      Resource resource = base.getResource(s.getURI());
      log.debug("[ModelOperationsTest::test] Found resource: {}", resource.getURI());

      if (ModelUtil.isBidirectionalPort(resource)) {
        log.debug("[ModelOperationsTest::test] Nml.BidirectionalPort");
      } else if (ModelUtil.isSwitchingService(resource)) {
        log.debug("[ModelOperationsTest::test] Mrs.SwitchingService");
      } else if (ModelUtil.isSwitchingSubnet(resource)) {
        log.debug("[ModelOperationsTest::test] Nml.SwitchingSubnet");
      } else if (ModelUtil.isBandwidthService(resource)) {
        log.debug("[ModelOperationsTest::test] Mrs.BandwidthService");
      } else {
        log.debug("[ModelOperationsTest::test] Not interested in {}", resource.getURI());
      }
    });

    // Load the requested reduction model.
    Model reduction = ModelUtil.unmarshalOntModelFromFile(REDUCTION_FILE_5, Lang.TURTLE.getLabel());

    HashMap<String, Resource> results =  ModelOperations.getModifiedBandwidthServices(addition, reduction);
    results.values().forEach(resource -> {
      log.debug("[ModelOperationsTest::test] endpoint: {}", resource.getURI());
    });

    //log.debug(ModelUtil.marshalModel(model));

    log.debug("[ModelOperationsTest::test] completed!");
  }
}