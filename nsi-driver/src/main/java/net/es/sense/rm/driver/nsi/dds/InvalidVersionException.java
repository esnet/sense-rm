package net.es.sense.rm.driver.nsi.dds;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 *
 * @author hacksaw
 */
public class InvalidVersionException extends WebApplicationException {
    private static final long serialVersionUID = 1L;
    private final long version;
    private final long actual;

    public InvalidVersionException(Response response, final long version, final long actual) {
        super(response);
        this.version = version;
        this.actual = actual;
    }

    public long getVersion() {
        return version;
    }

    /**
     * @return the actual
     */
    public long getActual() {
        return actual;
    }
}
