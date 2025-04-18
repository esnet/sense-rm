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
import net.es.sense.rm.measurements.MeasurementController;
import net.es.sense.rm.measurements.db.MeasurementType;
import net.es.sense.rm.measurements.db.MetricType;
import org.apache.jena.riot.Lang;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Audit the information associated with our MRML model and generate a new model if anything has changed.
 *
 * @author hacksaw
 */
@Slf4j
@Service
public class AuditServiceBean implements AuditService {
  private final NsiProperties nsiProperties;
  private final DocumentReader documentReader;
  private final ReservationService reservationService;
  private final ConnectionMapService connectionMapService;
  private final ModelService modelService;
  private final MeasurementController measurementController;
  private long lastDds = 0;
  private long lastCon = 0;

  /**
   * Constructor for autowire injection of service beans.
   *
   * @param nsiProperties
   * @param documentReader
   * @param reservationService
   * @param connectionMapService
   * @param modelService
   * @param measurementController
   */
  public AuditServiceBean(NsiProperties nsiProperties, DocumentReader documentReader,
                          ReservationService reservationService, ConnectionMapService connectionMapService,
                          ModelService modelService, MeasurementController measurementController) {
    this.nsiProperties = nsiProperties;
    this.documentReader = documentReader;
    this.reservationService = reservationService;
    this.connectionMapService = connectionMapService;
    this.modelService = modelService;
    this.measurementController = measurementController;
  }

  /**
   * Audit all models.
   */
  @Override
  public void audit() {
    // We really only have one model to audit.
    try {
      audit(nsiProperties.getNetworkId());
    } catch (IOException e) {
      // Ignore
      log.error("AuditServiceBean::audit failed", e);
    }
  }

  /**
   * Audit the model associated with the supplied topologyId.
   *
   * @param topologyId
   */
  @Override
  public void audit(String topologyId) throws IOException {
    long start = System.currentTimeMillis();

    log.info("[AuditService] starting audit for {}.", topologyId);
    log.info("[AuditService] lastDds = {}, lastCon = {}", lastDds, lastCon);

    // Here is a little bug hack to get us through SC23.
    if (topologyId == null) {
      topologyId = nsiProperties.getNetworkId();
      log.error("[AuditService] Adding topologyId={} in place of null.", topologyId);
      log.error("[AuditService] here is a trace:\n{}", Arrays.toString(Thread.currentThread().getStackTrace()));
    }

    // How do we determine if there was a change in network data that would
    // result in a new model being generated?  First check to see if we have
    // had a new DDS document or connection arrive since our last audit.
    long dds = documentReader.getLastDiscovered();
    long con = reservationService.getLastDiscovered();

    // If we have then there is a possibility a change in topology has occured.
    if (lastDds == dds && lastCon == con) {
      log.info("[AuditService] No new documents so skipping audit.");
      return;
    }

    log.debug("[AuditService] generating NML topology model, new dds = {}, con = {}", dds, con);

    // Get the new document context.
    NmlModel nml = new NmlModel(documentReader);
    nml.setDefaultServiceType(nsiProperties.getDefaultServiceType());
    nml.setDefaultType(nsiProperties.getDefaultType());
    nml.setDefaultUnits(nsiProperties.getDefaultUnits());
    nml.setDefaultGranularity(nsiProperties.getDefaultGranularity());

    log.debug("[AuditService] processing topologyId = {}", topologyId);

    try {
      // Generate a SwitchingSubnet model based off of the NSI connections and
      // discovered NML model.
      SwitchingSubnetModel ssm = new SwitchingSubnetModel(reservationService, connectionMapService, nml, topologyId);

      // Now generate an MRML model based on the combined NSI and NML information.
      log.debug("[AuditService] generating MRML model.");
      MrmlFactory mrml = new MrmlFactory(nml, ssm, topologyId);

      // Check to see if this is a new version.
      if (modelService.isPresent(topologyId, mrml.getVersion())) {
        log.info("[AuditService] found matching model topologyId = {}, version = {}.", topologyId, mrml.getVersion());
      } else {
        log.info("[AuditService] adding new topology version, topologyId = {}, version = {}", topologyId, mrml.getVersion());
        String modelAsString = mrml.getModelAsString(Lang.TURTLE);

        UUID uuid = UUID.randomUUID();
        Model model = new Model();
        model.setTopologyId(topologyId);
        model.setModelId(uuid.toString());
        model.setVersion(mrml.getVersion());
        model.setBase(modelAsString);

        Model create = modelService.create(model);
        if (create != null) {
          log.debug("[AuditService] created modelId = {} for topology {}",
                  create.getModelId(), create.getTopologyId());
        } else {
          log.error("[AuditService] failed to create modelId = {} for topology {}",
                  model.getModelId(), model.getTopologyId());
        }

        measurementController.add(
                MeasurementType.MODEL_AUDIT,
                uuid.toString(),
                MetricType.DURATION,
                String.valueOf(System.currentTimeMillis() - start));
      }
    } catch (Exception ex) {
      log.error("[AuditService] caught an unexpected exception so aborting model generation", ex);
      throw ex;
    }

    // Save the updated values for the next audit.
    lastDds = dds;
    lastCon = con;
    log.debug("[AuditService] generated NML topology model, new lastDds = {}, lastCon = {}", lastDds, lastCon);
    
    // Delete older models (keep last 5).
    modelService.purge(topologyId, nsiProperties.getModelPruneSize());
  }
}
