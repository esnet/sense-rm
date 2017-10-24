package net.es.nsi.cs.lib;

/**
 *
 * @author hacksaw
 */
@lombok.Data
public class Configuration {
    // Local web server configuration information.
    private String localWebServerAddress = "localhost";
    private int    localWebServerPort    = 9080;

    // Remote NSA configuration information.
    private String providerEndpoint   = "http://localhost:8084/NSI-2.0/ConnectionServiceProvider";
    private String providerUserId     = "nsidemo";
    private String providerPassword   = "RioPlug-Fest2011!";

    // Requesting user information.
    //private String providerNsa        = "urn:ogf:network:nsa:netherlight";
    private String providerNsa        = "urn:ogf:network:nsa:czechlight";

    private String globalUserName     = "jrv@internet2.edu";
    private String globalRole         = "AuthorizedUser";

    // Reservation information.
    //private String reservationDescription       = "This is a test schedule connecting cph-80 to pra-80";
    //private String reservationSourceStp         = "urn:ogf:network:stp:netherlight.ets:cph-80";
    //private String reservationDestinationStp    = "urn:ogf:network:stp:netherlight.ets:pra-80";

    private String reservationDescription       = "This is a test schedule connecting ps-80 to ams-80";
    private String reservationSourceStp         = "urn:ogf:network:stp:czechlight.ets:ps-80";
    private String reservationDestinationStp    = "urn:ogf:network:stp:czechlight.ets:ams-80";

    private int    reservationBandwidth         = 500;

    // My local NSA identity information.
    private String requesterEndpoint   = "http://localhost:9080/nsi-v1/ConnectionServiceRequester";
    private String requesterNSA        = "urn:ogf:network:nsa:ferb.surfnet.nl";
}
