package net.es.sense.rm.driver.nsi.dds.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBElement;
import net.es.nsi.dds.lib.jaxb.dds.ErrorType;
import net.es.nsi.dds.lib.jaxb.dds.ObjectFactory;
import net.es.sense.rm.driver.nsi.dds.InvalidVersionException;

/**
 *
 * @author hacksaw
 */
public class Exceptions {
    private static final ObjectFactory factory = new ObjectFactory();

    public static WebApplicationException internalServerErrorException(String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(DiscoveryError.INTERNAL_SERVER_ERROR, resource, parameter);
        Response ex = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException unauthorizedException(String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(DiscoveryError.UNAUTHORIZED, resource, parameter);
        Response ex = Response.status(Response.Status.UNAUTHORIZED).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException missingParameterException(String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(DiscoveryError.MISSING_PARAMETER, resource, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException invalidXmlException(String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(DiscoveryError.MISSING_PARAMETER, resource, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException illegalArgumentException(DiscoveryError errorEnum, String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(errorEnum, resource, parameter);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException doesNotExistException(DiscoveryError errorEnum, String resource, String id) {
        ErrorType error = DiscoveryError.getErrorType(errorEnum, id, resource);
        Response ex = Response.status(Response.Status.NOT_FOUND).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static WebApplicationException resourceExistsException(DiscoveryError errorEnum, String resource, String parameter) {
        ErrorType error = DiscoveryError.getErrorType(errorEnum, resource, parameter);
        Response ex = Response.status(Response.Status.CONFLICT).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new WebApplicationException(ex);
    }

    public static InvalidVersionException invalidVersionException(DiscoveryError errorEnum, String resource, long request, long actual) {
        ErrorType error = DiscoveryError.getErrorType(errorEnum, resource, "request=" + request + ", actual=" + actual);
        Response ex = Response.status(Response.Status.BAD_REQUEST).entity(new GenericEntity<JAXBElement<ErrorType>>(factory.createError(error)){}).build();
        return new InvalidVersionException(ex, request, actual);
    }
}
