package net.es.sense.rm.driver.nsi.cs.api;

import java.util.HashMap;
import java.util.Map;
import javax.xml.ws.Endpoint;
import net.es.sense.rm.driver.nsi.cs.db.OperationMap;
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
  private OperationMap operationMap;

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
