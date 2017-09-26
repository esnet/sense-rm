package net.es.sense.rm.driver.nsi.dds;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author hacksaw
 */
public class InvalidVersionException extends WebApplicationException {
    private static final long serialVersionUID = 1L;
    private final XMLGregorianCalendar version;
    private final XMLGregorianCalendar actual;

    public InvalidVersionException(Response response, final XMLGregorianCalendar version, final XMLGregorianCalendar actual) {
        super(response);
        this.version = version;
        this.actual = actual;
    }

    public XMLGregorianCalendar getVersion() {
        return version;
    }

    /**
     * @return the actual
     */
    public XMLGregorianCalendar getActual() {
        return actual;
    }
}
