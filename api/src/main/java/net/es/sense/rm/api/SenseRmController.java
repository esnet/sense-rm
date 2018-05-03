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
package net.es.sense.rm.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response.Status;
import javax.xml.datatype.DatatypeConfigurationException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.Decoder;
import net.es.nsi.common.util.UrlHelper;
import net.es.nsi.common.util.UuidHelper;
import net.es.nsi.common.util.XmlUtilities;
import net.es.sense.rm.api.common.Encoder;
import net.es.sense.rm.api.common.Error;
import net.es.sense.rm.api.common.HttpConstants;
import net.es.sense.rm.api.common.Resource;
import net.es.sense.rm.api.common.ResourceAnnotation;
import net.es.sense.rm.api.common.UrlTransform;
import net.es.sense.rm.api.config.SenseProperties;
import net.es.sense.rm.driver.api.DeltaResponse;
import net.es.sense.rm.driver.api.DeltasResponse;
import net.es.sense.rm.driver.api.Driver;
import net.es.sense.rm.driver.api.ModelResponse;
import net.es.sense.rm.driver.api.ModelsResponse;
import net.es.sense.rm.driver.api.ResourceResponse;
import net.es.sense.rm.model.DeltaRequest;
import net.es.sense.rm.model.DeltaResource;
import net.es.sense.rm.model.ModelResource;
import org.apache.http.client.utils.DateUtils;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The SENSE RM API web services module based on Spring REST annotations.
 * This class handles the SENSE RM API specific parameters, encodings, and
 * behaviors but delegates all message processing to technology specific
 * drivers.
 *
 * Supported resource operations:
 *  GET /api/sense/v1
 *
 * @author hacksaw
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/sense/v1")
@Api(tags = "SENSE RM API")
@ResourceAnnotation(name = "sense", version = "v1")
public class SenseRmController extends SenseController {

  // Spring application context.
  @Autowired
  ApplicationContext context;

  // SENSE YAML configuration.
  @Autowired(required = true)
  SenseProperties config;

  // Transformer to manipulate URL path in case there are mapping issues.
  private UrlTransform utilities;

  // The solution specific technology driver implementing SENSE protocol
  // mapping to underlying network technology.
  private Driver driver;

  /**
   * Initialize API by loading technology specific driver using reflection.
   *
   * @throws Exception
   */
  @PostConstruct
  public void init() throws Exception {
    utilities = new UrlTransform(config.getProxy());
    Class<?> forName = Class.forName(config.getDriver());
    driver = context.getBean(forName.asSubclass(Driver.class));
  }

