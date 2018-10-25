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
package net.es.sense.rm.driver.nsi;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.cs.db.ConnectionMapService;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import net.es.sense.rm.driver.nsi.db.Model;
import net.es.sense.rm.driver.nsi.db.ModelService;
import net.es.sense.rm.driver.nsi.dds.api.DocumentReader;
import net.es.sense.rm.driver.nsi.mrml.MrmlFactory;
import net.es.sense.rm.driver.nsi.mrml.NmlModel;
import net.es.sense.rm.driver.nsi.mrml.SwitchingSubnetModel;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.apache.jena.riot.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Service
public class AuditServiceBean implements AuditService {
  @Autowired
  private NsiProperties nsiProperties;

  @Autowired
  private DocumentReader documentReader;

  @Autowired
  private ReservationService reservationService;

  @Autowired
  private ConnectionMapService connectionMapService;

  @Autowired
  private ModelService modelService;

  @Override
  public void audit() {
    log.info("[AuditService] starting audit.");

    // Get the new document context.
    log.debug("[AuditService] generating NML topology model.");
    NmlModel nml = new NmlModel(documentReader);
    nml.setDefaultServiceType(nsiProperties.getDefaultServiceType());
    nml.setDefaultType(nsiProperties.getDefaultType());
    nml.setDefaultUnits(nsiProperties.getDefaultUnits());
    nml.setDefaultGranularity(nsiProperties.getDefaultGranularity());

    String topologyId = nsiProperties.getNetworkId();

    //nml.getTopologyIds().forEach((topologyId) -> {
      log.debug("[AuditService] processing topologyId = {}", topologyId);

      log.debug("[AuditService] generating SwitchingSubnet model.");
      SwitchingSubnetModel ssm = new SwitchingSubnetModel(reservationService, connectionMapService, nml, topologyId);

      log.debug("[AuditService] generating MRML model.");
      MrmlFactory mrml = new MrmlFactory(nml, ssm, topologyId);

    // Check to see if this is a new version.
      long version = mrml.getVersion();
      if (modelService.isPresent(topologyId, version)) {
        log.info("[AuditService] found matching model topologyId = {}, version = {}.", topologyId, version);
      } else {
        log.info("[AuditService] adding new topology version, topologyId = {}, version = {}", topologyId, version);
        String modelAsString = mrml.getModelAsString(Lang.TURTLE);

        UUID uuid = UUID.randomUUID();
        Model model = new Model();
        model.setTopologyId(topologyId);
        model.setModelId(uuid.toString());
        model.setVersion(mrml.getVersion());
        model.setBase(modelAsString);
        modelService.create(model);
      }
    //});

    // Delete older models (keep last 5).
    modelService.purge(topologyId, nsiProperties.getModelPruneSize());

    // Dump the current contents as an audit log.
    log.info("[AuditService] stored models after the audit...");
    modelService.get().forEach((m) -> {
      log.info(m.toString());
    });
  }

  @Override
  public void audit(String topologyId) {
    log.info("[AuditService] starting audit for {}.", topologyId);

    // Get the new document context.
    NmlModel nml = new NmlModel(documentReader);
    nml.setDefaultServiceType(nsiProperties.getDefaultServiceType());
    nml.setDefaultType(nsiProperties.getDefaultType());
    nml.setDefaultUnits(nsiProperties.getDefaultUnits());
    nml.setDefaultGranularity(nsiProperties.getDefaultGranularity());

    SwitchingSubnetModel ssm = new SwitchingSubnetModel(reservationService, connectionMapService, nml, topologyId);
    MrmlFactory mrml = new MrmlFactory(nml, ssm, topologyId);

    // Check to see if this is a new version.
    if (modelService.isPresent(topologyId, mrml.getVersion())) {
      log.debug("[AuditService] found matching model topologyId = {}, version = {}.", topologyId, mrml.getVersion());
    } else {
      log.info("[AuditService] adding new topology version, topologyId = {}, version = {}", topologyId, mrml.getVersion());
      UUID uuid = UUID.randomUUID();
      Model model = new Model();
      model.setTopologyId(topologyId);
      model.setModelId(uuid.toString());
      model.setVersion(mrml.getVersion());
      model.setBase(mrml.getModelAsString(Lang.TURTLE));
      modelService.create(model);
    }
  }
}
