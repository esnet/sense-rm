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
package net.es.sense.rm.driver.nsi.cs.api;

import jakarta.xml.ws.Endpoint;
import net.es.nsi.cs.lib.ClientUtil;
import net.es.sense.rm.driver.nsi.RaController;
import net.es.sense.rm.driver.nsi.cs.db.OperationMapRepository;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.apache.cxf.Bus;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConnectionServiceConfig {
  final private Bus bus;
  final private NsiProperties nsiProperties;
  final private ReservationService reservationService;
  final private OperationMapRepository operationMap;
  final private RaController raController;

  public ConnectionServiceConfig(Bus bus, NsiProperties nsiProperties, ReservationService reservationService,
                          OperationMapRepository operationMap, RaController raController) {
    this.bus = bus;
    this.nsiProperties = nsiProperties;
    this.reservationService = reservationService;
    this.operationMap = operationMap;
    this.raController = raController;
  }

  @Bean
  public Endpoint endpoint() {
    EndpointImpl endpoint = new EndpointImpl(bus, new ConnectionService(nsiProperties, reservationService, operationMap, raController));
    endpoint.setProperties(ClientUtil.setProps(endpoint.getProperties()));
    LoggingFeature lf = new LoggingFeature();
    lf.setPrettyLogging(true);
    endpoint.getFeatures().add(lf);

    endpoint.publish("/nsi-v2");

    return endpoint;
  }
}
