package net.es.sense.rm.driver.nsi.cs.api;

import javax.xml.ws.Endpoint;
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

    /*private final static List<String> schemaLocations = new ArrayList<String>() {
      {
        add("org.ogf.schemas.nsi._2013._12.connection.types");
        add("org.ogf.schemas.nsi._2013._12.services.point2point");
        add("org.ogf.schemas.nsi._2013._12.services.types");
        add("oasis.names.tc.saml._2_0.assertion");
        add("org.ogf.schemas.nsi._2013._12.framework.headers");
        add("org.ogf.schemas.nsi._2013._12.framework.types");
        add("org.w3._2000._09.xmldsig_");
        add("org.w3._2001._04.xmlenc_");
      }
    };*/

    @Bean
    public Endpoint endpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, new ConnectionService(reservationService));
        endpoint.publish("/nsi-v2");
        //endpoint.setSchemaLocations(schemaLocations);
        return endpoint;
    }
}