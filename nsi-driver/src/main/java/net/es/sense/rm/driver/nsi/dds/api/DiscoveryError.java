package net.es.sense.rm.driver.nsi.dds.api;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.dds.lib.jaxb.DdsParser;
import net.es.nsi.dds.lib.jaxb.dds.ErrorType;
import net.es.nsi.dds.lib.jaxb.dds.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the error values for the PCE logging system.
 *
 * @author hacksaw
 */
public enum DiscoveryError {
    // Message content errors.
    MISSING_PARAMETER(100, "MISSING_PARAMETER", "Missing parameter (%s)."),
    INVALID_PARAMETER(101, "INVALID_PARAMETER", "Invalid parameter (%s)."),
    UNSUPPORTED_PARAMETER(102, "UNSUPPORTED_PARAMETER", "Parameter provided contains an unsupported value which MUST be processed (%s)."),
    NOT_IMPLEMENTED(103, "NOT_IMPLEMENTED", "Parameter is for a feature that has not been implemented (%s)."),
    VERSION_NOT_SUPPORTED(104, "VERSION_NOT_SUPPORTED", "The service version requested is not supported. (%s)."),
    INTERNAL_SERVER_ERROR(105, "INTERNAL_SERVER_ERROR", "There was an internal server processing error (%s)."),
    NOT_FOUND(106, "NOT_FOUND", "Requested resources was not found (%s)."),
    INVALID_XML(107, "INVALID_XML", "Request contained invalid XML (%s)."),
    UNAUTHORIZED(108, "UNAUTHORIZED", "Supplied credentials did not have needed level of authorization (%s)."),

    DOCUMENT_EXISTS(110, "DOCUMENT_EXISTS", "There is already a registered document under provided id (%s)."),
    DOCUMENT_DOES_NOT_EXIST(111, "DOCUMENT_DOES_NOT_EXIST", "The requested document does not exist (%s)."),
    DOCUMENT_INVALID(112, "DOCUMENT_INVALID", "There was a problem with the document that prevents storage (%s)."),
    DOCUMENT_VERSION(113, "DOCUMENT_VERSION", "The document version was older than the current document (%s)."),

    SUBCRIPTION_DOES_NOT_EXIST(120, "SUBCRIPTION_DOES_NOT_EXIST", "Requested subscription identifier does not exist (%s)."),

    // Mark the end.
    END(999, "END", "END");

    private final static Logger log = LoggerFactory.getLogger(DiscoveryError.class);

    private final int code;
    private final String label;
    private final String description;

    /**
     * A mapping between the integer code and its corresponding Status to facilitate lookup by code.
     */
    private static Map<Integer, DiscoveryError> codeToStatusMapping;

    private static final ObjectFactory factory = new ObjectFactory();

    private DiscoveryError(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    private static void initMapping() {
        codeToStatusMapping = new HashMap<>();
        for (DiscoveryError s : values()) {
            codeToStatusMapping.put(s.code, s);
        }
    }

    public static DiscoveryError getStatus(int i) {
        if (codeToStatusMapping == null) {
            initMapping();
        }
        return codeToStatusMapping.get(i);
    }

    public static ErrorType getErrorType(DiscoveryError error, String resource, String info) {
        ErrorType fp = factory.createErrorType();
        fp.setCode(error.getCode());
        fp.setLabel(error.getLabel());
        fp.setResource(resource);
        fp.setDescription(String.format(error.getDescription(), info));
        return fp;
    }

    public static ErrorType getErrorType(String xml) {
        ErrorType error;
        try {
            error = DdsParser.getInstance().xml2Jaxb(ErrorType.class, xml);
        }
        catch (JAXBException ex) {
            error = getErrorType(INTERNAL_SERVER_ERROR, "JAXB", xml);
        }
        return error;
    }

    public static String getErrorString(DiscoveryError error, String resource, String info) {
        try {
            ErrorType fp = getErrorType(error, resource, info);
            JAXBElement<ErrorType> errorElement = factory.createError(fp);
            return DdsParser.getInstance().jaxb2Xml(errorElement);
        } catch (JAXBException ex) {
            log.error("getErrorString: could not generate xml", ex);
            return null;
        }
    }

    public static String getErrorString(ErrorType error) throws JAXBException {
        JAXBElement<ErrorType> errorElement = factory.createError(error);
        String xml = DdsParser.getInstance().jaxb2Xml(errorElement);
        return xml;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("DisocveryError ");
        sb.append("{ code=").append(code);
        sb.append(", label='").append(label).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(" }");
        return sb.toString();
    }
}