  private ResponseEntity<?> toResponseEntity(HttpHeaders headers, ResourceResponse rr) {
    if (rr == null) {
      return new ResponseEntity<>(headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    switch (rr.getStatus()) {
      case NOT_MODIFIED:
        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);

        default:
          Error.ErrorBuilder eb = Error.builder().error(rr.getStatus().getReasonPhrase());
          rr.getError().ifPresent(e -> eb.error_description(e));
          return new ResponseEntity<>(eb.build(), HttpStatus.valueOf(rr.getStatus().getStatusCode()));
    }
  }

  private long parseIfModfiedSince(String ifModifiedSince) {
    long ifms = 0;
    if (!Strings.isNullOrEmpty(ifModifiedSince)) {
      Date lastModified = DateUtils.parseDate(ifModifiedSince);
      if (lastModified != null) {
        ifms = lastModified.getTime();
      }
    }

    return ifms;
  }

  /**
   * Returns a list of available SENSE service API resource URLs.
   *
   * Operation: GET /api/sense/v1
   *
   * @return A RESTful response.
   * @throws java.net.MalformedURLException
   */
  @ApiOperation(
          value = "Get a list of supported SENSE resources.",
          notes = "Returns a list of available SENSE resource URL.",
          response = DeltaResource.class,
          responseContainer = "List")
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_MSG,
                    response = Resource.class)
            ,
            @ApiResponse(
                    code = HttpConstants.UNAUTHORIZED_CODE,
                    message = HttpConstants.UNAUTHORIZED_MSG,
                    response = Error.class)
            ,
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class)
            ,
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class),})
  @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  public ResponseEntity<?> getResources() throws MalformedURLException {
    try {
      // We need the request URL to build fully qualified resource URLs.
      final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

      log.info("[SenseRmController] GET operation = {}", location);

      // We will populate some HTTP response headers.
      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Location", location.toASCIIString());

      List<Resource> resources = new ArrayList<>();
      Method[] methods = SenseRmController.class.getMethods();
      for (Method m : methods) {
        if (m.isAnnotationPresent(ResourceAnnotation.class)) {
          ResourceAnnotation ra = m.getAnnotation(ResourceAnnotation.class);
          RequestMapping rm = m.getAnnotation(RequestMapping.class);
          if (ra == null || rm == null) {
            continue;
          }
          Resource resource = new Resource();
          resource.setId(ra.name());
          resource.setVersion(ra.version());
          UriComponentsBuilder path = utilities.getPath(location.toASCIIString());
          path.path(rm.value()[0]);
          resource.setHref(path.build().toUriString());
          resources.add(resource);
        }
      }

      return new ResponseEntity<>(resources, headers, HttpStatus.OK);
    } catch (SecurityException | MalformedURLException ex) {
      log.error("[SenseRmController] Exception caught", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns a list of available SENSE topology models.
   *
   * Operation: GET /api/sense/v1/models
   *
   * @param accept Provides media types that are acceptable for the response.
   *    At the moment 'application/json' is the supported response encoding.
   *
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
   *    header requesting all models with creationTime after the specified
   *    date. The date must be specified in RFC 1123 format.
   *
   * @param current If current=true then a collection of models containing only
   *    the most recent model will be returned. Default value is current=false.
   *
   * @param encode Transfer size of the model element contents can be optimized
   *    by gzip/base64 encoding the contained model.  If encode=true the
   *    returned model will be gzipped (contentType="application/x-gzip") and
   *    base64 encoded (contentTransferEncoding= "base64") to reduce transfer
   *    size. Default value is encode=false.
   *
   * @param summary If summary=true then a summary collection of models will be
   *    returned including the model meta-data while excluding the model
   *    element. Default value is summary=true.
   *
   * @param model Specify the model schema format (TURTLE, JSON-LD, etc.).
   *
   * @return A RESTful response.
   */
  @ApiOperation(
          value = "Get a collection of available model resources.",
          notes = "Returns a list of available SENSE topology model resources.",
          response = ModelResource.class,
          responseContainer = "List")
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_TOPOLOGIES_MSG,
                    response = ModelResource.class,
                    responseContainer = "List",
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_MODIFIED,
                    message = HttpConstants.NOT_MODIFIED_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.BAD_REQUEST_CODE,
                    message = HttpConstants.BAD_REQUEST_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.UNAUTHORIZED_CODE,
                    message = HttpConstants.UNAUTHORIZED_MSG,
                    response = Error.class,
                     responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),})
  @RequestMapping(
          value = "/models",
          method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  @ResourceAnnotation(name = "models", version = "v1")
  public ResponseEntity<?> getModels(
          @RequestHeader(
                  value = HttpConstants.ACCEPT_NAME,
                  defaultValue = MediaType.APPLICATION_JSON_VALUE)
          @ApiParam(value = HttpConstants.ACCEPT_MSG, required = false) String accept,
          @RequestHeader(
                  value = HttpConstants.IF_MODIFIED_SINCE_NAME,
                  required = false)
          @ApiParam(value = HttpConstants.IF_MODIFIED_SINCE_MSG, required = false) String ifModifiedSince,
          @RequestParam(
                  value = HttpConstants.CURRENT_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.CURRENT_MSG, required = false) boolean current,
          @RequestParam(
                  value = HttpConstants.SUMMARY_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.SUMMARY_MSG, required = false) boolean summary,
          @RequestParam(
                  value = HttpConstants.ENCODE_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.ENCODE_MSG, required = false) boolean encode,
          @RequestParam(
                  value = HttpConstants.MODEL_NAME,
                  defaultValue = HttpConstants.MODEL_TURTLE)
          @ApiParam(value = HttpConstants.MODEL_MSG, required = false) String model) {

    // We need the request URL to build fully qualified resource URLs.
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] GET operation = {}, accept = {}, If-Modified-Since = {}, current = {}, "
            + "summary = {}, model = {}", location, accept, ifModifiedSince, current, summary, model);

    // Parse the If-Modified-Since header if it is present.
    long ifms = parseIfModfiedSince(ifModifiedSince);

    // Populate the content location header with our URL location.
    final HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Location", location.toASCIIString());

    try {
      // Track matching models here.
      List<ModelResource> models = new ArrayList<>();

      // Keep track of the most recently updated model date.
      long newest = 0;

      // Query the driver for a list of models.
      Collection<ModelResource> result = new ArrayList<>();

      // First case is to handle a targeted request for the current model.
      if (current) {
        ModelResponse response = driver.getCurrentModel(model, ifms).get();
        if (response == null || response.getStatus() != Status.OK) {
          return toResponseEntity(headers, response);
        }

        response.getModel().ifPresent(m -> result.add(m));
      } else {
        ModelsResponse response = driver.getModels(model, ifms).get();
        if (response == null || response.getStatus() != Status.OK) {
          return toResponseEntity(headers, response);
        }

        result.addAll(response.getModels());
      }

      // The requester asked for a list of models so apply any filtering criteria.
      for (ModelResource m : result) {
        long creationTime = XmlUtilities.xmlGregorianCalendar(m.getCreationTime())
                .toGregorianCalendar().getTimeInMillis();

        log.info("[SenseRmController] returning model id = {}, creationTime = {}, If-Modified-Since = {}\n{}",
                m.getId(), m.getCreationTime(), ifModifiedSince, Encoder.encode(m.getModel()));

        // Create the unique resource URL.
        m.setHref(UrlHelper.append(location.toASCIIString(), m.getId()));

        // If summary results are requested we do not return the model.
        if (summary) {
          m.setModel(null);
        } else {
          // If they requested an encoded transfer we will encode the model contents.
          if (encode) {
            m.setModel(Encoder.encode(m.getModel()));
          }
        }

        // Save this model and update If-Modified-Since with the creation time.
        models.add(m);

        if (creationTime > newest) {
            newest = creationTime;
        }
      }

      // Update the LastModified header with the value of the newest model.
      headers.setLastModified(newest);

      // We have success so return the models we have found.
      return new ResponseEntity<>(models, headers, HttpStatus.OK);
    } catch (InterruptedException | IOException | DatatypeConfigurationException | IllegalArgumentException |
            ExecutionException ex) {
      log.error("[SenseRmController] getModels failed, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns the SENSE topology model identified by id.
   *
   * Operation: GET /api/sense/v1/models/{id}
   *
   * @param accept Provides media types that are acceptable for the response. At the moment
   *    'application/json' is the supported response encoding.
   *
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting
   *    all models with creationTime after the specified date. The date must be specified in
   *    RFC 1123 format.
   *
   * @param encode Transfer size of the model element contents can be optimized by gzip/base64
   *    encoding the contained model.  If encode=true then returned model will be gzipped
   *    (contentType="application/x-gzip") and base64 encoded (contentTransferEncoding= "base64")
   *    to reduce transfer size. Default value is encode=false.
   *
   * @param model This version’s detailed topology model in the requested format (TURTLE, etc.).
   *    To optimize transfer the contents of this model element should be gzipped
   *    (contentType="application/x-gzip") and base64 encoded (contentTransferEncoding="base64").
   *    This will reduce the transfer size and encapsulate the original model contents.
   *
   * @param id Identifier of the target topology model resource.
   *
   * @return A RESTful response.
   */
  @ApiOperation(
          value = "Get a specific SENSE topology model resource.",
          notes = "Returns SENSE topology model resource corresponding to the specified resource id.",
          response = ModelResource.class)
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_TOPOLOGIES_MSG,
                    response = ModelResource.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_MODIFIED,
                    message = HttpConstants.NOT_MODIFIED_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.BAD_REQUEST_CODE,
                    message = HttpConstants.BAD_REQUEST_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_FOUND_CODE,
                    message = HttpConstants.NOT_FOUND_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_ACCEPTABLE_CODE,
                    message = HttpConstants.NOT_ACCEPTABLE_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),})
  @RequestMapping(
          value = "/models/{" + HttpConstants.ID_NAME + "}",
          method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  public ResponseEntity<?> getModel(
          @RequestHeader(
                  value = HttpConstants.ACCEPT_NAME,
                  defaultValue = MediaType.APPLICATION_JSON_VALUE)
          @ApiParam(value = HttpConstants.ACCEPT_MSG, required = false) String accept,
          /*
            @RequestHeader(
                  value = HttpConstants.IF_MODIFIED_SINCE_NAME,
                  defaultValue = HttpConstants.IF_MODIFIED_SINCE_DEFAULT)
            @ApiParam(value = HttpConstants.IF_MODIFIED_SINCE_MSG, required = false) String ifModifiedSince,
          */
          @RequestHeader(
                  value = HttpConstants.IF_MODIFIED_SINCE_NAME,
                  required = false)
          @ApiParam(value = HttpConstants.IF_MODIFIED_SINCE_MSG, required = false) String ifModifiedSince,
          @RequestParam(
                  value = HttpConstants.MODEL_NAME,
                  defaultValue = HttpConstants.MODEL_TURTLE)
          @ApiParam(value = HttpConstants.MODEL_MSG, required = false) String model,
          @RequestParam(
                  value = HttpConstants.ENCODE_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.ENCODE_MSG, required = false) boolean encode,
          @PathVariable(HttpConstants.ID_NAME)
          @ApiParam(value = HttpConstants.ID_MSG, required = true) String id) {

    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] operation = {}, id = {}, accept = {}, ifModifiedSince = {}, model = {}",
            location, id, accept, ifModifiedSince, model);

    // Parse the If-Modified-Since header if it is present.
    long ifms = parseIfModfiedSince(ifModifiedSince);

    // Return the local in HTTP header.
    final HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Location", location.toASCIIString());

    try {
      // Retrieve the model if newer than specified If-Modified-Since header.
      ModelResponse response = driver.getModel(id, model, ifms).get();

      if (response == null || response.getStatus() != Status.OK) {
        return toResponseEntity(headers, response);
      }

      ModelResource m = response.getModel().get();

      // Get the creation time for HTTP header.
      long creationTime = XmlUtilities.xmlGregorianCalendar(m.getCreationTime())
              .toGregorianCalendar().getTimeInMillis();
      headers.setLastModified(creationTime);

      log.info("[SenseRmController] returning id = {}, creationTime = {}, queried If-Modified-Since = {}\n{}",
              m.getId(), m.getCreationTime(), ifModifiedSince, Encoder.encode(m.getModel()));

      // Update the HREF to point to the absolute URL for the resource.
      m.setHref(location.toASCIIString());
      if (encode) {
        m.setModel(Encoder.encode(m.getModel()));
      }

      return new ResponseEntity<>(m, headers, HttpStatus.OK);
    } catch (InterruptedException | IOException | DatatypeConfigurationException | ExecutionException ex) {
      log.error("[SenseRmController] getModel failed, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * ***********************************************************************
   * GET /api/sense/v1/models/{id}/deltas ***********************************************************************
   */
  /**
   * Returns a list of accepted delta resources associated with the specified SENSE topology model.
   *
   * @param accept Provides media types that are acceptable for the response. At the moment 'application/json' is the
   * supported response encoding.
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting all models with
   * creationTime after the specified date. The date must be specified in RFC 1123 format.
   * @param summary If summary=true then a summary collection of delta resources will be returned including the delta
 meta-data while excluding the addition, reduction, and m elements. Default value is summary=true.
 *
   * @param encode Transfer size of the model element contents can be optimized by gzip/base64
   *    encoding the contained model.  If encode=true then returned model will be gzipped
   *    (contentType="application/x-gzip") and base64 encoded (contentTransferEncoding= "base64")
   *    to reduce transfer size. Default value is encode=false.
   *
   * @param model If model=turtle then the returned addition, reduction, and m elements will contain the full
 topology model in a TURTLE representation. Default value is model=turtle.
   * @param id The UUID uniquely identifying the topology model resource.
   * @return A RESTful response.
   */
  @ApiOperation(
          value = "Get a collection of delta resources associated with the model resource "
          + "identified by id.",
          notes = "Returns a collection of delta resources associated with a model resource.",
          response = DeltaResource.class,
          responseContainer = "List")
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_DELTAS_MSG,
                    response = DeltaResource.class,
                    responseContainer = "List",
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_MODIFIED,
                    message = HttpConstants.NOT_MODIFIED_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.BAD_REQUEST_CODE,
                    message = HttpConstants.BAD_REQUEST_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_FOUND_CODE,
                    message = HttpConstants.NOT_FOUND_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_ACCEPTABLE_CODE,
                    message = HttpConstants.NOT_ACCEPTABLE_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),})
  @RequestMapping(
          value = "/models/{" + HttpConstants.ID_NAME + "}/deltas",
          method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_JSON_VALUE}
  )
  @ResponseBody
  public ResponseEntity<?> getModelDeltas(
          @RequestHeader(
                  value = HttpConstants.ACCEPT_NAME,
                  defaultValue = MediaType.APPLICATION_JSON_VALUE)
          @ApiParam(value = HttpConstants.ACCEPT_MSG, required = false) String accept,
          @RequestHeader(
                  value = HttpConstants.IF_MODIFIED_SINCE_NAME,
                  required = false)
          @ApiParam(value = HttpConstants.IF_MODIFIED_SINCE_MSG, required = false) String ifModifiedSince,
          @RequestParam(
                  value = HttpConstants.SUMMARY_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.SUMMARY_MSG, required = false) boolean summary,
          @RequestParam(
                  value = HttpConstants.ENCODE_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.ENCODE_MSG, required = false) boolean encode,
          @RequestParam(
                  value = HttpConstants.MODEL_NAME,
                  defaultValue = HttpConstants.MODEL_TURTLE)
          @ApiParam(value = HttpConstants.MODEL_MSG, required = false) String model,
          @PathVariable(HttpConstants.ID_NAME)
          @ApiParam(value = HttpConstants.ID_MSG, required = true) String id) {

    // We need the request URL to build fully qualified resource URLs.
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] GET operation = {}, accept = {}, If-Modified-Since = {}, current = {}, "
            + "summary = {}, model = {}, modelId = {}", location, accept, ifModifiedSince, summary, model, id);

    // Parse the If-Modified-Since header if it is present.
    long ifms = parseIfModfiedSince(ifModifiedSince);

    // Populate the content location header with our URL location.
    final HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Location", location.toASCIIString());

    try {
      // Track matching deltas here.
      List<DeltaResource> deltas = new ArrayList<>();

      // Keep track of the most recently updated delta date.
      long newest = 0;

      // Query the driver for a list of deltas.
      DeltasResponse response = driver.getDeltas(model, ifms).get();
      if (response == null || response.getStatus() != Status.OK) {
        return toResponseEntity(headers, response);
      }

      // The requester asked for a list of models so apply any filtering criteria.
      for (DeltaResource d : response.getDeltas()) {
        long lastModified = XmlUtilities.xmlGregorianCalendar(d.getLastModified())
                .toGregorianCalendar().getTimeInMillis();

        log.info("[SenseRmController] delta id = {}, lastModified = {}, If-Modified-Since = {}",
                d.getId(), d.getLastModified(), ifModifiedSince);

        // Create the unique resource URL.
        d.setHref(UrlHelper.append(location.toASCIIString(), d.getId()));

        // If summary results are requested we do not return the model.
        if (summary) {
          d.setAddition(null);
          d.setReduction(null);
          d.setResult(null);
        } else {
          // If they requested an encoded transfer we will encode the model contents.
          if (encode) {
            d.setAddition(Encoder.encode(d.getAddition()));
            d.setReduction(Encoder.encode(d.getReduction()));
            d.setResult(Encoder.encode(d.getResult()));
          }
        }

        // Save this model and update If-Modified-Since with the creation time.
        deltas.add(d);

        if (lastModified > newest) {
            newest = lastModified;
        }
      }

      // Update the LastModified header with the value of the newest model.
      headers.setLastModified(newest);

      // We have success so return the models we have found.
      return new ResponseEntity<>(deltas, headers, HttpStatus.OK);
    } catch (InterruptedException | IOException | DatatypeConfigurationException | IllegalArgumentException |
            ExecutionException ex) {
      log.error("[SenseRmController] getDeltas failed, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * ***********************************************************************
   * GET /api/sense/v1/models/{id}/deltas/{deltaId}
   * ***********************************************************************
   */
  /**
   * Returns the delta resource identified by deltaId that is associated with model identified by id within the Resource
   * Manager.
   *
   * @param accept Provides media types that are acceptable for the response. At the moment 'application/json' is the
   * supported response encoding.
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting all models with
   * creationTime after the specified date. The date must be specified in RFC 1123 format.
   * @param encode
   * @param model This version’s detailed topology model in the requested format (TURTLE, etc.). To optimize transfer
   * the contents of this model element should be gzipped (contentType="application/x-gzip") and base64 encoded
   * (contentTransferEncoding="base64"). This will reduce the transfer size and encapsulate the original model contents.
   * @param id Identifier of the target topology model resource.
   * @param deltaId Identifier of the target delta resource.
   * @return A RESTful response.
   */
  @ApiOperation(
          value = "Get a specific SENSE topology model resource.",
          notes = "Returns SENSE topology model resource corresponding to the specified resource id.",
          response = DeltaResource.class)
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_DELTA_MSG,
                    response = DeltaResource.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_MODIFIED,
                    message = HttpConstants.NOT_MODIFIED_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.BAD_REQUEST_CODE,
                    message = HttpConstants.BAD_REQUEST_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_FOUND_CODE,
                    message = HttpConstants.NOT_FOUND_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_ACCEPTABLE_CODE,
                    message = HttpConstants.NOT_ACCEPTABLE_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),}
  )
  @RequestMapping(
          value = "/models/{" + HttpConstants.ID_NAME + "}/deltas/{"
          + HttpConstants.DELTAID_NAME + "}",
          method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  public ResponseEntity<?> getModelDelta(
          @RequestHeader(
                  value = HttpConstants.ACCEPT_NAME,
                  defaultValue = MediaType.APPLICATION_JSON_VALUE)
          @ApiParam(value = HttpConstants.ACCEPT_MSG, required = false) String accept,
          @RequestHeader(
                  value = HttpConstants.IF_MODIFIED_SINCE_NAME,
                  defaultValue = HttpConstants.IF_MODIFIED_SINCE_DEFAULT)
          @ApiParam(value = HttpConstants.IF_MODIFIED_SINCE_MSG, required = false) String ifModifiedSince,
          @RequestParam(
                  value = HttpConstants.MODEL_NAME,
                  defaultValue = HttpConstants.MODEL_TURTLE)
          @ApiParam(value = HttpConstants.MODEL_MSG, required = false) String model,
          @RequestParam(
                  value = HttpConstants.ENCODE_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.ENCODE_MSG, required = false) boolean encode,
          @PathVariable(HttpConstants.ID_NAME)
          @ApiParam(value = HttpConstants.ID_MSG, required = true) String id,
          @PathVariable(HttpConstants.DELTAID_NAME)
          @ApiParam(value = HttpConstants.DELTAID_MSG, required = true) String deltaId) {


    // Get the requested resource URL.
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] operation = {}, id = {}, deltaId = {}, accept = {}, ifModifiedSince = {}, model = {}",
            location, id, deltaId, accept, ifModifiedSince, model);

    // Parse the If-Modified-Since header if it is present.
    long ifms = parseIfModfiedSince(ifModifiedSince);

    final HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Location", location.toASCIIString());

    try {
      DeltaResponse response = driver.getDelta(deltaId, model, ifms).get();
      if (response == null || response.getStatus() != Status.OK) {
        return toResponseEntity(headers, response);
      }

      DeltaResource d = response.getDelta().get();

      log.info("[SenseRmController] deltaId = {}, lastModified = {}, If-Modified-Since = {}", d.getId(),
              d.getLastModified(), ifModifiedSince);

      long lastModified = XmlUtilities.xmlGregorianCalendar(d.getLastModified()).toGregorianCalendar().getTimeInMillis();

      d.setHref(location.toASCIIString());
      if (encode) {
        d.setAddition(Encoder.encode(d.getAddition()));
        d.setReduction(Encoder.encode(d.getReduction()));
        d.setResult(Encoder.encode(d.getResult()));
      }

      headers.setLastModified(lastModified);

      log.info("[SenseRmController] getDelta returning id = {}, creationTime = {}, queried If-Modified-Since = {}.",
              d.getId(), d.getLastModified(), ifModifiedSince);

      return new ResponseEntity<>(d, headers, HttpStatus.OK);
    } catch (InterruptedException | ExecutionException | IOException | DatatypeConfigurationException ex) {
      log.error("getDelta failed, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * ***********************************************************************
   * POST /api/sense/v1/models/{id}/deltas
   *
   * *********************************************************************** @param accept
   * @param accept
   * @param deltaRequest
   * @param model
   * @param id
   * @return
   */
  @ApiOperation(
          value = "Submits a proposed model delta to the Resource Manager based on the model identified by id.",
          notes = "The Resource Manager must verify the proposed model change, confirming (201 Created), rejecting (500 Internal Server Error), or proposing an optional counter-offer (200 OK).",
          response = DeltaResource.class)
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_DELTA_COUNTER_MSG,
                    response = DeltaRequest.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.CREATED_CODE,
                    message = HttpConstants.CREATED_MSG,
                    response = DeltaResource.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.BAD_REQUEST_CODE,
                    message = HttpConstants.BAD_REQUEST_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_FOUND_CODE,
                    message = HttpConstants.NOT_FOUND_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_ACCEPTABLE_CODE,
                    message = HttpConstants.NOT_ACCEPTABLE_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.CONFLICT_CODE,
                    message = HttpConstants.CONFLICT_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),}
  )
  @RequestMapping(
          value = "/models/{" + HttpConstants.ID_NAME + "}/deltas",
          method = RequestMethod.POST,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<?> propagateModelDelta(
          @RequestHeader(
                  value = HttpConstants.ACCEPT_NAME,
                  defaultValue = MediaType.APPLICATION_JSON_VALUE)
          @ApiParam(value = HttpConstants.ACCEPT_MSG, required = false) String accept,
          @RequestParam(
                  value = HttpConstants.MODEL_NAME,
                  defaultValue = HttpConstants.MODEL_TURTLE)
          @ApiParam(value = HttpConstants.MODEL_MSG, required = false) String model,
          @PathVariable(HttpConstants.ID_NAME)
          @ApiParam(value = HttpConstants.ID_MSG, required = true) String id,
          @RequestBody
          @ApiParam(value = "A JSON structure-containing the model reduction and/or addition "
                  + " elements. If provided, the model reduction element is applied first, "
                  + " followed by the model addition element.", required = true) DeltaRequest deltaRequest
  ) {

    log.info("propagateModelDelta: {}", deltaRequest);

    DeltaResource delta = new DeltaResource();
    delta.setId("922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setHref("http://localhost:8080/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setLastModified("2016-07-26T13:45:21.001Z");
    delta.setModelId("922f4388-c8f6-4014-b6ce-5482289b02ff");
    delta.setResult("H4sIACKIo1cAA+1Y32/aMBB+56+I6NtUx0loVZEV1K6bpkm0m5Y+7NWNTbCa2JEdCP3vZydULYUEh0CnQXlC+O6+O/ u7X1ylgozp3BJ4LH3rcpJlqQ9hnud23rO5iKDnOA50XKgEgAwnJEEnQ8vuXC30eB5XqXnQuYDqfEl+LnGVvAv/3I6CVQiFv FbF7ff7UKF4Hiice2IZmgMml5RZ8sq/0n9p82hcWFCHCtjtQacHH5AkS5qJkNWa6rDUdD2Y8ZTHPHoqtDudy6lgvpLzGclyL h59zBNE2YBIW/0y7FhvPqjw8X5h5LS40TuUEPyDYTqjeIpi6/OKltZ5IDFnkbzn1gqmxMxO0DyiEUp5qoGfj4YVxiZIfqGYCh JmlDMU/+IiW7W7FIvPOCYDOWUMhOLcT5XG4AJ60PVjyh4HKPbk8NTIBmIxSIRXmpgT4EAXOqWVT4YmwgkNX9w4V wZeu1EddEDEjIZkE4YyktNMgbCoyhhTj2Z1vwVK3PoZ3Fz/DqyvN3ddTYoq49o3bd6mLCNCffFsgqeHwZH1sS04gxmQua 3fbI1I8YIkm5xDr3xCInXmVPPAAEqztAaqtwQFtHQ7vBzJiQmeeoAW5KxwxJisP57VrOuRF2xB1ZZ37M9ixIBALCLrSG9ct 0dI8fy74NN02CQ5Yq2WPaVkE5KqaQ5UIRRRnWinq+51huIpkVbXA2dO/6ztjTZhUUXRWEnYHVUPcwY2zaOafHh553fKz dcErXCLyuuYIlnpEBYo4quVVvtzezO6K2Fd0AMG/arMWuoBLQTcWnqZ9tcNOahhX679/8muNX27IrrgWWBRbZvESFjIsV LdXYhHOYcVVAlylKb6LrtjFEvStnY2Gi4ONAn2Mxh9dJr3mYi2bDjmRWHHXaYe7N+wZk0b2FTH2rHC+EJ28NL7Se9aVp Ry9ZwwyNTDa8Uf627LdXcfM8CWk/4BTQBblaMDjb92ND2C+Gun+yNsz6ZbcdPuLBSQfLvwJ1QggDPmF6Uo5IyVt+rnCq es+AaNt7cbsh/jaxtn//6GsWYDgAEdvHddkj8Wv/3/+bCDnW+rf2DW7Xx/ASQO0KQcHgAA");
    URI deltaURI;
    try {
      deltaURI = new URI("http://localhost:8080/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200");
    } catch (URISyntaxException ex) {
      Error error = new Error(HttpStatus.INTERNAL_SERVER_ERROR.toString(),
              ex.getLocalizedMessage(), null);
      return new ResponseEntity<>(error, null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.error("URL: " + location.toASCIIString());

    //return Response.created(deltaURI).entity(getProxy().serialize(delta)).build();
    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(deltaURI);
    return new ResponseEntity<>(delta, headers, HttpStatus.CREATED);
  }

  /**
   * Returns a collection of delta resources.
   *
   * Operation: GET /api/sense/v1/deltas
   *
   * @param accept Provides media types that are acceptable for the response. At the moment 'application/json' is the
   * supported response encoding.
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting all models with
   * creationTime after the specified date. The date must be specified in RFC 1123 format.
   * @param summary If summary=true then a summary collection of delta resources will be returned including the delta
 meta-data while excluding the addition, reduction, and m elements. Default value is summary=true.
   * @param encode
   * @param model If model=turtle then the returned addition, reduction, and m elements will contain the full
 topology model in a TURTLE representation. Default value is model=turtle.
   * @return A RESTful response.
   */
  @ApiOperation(
          value = "Get a collection of accepted delta resources.",
          notes = "Returns a collection of available delta resources.",
          response = DeltaResource.class,
          responseContainer = "List")
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_DELTAS_MSG,
                    response = DeltaResource.class,
                    responseContainer = "List",
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_MODIFIED,
                    message = HttpConstants.NOT_MODIFIED_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.BAD_REQUEST_CODE,
                    message = HttpConstants.BAD_REQUEST_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_FOUND_CODE,
                    message = HttpConstants.NOT_FOUND_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.NOT_ACCEPTABLE_CODE,
                    message = HttpConstants.NOT_ACCEPTABLE_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            )
            ,
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),}
  )
  @RequestMapping(
          value = "/deltas",
          method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  @ResourceAnnotation(name = "deltas", version = "v1")
  public ResponseEntity<?> getDeltas(
          @RequestHeader(
                  value = HttpConstants.ACCEPT_NAME,
                  defaultValue = MediaType.APPLICATION_JSON_VALUE)
          @ApiParam(value = HttpConstants.ACCEPT_MSG, required = false) String accept,
          @RequestHeader(
                  value = HttpConstants.IF_MODIFIED_SINCE_NAME,
                  defaultValue = HttpConstants.IF_MODIFIED_SINCE_DEFAULT)
          @ApiParam(value = HttpConstants.IF_MODIFIED_SINCE_MSG, required = false) String ifModifiedSince,
          @RequestParam(
                  value = HttpConstants.SUMMARY_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.SUMMARY_MSG, required = false) boolean summary,
          @RequestParam(
                  value = HttpConstants.MODEL_NAME,
                  defaultValue = HttpConstants.MODEL_TURTLE)
          @ApiParam(value = HttpConstants.MODEL_MSG, required = false) String model,
          @RequestParam(
                  value = HttpConstants.ENCODE_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.ENCODE_MSG, required = false) boolean encode) {

    // We need the request URL to build fully qualified resource URLs.
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] GET operation = {}, accept = {}, If-Modified-Since = {}, current = {}, "
            + "summary = {}, model = {}", location, accept, ifModifiedSince, summary, model);

    // Parse the If-Modified-Since header if it is present.
    long ifms = parseIfModfiedSince(ifModifiedSince);

    // Populate the content location header with our URL location.
    final HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Location", location.toASCIIString());

    try {
      // Track matching deltas here.
      List<DeltaResource> deltas = new ArrayList<>();

      // Keep track of the most recently updated delta date.
      long newest = 0;

      // Query the driver for a list of deltas.
      DeltasResponse response = driver.getDeltas(model, ifms).get();
      if (response == null || response.getStatus() != Status.OK) {
        return toResponseEntity(headers, response);
      }

      // The requester asked for a list of models so apply any filtering criteria.
      for (DeltaResource d : response.getDeltas()) {
        long lastModified = XmlUtilities.xmlGregorianCalendar(d.getLastModified())
                .toGregorianCalendar().getTimeInMillis();

        log.info("[SenseRmController] delta id = {}, lastModified = {}, If-Modified-Since = {}",
                d.getId(), d.getLastModified(), ifModifiedSince);

        // Create the unique resource URL.
        d.setHref(UrlHelper.append(location.toASCIIString(), d.getId()));

        // If summary results are requested we do not return the model.
        if (summary) {
          d.setAddition(null);
          d.setReduction(null);
          d.setResult(null);
        } else {
          // If they requested an encoded transfer we will encode the model contents.
          if (encode) {
            d.setAddition(Encoder.encode(d.getAddition()));
            d.setReduction(Encoder.encode(d.getReduction()));
            d.setResult(Encoder.encode(d.getResult()));
          }
        }

        // Save this model and update If-Modified-Since with the creation time.
        deltas.add(d);

        if (lastModified > newest) {
            newest = lastModified;
        }
      }

      // Update the LastModified header with the value of the newest model.
      headers.setLastModified(newest);

      // We have success so return the models we have found.
      return new ResponseEntity<>(deltas, headers, HttpStatus.OK);
    } catch (InterruptedException | IOException | DatatypeConfigurationException | IllegalArgumentException |
            ExecutionException ex) {
      log.error("[SenseRmController] getDeltas failed, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Returns the delta resource identified by deltaId.
   *
   * Operation: GET /api/sense/v1/deltas/{deltaId}
   *
   * @param accept Provides media types that are acceptable for the response. At the moment 'application/json' is the
   * supported response encoding.
   * @param summary
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting all models with
   * creationTime after the specified date. The date must be specified in RFC 1123 format.
   * @param encode
   * @param model Specifies the model encoding to use (i.e. turtle, ttl, json-ld, etc).
   * @param deltaId Identifier of the target delta resource.
   * @return A RESTful response.
   */
  @ApiOperation(
          value = "Get a specific SENSE topology model resource.",
          notes = "Returns SENSE topology model resource corresponding to the specified resource id.",
          response = DeltaResource.class)
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_DELTA_MSG,
                    response = DeltaResource.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.NOT_MODIFIED,
                    message = HttpConstants.NOT_MODIFIED_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.BAD_REQUEST_CODE,
                    message = HttpConstants.BAD_REQUEST_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.NOT_FOUND_CODE,
                    message = HttpConstants.NOT_FOUND_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.NOT_ACCEPTABLE_CODE,
                    message = HttpConstants.NOT_ACCEPTABLE_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
          }
  )
  @RequestMapping(
          value = "/deltas/{" + HttpConstants.DELTAID_NAME + "}",
          method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  public ResponseEntity<?> getDelta(
          @RequestHeader(
                  value = HttpConstants.ACCEPT_NAME,
                  defaultValue = MediaType.APPLICATION_JSON_VALUE)
          @ApiParam(value = HttpConstants.ACCEPT_MSG, required = false) String accept,
          @RequestHeader(
                  value = HttpConstants.IF_MODIFIED_SINCE_NAME,
                  required = false)
          @ApiParam(value = HttpConstants.IF_MODIFIED_SINCE_MSG, required = false) String ifModifiedSince,
          @RequestParam(
                  value = HttpConstants.SUMMARY_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.SUMMARY_MSG, required = false) boolean summary,
          @RequestParam(
                  value = HttpConstants.MODEL_NAME,
                  defaultValue = HttpConstants.MODEL_TURTLE)
          @ApiParam(value = HttpConstants.MODEL_MSG, required = false) String model,
          @RequestParam(
                  value = HttpConstants.ENCODE_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.ENCODE_MSG, required = false) boolean encode,
          @PathVariable(HttpConstants.DELTAID_NAME)
          @ApiParam(value = HttpConstants.DELTAID_MSG, required = true) String deltaId
  ) {

    // Get the requested resource URL.
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] operation = {}, id = {}, accept = {}, ifModifiedSince = {}, model = {}",
            location, deltaId, accept, ifModifiedSince, model);

    // Parse the If-Modified-Since header if it is present.
    long ifms = parseIfModfiedSince(ifModifiedSince);

    // We need to return the current location of this resource in the response header.
    final HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Location", location.toASCIIString());

    try {
      // Query for the requested delta.
      DeltaResponse response = driver.getDelta(deltaId, model, ifms).get();
      if (response == null || response.getStatus() != Status.OK) {
        return toResponseEntity(headers, response);
      }

      DeltaResource d = response.getDelta().get();

      log.info("[SenseRmController] deltaId = {}, lastModified = {}, If-Modified-Since = {}", d.getId(),
              d.getLastModified(), ifModifiedSince);

      // Determine when this was last modified.
      long lastModified = XmlUtilities.xmlGregorianCalendar(d.getLastModified())
              .toGregorianCalendar().getTimeInMillis();
      headers.setLastModified(lastModified);

      // Do we need to return this delta?
      if (lastModified <= ifms) {
        log.info("[SenseRmController] returning not modified, deltaId = {}", d.getId());
        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
      }

      d.setHref(location.toASCIIString());
      if (summary) {
        // If a summary resource view was requested we do not send back any models.
        d.setAddition(null);
        d.setReduction(null);
        d.setResult(null);
      } else if (encode) {
        // Compress and base64 encode the model contents if requested.
        d.setAddition(Encoder.encode(d.getAddition()));
        d.setReduction(Encoder.encode(d.getReduction()));
        d.setResult(Encoder.encode(d.getResult()));
      }

      log.info("[SenseRmController] getDelta returning id = {}, creationTime = {}, queried If-Modified-Since = {}.",
              d.getId(), d.getLastModified(), ifModifiedSince);
      return new ResponseEntity<>(d, headers, HttpStatus.OK);
    } catch (InterruptedException | ExecutionException | IOException | DatatypeConfigurationException ex) {
      log.error("[SenseRmController] getDelta failed, deltaId = {}, ex = {}", deltaId, ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();

      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Submits a proposed model delta to the Resource Manager based on the model
   * identified by modelId within the deltaRequest.
   *
   * Operation: POST /api/sense/v1/deltas
   *
   * @param accept
   * @param deltaRequest
   * @param encode
   * @param model
   * @return
   * @throws java.net.URISyntaxException
   */
  @ApiOperation(
          value = "Submits a proposed model delta to the Resource Manager based on the model "
          + "identified by id.",
          notes = "The Resource Manager must verify the proposed model change, confirming "
          + "(201 Created), rejecting (500 Internal Server Error), or proposing an "
          + "optional counter-offer (200 OK).",
          response = DeltaResource.class)
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_DELTA_COUNTER_MSG,
                    response = DeltaRequest.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.CREATED_CODE,
                    message = HttpConstants.CREATED_MSG,
                    response = DeltaResource.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                      ,
                      @ResponseHeader(
                              name = HttpConstants.LAST_MODIFIED_NAME,
                              description = HttpConstants.LAST_MODIFIED_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.BAD_REQUEST_CODE,
                    message = HttpConstants.BAD_REQUEST_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.NOT_FOUND_CODE,
                    message = HttpConstants.NOT_FOUND_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.NOT_ACCEPTABLE_CODE,
                    message = HttpConstants.NOT_ACCEPTABLE_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.CONFLICT_CODE,
                    message = HttpConstants.CONFLICT_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
          }
  )
  @RequestMapping(
          value = "/deltas",
          method = RequestMethod.POST,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<?> propagateDelta(
          @RequestHeader(
                  value = HttpConstants.ACCEPT_NAME,
                  defaultValue = MediaType.APPLICATION_JSON_VALUE)
          @ApiParam(value = HttpConstants.ACCEPT_MSG, required = false) String accept,
          @RequestParam(
                  value = HttpConstants.MODEL_NAME,
                  defaultValue = HttpConstants.MODEL_TURTLE)
          @ApiParam(value = HttpConstants.MODEL_MSG, required = false) String model,
          @RequestParam(
                  value = HttpConstants.ENCODE_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.ENCODE_MSG, required = false) boolean encode,
          @RequestBody
          @ApiParam(value = "A JSON structure-containing the model reduction and/or addition "
                  + " elements. If provided, the model reduction element is applied first, "
                  + " followed by the model addition element.", required = true) DeltaRequest deltaRequest
  ) throws URISyntaxException {

    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] POST operation = {}, accept = {}, deltaId = {}, modelId = {}, deltaRequest = {}",
            location, accept, deltaRequest.getId(), model, deltaRequest);

    // If the requester did not specify a delta id then we need to create one.
    if (Strings.isNullOrEmpty(deltaRequest.getId())) {
      deltaRequest.setId(UuidHelper.getUUID());
      log.info("[SenseRmController] assigning delta id = {}", deltaRequest.getId());
    }

    try {
      if (encode) {
        if (!Strings.isNullOrEmpty(deltaRequest.getAddition())) {
          deltaRequest.setAddition(Decoder.decode(deltaRequest.getAddition()));
        }

        if (!Strings.isNullOrEmpty(deltaRequest.getReduction())) {
          deltaRequest.setReduction(Decoder.decode(deltaRequest.getReduction()));
        }
      }

      // We need to return the current location of this resource in the response header.
      final HttpHeaders headers = new HttpHeaders();

      // Query for the requested delta.
      DeltaResponse response = driver.propagateDelta(deltaRequest, model).get();
      if (response == null || response.getStatus() != Status.CREATED) {
        return toResponseEntity(headers, response);
      }

      DeltaResource delta = response.getDelta().get();

      String contentLocation = UrlHelper.append(location.toASCIIString(), delta.getId());

      log.info("[SenseRmController] Delta id = {}, lastModified = {}, content-location = {}",
              delta.getId(), delta.getLastModified(), contentLocation);

      long lastModified = XmlUtilities.xmlGregorianCalendar(delta.getLastModified()).toGregorianCalendar().getTimeInMillis();

      delta.setHref(contentLocation);
      if (encode) {
        if (!Strings.isNullOrEmpty(delta.getAddition())) {
          delta.setAddition(Encoder.encode(delta.getAddition()));
        }

        if (!Strings.isNullOrEmpty(delta.getReduction())) {
          delta.setReduction(Encoder.encode(delta.getReduction()));
        }

        if (!Strings.isNullOrEmpty(delta.getResult())) {
          delta.setResult(Encoder.encode(delta.getResult()));
        }
      }

      headers.add("Content-Location", contentLocation);
      headers.setLastModified(lastModified);

      log.info("[SenseRmController] Delta returning id = {}, creationTime = {}",
              delta.getId(), delta.getLastModified());

      return new ResponseEntity<>(delta, headers, HttpStatus.CREATED);
    } catch (InterruptedException | ExecutionException | IOException | DatatypeConfigurationException ex) {
      log.error("pullModel failed", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Transition a delta resource from the Accepted to Committed state.
   *
   * Operation: PUT /api/sense/v1/deltas/{id}/actions/commit
   *
   * @param deltaId The identifier of the delta resource to commit.
   *
   * @return A RESTful response with status NO_CONTENT if successful.
   */
  @ApiOperation(
          value = "Transition a delta resource from the Accepted to Committed state.",
          notes = "The Resource Manager must verify the proposed delta commit and will "
          + "confirm success returning (204 No Content).",
          response = DeltaResource.class)
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.NO_CONTENT_CODE,
                    message = HttpConstants.NO_CONTENT_MSG
            ),
            @ApiResponse(
                    code = HttpConstants.BAD_REQUEST_CODE,
                    message = HttpConstants.BAD_REQUEST_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.NOT_FOUND_CODE,
                    message = HttpConstants.NOT_FOUND_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.NOT_ACCEPTABLE_CODE,
                    message = HttpConstants.NOT_ACCEPTABLE_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),
          }
  )
  @RequestMapping(
          value = "/deltas/{" + HttpConstants.DELTAID_NAME + "}/actions/commit",
          method = RequestMethod.PUT,
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  public ResponseEntity<?> commitDelta(
          @PathVariable(HttpConstants.DELTAID_NAME)
          @ApiParam(value = HttpConstants.DELTAID_MSG, required = true) String deltaId
  ) {

    // Get the requested resource URL.
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] operation = {}, deltaId = {}", location, deltaId);

    DeltaResource d;
    try {
      // We need to return the current location of this resource in the response header.
      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Location", location.toASCIIString());

      // Query for the requested delta.
      DeltaResponse response = driver.commitDelta(deltaId).get();
      if (response == null || response.getStatus() != Status.NO_CONTENT) {
        return toResponseEntity(headers, response);
      }

      DeltaResource delta = response.getDelta().get();
      log.info("[SenseRmController] commitDelta deltaId = {}, lastModified = {}", delta.getId(), delta.getLastModified());

      return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
    } catch (InterruptedException | ExecutionException ex) {
      log.error("getDelta failed, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
