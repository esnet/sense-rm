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

import java.util.HashMap;
import java.util.Map;
import javax.xml.ws.Endpoint;
import net.es.sense.rm.driver.nsi.cs.db.OperationMapRepository;
import net.es.sense.rm.driver.nsi.cs.db.ReservationService;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConnectionServiceConfig {

  @Autowired
  private Bus bus;

  @Autowired
  private ReservationService reservationService;

  @Autowired
  private OperationMapRepository operationMap;

  @Bean
  public Endpoint endpoint() {
    EndpointImpl endpoint = new EndpointImpl(bus, new ConnectionService(reservationService, operationMap));

    Map<String, Object> props = endpoint.getProperties();
    if (props == null) {
      props = new HashMap<>();
    }
    props.put("jaxb.additionalContextClasses",
            new Class[]{
              org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory.class,
              org.ogf.schemas.nsi._2013._12.services.types.ObjectFactory.class
            });
    endpoint.setProperties(props);
    endpoint.publish("/nsi-v2");

    return endpoint;
  }
}
