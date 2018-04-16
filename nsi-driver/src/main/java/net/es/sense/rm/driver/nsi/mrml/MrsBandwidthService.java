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
import net.es.sense.rm.driver.api.mrml.ModelUtil;
import net.es.sense.rm.driver.schema.Mrs;
import net.es.sense.rm.driver.schema.Nml;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class MrsBandwidthService {

  private final Resource service;
  private final MrsUnits unit;
  private final long maximumCapacity;

  public MrsBandwidthService(Resource port, Model model) throws IllegalArgumentException {
    // Get the MrsBandwidthService assocated with this BidirectionalPort.
    Statement bwServiceRef = port.getProperty(Nml.hasService);
    if (bwServiceRef == null) {
      log.error("BidirectionalPort does not contain hasService property {}", port.getURI());
      throw new IllegalArgumentException("BidirectionalPort does not contain hasService property " + port.getURI());
    }

    service = ModelUtil.getResourceOfType(model, bwServiceRef.getResource(), Mrs.BandwidthService);
    if (service == null) {
      log.error("BidirectionalPort does not contain BandwidthService {}", port.getURI());
      throw new IllegalArgumentException("BidirectionalPort does not contain BandwidthService " + port.getURI());
    }

    Statement unitProperty = service.getProperty(Mrs.unit);
    if (unitProperty == null) {
      log.error("BandwidthService does not contain a unit property {}", service.getURI());
      throw new IllegalArgumentException("BandwidthService does not contain a unit property " + service.getURI());
    }

    unit = MrsUnits.valueOf(unitProperty.getString().toLowerCase());

    Statement mcProperty = service.getProperty(Mrs.maximumCapacity);
    if (mcProperty == null) {
      log.error("BandwidthService does not contain a maximumCapacity property {}", service.getURI());
      throw new IllegalArgumentException("BandwidthService does not contain a maximumCapacity property " + service.getURI());
    }
    
    maximumCapacity = mcProperty.getLong();
  }

  public String getId() {
    return service.getURI();
  }

  public MrsUnits getUnit() {
    return unit;
  }

  public long getMaximumCapacity() {
    return maximumCapacity;
  }
}
