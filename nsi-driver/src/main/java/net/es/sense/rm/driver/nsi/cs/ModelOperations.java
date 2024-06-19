package net.es.sense.rm.driver.nsi.cs;

import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.schema.Mrs;
import net.es.sense.rm.driver.schema.Nml;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
public class ModelOperations {

  public static HashMap<String, Resource> getModifiedBandwidthServices(OntModel addition, Model reduction) {
    log.debug("[getModifiedBandwidthServices] entering...");

    NodeIterator resIterator = addition.listObjects();
    while (resIterator.hasNext()) {
      RDFNode resource = resIterator.next();
      log.debug("[processServiceModifications] : " + resource.toString());
      if (resource.isResource()) {
        log.debug("[processServiceModifications] found URI resource: " + resource.asResource().getURI());
      }
    }

    // Handle bandwidth modifications first.  A bandwidth modification will be presented as a
    // change in attributes of the mrs:BandwidthService resource.  For this modification the
    // addition model SHOULD NOT have a nml:BidirectionalPort referencing the mrs:BandwidthService
    // resource via a nml:hasService relationship (should distinguish ).
    HashMap<String, Resource> bandwidthServiceMap = new LinkedHashMap<>();

    // Find all mrs:BandwidthService resources in the addition model.
    List<Resource> bsSet = ModelUtil.getResourcesOfType(addition, Mrs.BandwidthService);
    for (Resource bandwidthService : bsSet) {

      // Get the BandwidthService resource.
      log.debug("[processServiceModifications] found BandwidthService: " + bandwidthService.getURI());
      bandwidthServiceMap.put(bandwidthService.getURI(), bandwidthService);
    }

    // Now we find all the nml:BidirectionalPort entries and remove the corresponding
    // mrs:BandwidthService resources from our list to get our final modification list.
    List<Resource> bpSet = ModelUtil.getResourcesOfType(addition, Nml.BidirectionalPort);
    for (Resource bidirectionalPort : bpSet) {
      // Get the BidirectionalPort resource.
      log.debug("[processServiceModifications] found BidirectionalPort: " + bidirectionalPort.getURI());
      Statement hasService = bidirectionalPort.getProperty(Nml.hasService);
      Resource hasServiceRef = hasService.getResource();
      log.debug("[processServiceModifications] removing hasService reference: " + hasServiceRef.getURI());

      bandwidthServiceMap.remove(hasServiceRef.getURI());
    }

    log.debug("[getModifiedBandwidthServices] exiting...");
    return bandwidthServiceMap;
  }
}
