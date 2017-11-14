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
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.xml.datatype.DatatypeConfigurationException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.Decoder;
import net.es.nsi.common.util.UrlHelper;
import net.es.nsi.common.util.XmlUtilities;
import net.es.sense.rm.api.common.Encoder;
import net.es.sense.rm.api.common.Error;
import net.es.sense.rm.api.common.HttpConstants;
import net.es.sense.rm.api.common.Resource;
import net.es.sense.rm.api.common.ResourceAnnotation;
import net.es.sense.rm.api.common.UrlTransform;
import net.es.sense.rm.api.config.SenseProperties;
import net.es.sense.rm.driver.api.Driver;
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
 * The SENSE RM API web services.
 *
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/sense/v1")
@Api(tags = "SENSE RM API")
@ResourceAnnotation(name = "sense", version = "v1")
public class SenseRmController extends SenseController {

  @Autowired
  ApplicationContext context;

  @Autowired(required = true)
  SenseProperties config;

  private UrlTransform utilities;
  private Driver driver;

  @PostConstruct
  public void init() throws Exception {
    utilities = new UrlTransform(config.getProxy());
    Class<?> forName = Class.forName(config.getDriver());
    driver = context.getBean(forName.asSubclass(Driver.class));
  }

  /**
   * *********************************************************************
   * GET /api/sense/v1 *********************************************************************
   */
  /**
   * Returns a list of available SENSE service API resources.
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
  public List<Resource> getResources() throws MalformedURLException {
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
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

    return resources;
  }

  /**
   * Returns a list of available SENSE topology models.
   *
   * @param uriInfo The incoming URI context used to invoke the service.
   * @param accept Provides media types that are acceptable for the response. At the moment 'application/json' is the
   * supported response encoding.
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting all models with
   * creationTime after the specified date. The date must be specified in RFC 1123 format.
   * @param current If current=true then a collection of models containing only the most recent model will be returned.
   * Default value is current=false.
   * @param encode
   * @param summary If summary=true then a summary collection of models will be returned including the model meta-data
   * while excluding the model element. Default value is summary=true.
   * @param model This version’s detailed topology model in the requested format (TURTLE, etc.). To optimize transfer
   * the contents of this model element should be gzipped (contentType="application/x-gzip") and base64 encoded
   * (contentTransferEncoding= "base64"). This will reduce the transfer size and encapsulate the original model
   * contents.
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
                  defaultValue = HttpConstants.IF_MODIFIED_SINCE_DEFAULT)
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

    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] GET operation = {}, accept = {}, If-Modified-Since = {}, current = {}, summary = {}, model = {}",
            location, accept, ifModifiedSince, current, summary, model);

    Date lastModified = DateUtils.parseDate(ifModifiedSince);
    if (lastModified == null) {
      log.error("[SenseRmController] invalid header {}, value = {}.", HttpConstants.IF_MODIFIED_SINCE_NAME,
              ifModifiedSince);
      lastModified = DateUtils.parseDate(HttpConstants.IF_MODIFIED_SINCE_DEFAULT);
    }

    // First case is to handle a targeted request for the current model.
    long newest = 0;
    final HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Location", location.toASCIIString());

    try {
      List<ModelResource> models = new ArrayList<>();
      Collection<ModelResource> result = driver.getModels(current, model).get();
      if (current) {
        Optional<ModelResource> first = result.stream().reduce((e1, e2) -> {
          throw new IllegalArgumentException("Multiple models returned for parameter current = " + current);
        });

        // No results means resource not found.
        if (!first.isPresent()) {
          return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ModelResource m = first.get();
        log.info("[SenseRmController] model id = {}, creationTime = {}, If-Modified-Since = {}", m.getId(),
                m.getCreationTime(), DateUtils.formatDate(lastModified));

        long creationTime = XmlUtilities.xmlGregorianCalendar(m.getCreationTime()).toGregorianCalendar().getTimeInMillis();

        if (creationTime <= lastModified.getTime()) {
          log.info("[SenseRmController] resource not modified {}", m.getId());
          headers.setLastModified(creationTime);
          return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
        }

        log.info("[SenseRmController] returning matching resource {}", m.getId());

        m.setHref(UrlHelper.append(location.toASCIIString(), m.getId()));
        if (!summary) {
          if (encode) {
            m.setModel(Encoder.encode(m.getModel()));
          }
        }
        models.add(m);
        newest = creationTime;
      } else {
        for (ModelResource m : result) {
          long creationTime = XmlUtilities.xmlGregorianCalendar(m.getCreationTime()).toGregorianCalendar().getTimeInMillis();

          log.info("[SenseRmController] model id = {}, creationTime = {}, If-Modified-Since = {}", m.getId(),
                m.getCreationTime(), DateUtils.formatDate(lastModified));

          if (creationTime > newest) {
            newest = creationTime;
          }

          if (creationTime > lastModified.getTime()) {
            log.info("[SenseRmController] returning matching resource {}", m.getId());
            m.setHref(UrlHelper.append(location.toASCIIString(), m.getId()));
            if (!summary) {
              if (encode) {
                m.setModel(Encoder.encode(m.getModel()));
              }
            }
            models.add(m);
          }
        }
      }

      headers.setLastModified(newest);

      for (ModelResource m : models) {
        log.info("[SenseRmController] returning id = {}, creationTime = {}, new LastModfied = {}, queries If-Modified-Since = {}.", m.getId(), m.getCreationTime(), DateUtils.formatDate(new Date(newest)), DateUtils.formatDate(lastModified));
      }

      return new ResponseEntity<>(models, headers, HttpStatus.OK);
    } catch (InterruptedException | ExecutionException | IOException | IllegalArgumentException | DatatypeConfigurationException ex) {
      log.error("[SenseRmController] Exception caught", ex.getMessage());
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   ***********************************************************************
   * GET /api/sense/v1/models/{id} **********************************************************************
   */
  /**
   * Returns the model identified by id within the Resource Manager.
   *
   * @param uriInfo The incoming URI context used to invoke the service.
   * @param accept Provides media types that are acceptable for the response. At the moment 'application/json' is the
   * supported response encoding.
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting all models with
   * creationTime after the specified date. The date must be specified in RFC 1123 format.
   * @param encode
   * @param model This version’s detailed topology model in the requested format (TURTLE, etc.). To optimize transfer
   * the contents of this model element should be gzipped (contentType="application/x-gzip") and base64 encoded
   * (contentTransferEncoding="base64"). This will reduce the transfer size and encapsulate the original model contents.
   * @param id Identifier of the target topology model resource.
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
          @ApiParam(value = HttpConstants.ID_MSG, required = true) String id) {

    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] operation = {}, id = {}, accept = {}, ifModifiedSince = {}, model = {}",
            location, id, accept, ifModifiedSince, model);

    Date lastModified = DateUtils.parseDate(ifModifiedSince);
    if (lastModified == null) {
      log.error("[SenseRmController] invalid header {}, value = {}.", HttpConstants.IF_MODIFIED_SINCE_NAME,
              ifModifiedSince);
      lastModified = DateUtils.parseDate(HttpConstants.IF_MODIFIED_SINCE_DEFAULT);
    }

    ModelResource m;
    try {
      m = driver.getModel(model, id).get();

      if (m == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Location", location.toASCIIString());

      log.info("[SenseRmController] model id = {}, getCreationTime = {}, If-Modified-Since = {}", m.getId(),
              m.getCreationTime(), DateUtils.formatDate(lastModified));

      long creationTime = XmlUtilities.xmlGregorianCalendar(m.getCreationTime()).toGregorianCalendar().getTimeInMillis();
      if (creationTime <= lastModified.getTime()) {
        log.info("[SenseRmController] returning not modified {}", m.getId());
        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
      }

      m.setHref(location.toASCIIString());
      if (encode) {
        m.setModel(Encoder.encode(m.getModel()));
      }

      headers.setLastModified(creationTime);

      log.info("[SenseRmController] returning id = {}, creationTime = {}, queried If-Modified-Since = {}.", m.getId(), m.getCreationTime(), DateUtils.formatDate(lastModified));

      return new ResponseEntity<>(m, headers, HttpStatus.OK);

    } catch (InterruptedException | IOException | ExecutionException | DatatypeConfigurationException ex) {
      log.error("getModel failed, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();

      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (NotFoundException ex) {
      log.error("getModel failed due unknown modelId = {}, ex = {}", id, ex);

      Error error = Error.builder()
              .error(HttpStatus.NOT_FOUND.getReasonPhrase())
              .error_description("modelId " + id + " not found")
              .build();

      return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
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
  public List<DeltaResource> getModelDeltas(
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
          @PathVariable(HttpConstants.ID_NAME)
          @ApiParam(value = HttpConstants.ID_MSG, required = true) String id) {

    DeltaResource delta = new DeltaResource();
    delta.setId("922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setHref("http://localhost:8080/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setLastModified("2016-07-26T13:45:21.001Z");
    delta.setModelId("922f4388-c8f6-4014-b6ce-5482289b02ff");
    delta.setResult("H4sIACKIo1cAA+1Y32/aMBB+56+I6NtUx0loVZEV1K6bpkm0m5Y+7NWNTbCa2JEdCP3vZydULYUEh0CnQXlC+O6+O/ u7X1ylgozp3BJ4LH3rcpJlqQ9hnud23rO5iKDnOA50XKgEgAwnJEEnQ8vuXC30eB5XqXnQuYDqfEl+LnGVvAv/3I6CVQiFv FbF7ff7UKF4Hiice2IZmgMml5RZ8sq/0n9p82hcWFCHCtjtQacHH5AkS5qJkNWa6rDUdD2Y8ZTHPHoqtDudy6lgvpLzGclyL h59zBNE2YBIW/0y7FhvPqjw8X5h5LS40TuUEPyDYTqjeIpi6/OKltZ5IDFnkbzn1gqmxMxO0DyiEUp5qoGfj4YVxiZIfqGYCh JmlDMU/+IiW7W7FIvPOCYDOWUMhOLcT5XG4AJ60PVjyh4HKPbk8NTIBmIxSIRXmpgT4EAXOqWVT4YmwgkNX9w4V wZeu1EddEDEjIZkE4YyktNMgbCoyhhTj2Z1vwVK3PoZ3Fz/DqyvN3ddTYoq49o3bd6mLCNCffFsgqeHwZH1sS04gxmQua 3fbI1I8YIkm5xDr3xCInXmVPPAAEqztAaqtwQFtHQ7vBzJiQmeeoAW5KxwxJisP57VrOuRF2xB1ZZ37M9ixIBALCLrSG9ct 0dI8fy74NN02CQ5Yq2WPaVkE5KqaQ5UIRRRnWinq+51huIpkVbXA2dO/6ztjTZhUUXRWEnYHVUPcwY2zaOafHh553fKz dcErXCLyuuYIlnpEBYo4quVVvtzezO6K2Fd0AMG/arMWuoBLQTcWnqZ9tcNOahhX679/8muNX27IrrgWWBRbZvESFjIsV LdXYhHOYcVVAlylKb6LrtjFEvStnY2Gi4ONAn2Mxh9dJr3mYi2bDjmRWHHXaYe7N+wZk0b2FTH2rHC+EJ28NL7Se9aVp Ry9ZwwyNTDa8Uf627LdXcfM8CWk/4BTQBblaMDjb92ND2C+Gun+yNsz6ZbcdPuLBSQfLvwJ1QggDPmF6Uo5IyVt+rnCq es+AaNt7cbsh/jaxtn//6GsWYDgAEdvHddkj8Wv/3/+bCDnW+rf2DW7Xx/ASQO0KQcHgAA");
    List<DeltaResource> deltas = new ArrayList<>();
    deltas.add(delta);
    return deltas;
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
   * @param uriInfo The incoming URI context used to invoke the service.
   * @param accept Provides media types that are acceptable for the response. At the moment 'application/json' is the
   * supported response encoding.
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting all models with
   * creationTime after the specified date. The date must be specified in RFC 1123 format.
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

    Date ifnms = DateUtils.parseDate(ifModifiedSince);
    if (ifnms == null) {
      log.error("[SenseRmController] invalid header {}, value = {}.", HttpConstants.IF_MODIFIED_SINCE_NAME,
              ifModifiedSince);
      ifnms = DateUtils.parseDate(HttpConstants.IF_MODIFIED_SINCE_DEFAULT);
    }

    try {
      DeltaResource d = driver.getDelta(deltaId, ifnms.getTime(), model).get();

      // We have an exception to report NOT_FOUND so null is NOT_MODIFIED.
      if (d == null) {
        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
      }

      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Location", location.toASCIIString());

      log.info("[SenseRmController] deltaId = {}, lastModified = {}, If-Modified-Since = {}", d.getId(),
              d.getLastModified(), DateUtils.formatDate(ifnms));

      long lastModified = XmlUtilities.xmlGregorianCalendar(d.getLastModified()).toGregorianCalendar().getTimeInMillis();

      d.setHref(location.toASCIIString());
      if (encode) {
        d.setAddition(Encoder.encode(d.getAddition()));
        d.setReduction(Encoder.encode(d.getReduction()));
        d.setResult(Encoder.encode(d.getResult()));
      }

      headers.setLastModified(lastModified);

      log.info("[SenseRmController] getDelta returning id = {}, creationTime = {}, queried If-Modified-Since = {}.",
              d.getId(), d.getLastModified(), DateUtils.formatDate(ifnms));

      return new ResponseEntity<>(d, headers, HttpStatus.OK);

    } catch (NotFoundException ex) {
      log.error("getDelta could not find delta, deltaId = {}, ex = {}", deltaId, ex);
      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Location", location.toASCIIString());
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
    } catch (Exception ex) {
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
   * ***********************************************************************
   * GET /api/sense/v1/deltas ***********************************************************************
   */
  /**
   * Returns a list of accepted delta resources.
   *
   * @param uriInfo The incoming URI context used to invoke the service.
   * @param accept Provides media types that are acceptable for the response. At the moment 'application/json' is the
   * supported response encoding.
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting all models with
   * creationTime after the specified date. The date must be specified in RFC 1123 format.
   * @param summary If summary=true then a summary collection of delta resources will be returned including the delta
 meta-data while excluding the addition, reduction, and m elements. Default value is summary=true.
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
  public List<DeltaResource> getDeltas(
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
          @ApiParam(value = HttpConstants.MODEL_MSG, required = false) String model) {

    DeltaResource delta = new DeltaResource();
    delta.setId("922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setHref("http://localhost:8080/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setLastModified("2016-07-26T13:45:21.001Z");
    delta.setModelId("922f4388-c8f6-4014-b6ce-5482289b02ff");
    delta.setResult("H4sIACKIo1cAA+1Y32/aMBB+56+I6NtUx0loVZEV1K6bpkm0m5Y+7NWNTbCa2JEdCP3vZydULYUEh0CnQXlC+O6+O/ u7X1ylgozp3BJ4LH3rcpJlqQ9hnud23rO5iKDnOA50XKgEgAwnJEEnQ8vuXC30eB5XqXnQuYDqfEl+LnGVvAv/3I6CVQiFv FbF7ff7UKF4Hiice2IZmgMml5RZ8sq/0n9p82hcWFCHCtjtQacHH5AkS5qJkNWa6rDUdD2Y8ZTHPHoqtDudy6lgvpLzGclyL h59zBNE2YBIW/0y7FhvPqjw8X5h5LS40TuUEPyDYTqjeIpi6/OKltZ5IDFnkbzn1gqmxMxO0DyiEUp5qoGfj4YVxiZIfqGYCh JmlDMU/+IiW7W7FIvPOCYDOWUMhOLcT5XG4AJ60PVjyh4HKPbk8NTIBmIxSIRXmpgT4EAXOqWVT4YmwgkNX9w4V wZeu1EddEDEjIZkE4YyktNMgbCoyhhTj2Z1vwVK3PoZ3Fz/DqyvN3ddTYoq49o3bd6mLCNCffFsgqeHwZH1sS04gxmQua 3fbI1I8YIkm5xDr3xCInXmVPPAAEqztAaqtwQFtHQ7vBzJiQmeeoAW5KxwxJisP57VrOuRF2xB1ZZ37M9ixIBALCLrSG9ct 0dI8fy74NN02CQ5Yq2WPaVkE5KqaQ5UIRRRnWinq+51huIpkVbXA2dO/6ztjTZhUUXRWEnYHVUPcwY2zaOafHh553fKz dcErXCLyuuYIlnpEBYo4quVVvtzezO6K2Fd0AMG/arMWuoBLQTcWnqZ9tcNOahhX679/8muNX27IrrgWWBRbZvESFjIsV LdXYhHOYcVVAlylKb6LrtjFEvStnY2Gi4ONAn2Mxh9dJr3mYi2bDjmRWHHXaYe7N+wZk0b2FTH2rHC+EJ28NL7Se9aVp Ry9ZwwyNTDa8Uf627LdXcfM8CWk/4BTQBblaMDjb92ND2C+Gun+yNsz6ZbcdPuLBSQfLvwJ1QggDPmF6Uo5IyVt+rnCq es+AaNt7cbsh/jaxtn//6GsWYDgAEdvHddkj8Wv/3/+bCDnW+rf2DW7Xx/ASQO0KQcHgAA");
    List<DeltaResource> deltas = new ArrayList<>();
    deltas.add(delta);
    return deltas;
  }

  /**
   * ***********************************************************************
   * GET /api/sense/v1/deltas/{deltaId} ***********************************************************************
   */
  /**
   * Returns the delta resource identified by deltaId that is associated with model identified by id within the Resource
   * Manager.
   *
   * @param uriInfo The incoming URI context used to invoke the service.
   * @param accept Provides media types that are acceptable for the response. At the moment 'application/json' is the
   * supported response encoding.
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting all models with
   * creationTime after the specified date. The date must be specified in RFC 1123 format.
   * @param encode
   * @param model This version’s detailed topology model in the requested format (TURTLE, etc.). To optimize transfer
   * the contents of this model element should be gzipped (contentType="application/x-gzip") and base64 encoded
   * (contentTransferEncoding="base64"). This will reduce the transfer size and encapsulate the original model contents.
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
          @ApiParam(value = HttpConstants.ENCODE_MSG, required = false) boolean encode,
          @PathVariable(HttpConstants.DELTAID_NAME)
          @ApiParam(value = HttpConstants.DELTAID_MSG, required = true) String deltaId
  ) {

    // Get the requested resource URL.
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseRmController] operation = {}, id = {}, accept = {}, ifModifiedSince = {}, model = {}",
            location, deltaId, accept, ifModifiedSince, model);

    Date ifnms = DateUtils.parseDate(ifModifiedSince);
    if (ifnms == null) {
      log.error("[SenseRmController] invalid header {}, value = {}.", HttpConstants.IF_MODIFIED_SINCE_NAME,
              ifModifiedSince);
      ifnms = DateUtils.parseDate(HttpConstants.IF_MODIFIED_SINCE_DEFAULT);
    }

    DeltaResource d;
    try {
      d = driver.getDelta(deltaId, ifnms.getTime(), model).get();

      // We have an exception to report NOT_FOUND so null is NOT_MODIFIED.
      if (d == null) {
        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
      }

      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Location", location.toASCIIString());

      log.info("[SenseRmController] deltaId = {}, lastModified = {}, If-Modified-Since = {}", d.getId(),
              d.getLastModified(), DateUtils.formatDate(ifnms));

      long lastModified = XmlUtilities.xmlGregorianCalendar(d.getLastModified()).toGregorianCalendar().getTimeInMillis();

      d.setHref(location.toASCIIString());
      if (summary) {
        d.setAddition(null);
        d.setReduction(null);
        d.setResult(null);
      } else if (encode) {
        d.setAddition(Encoder.encode(d.getAddition()));
        d.setReduction(Encoder.encode(d.getReduction()));
        d.setResult(Encoder.encode(d.getResult()));
      }

      headers.setLastModified(lastModified);

      log.info("[SenseRmController] getDelta returning id = {}, creationTime = {}, queried If-Modified-Since = {}.",
              d.getId(), d.getLastModified(), DateUtils.formatDate(ifnms));

      //List<DeltaResource> result = new ArrayList<>();
      //result.add(d);
      //return new ResponseEntity<>(result, headers, HttpStatus.OK);
      return new ResponseEntity<>(d, headers, HttpStatus.OK);

    } catch (NotFoundException ex) {
      log.error("getDelta could not find delta, deltaId = {}, ex = {}", deltaId, ex);
      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Location", location.toASCIIString());
      return new ResponseEntity<>(headers, HttpStatus.NOT_FOUND);
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
   * POST /api/sense/v1/deltas
   *
   * @param accept
   * @param deltaRequest
   * @param encode
   * @param model
   * @return
   * @throws java.net.URISyntaxException
   */
  @RequestMapping(
          value = "/deltas",
          method = RequestMethod.POST,
          consumes = {MediaType.APPLICATION_JSON_VALUE},
          produces = {MediaType.APPLICATION_JSON_VALUE})
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

    log.info("[SenseRmController] operation = {}, accept = {}, model = {}, deltaRequest = {}",
            location, accept, model, deltaRequest);

    try {
      if (encode) {
        if (!Strings.isNullOrEmpty(deltaRequest.getAddition())) {
          deltaRequest.setAddition(Decoder.decode(deltaRequest.getAddition()));
        }

        if (!Strings.isNullOrEmpty(deltaRequest.getReduction())) {
          deltaRequest.setReduction(Decoder.decode(deltaRequest.getReduction()));
        }
      }

      DeltaResource delta = driver.propagateDelta(deltaRequest, model).get();

      if (delta == null) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }

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

      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Location", contentLocation);
      headers.setLastModified(lastModified);

      log.info("[SenseRmController] Delta returning id = {}, creationTime = {}",
              delta.getId(), delta.getLastModified());

      return new ResponseEntity<>(delta, headers, HttpStatus.CREATED);

    } catch (NotFoundException nfe) {
      Error error = Error.builder()
              .error(HttpStatus.NOT_FOUND.getReasonPhrase())
              .error_description(nfe.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    catch (InternalServerErrorException | InterruptedException | ExecutionException | IOException | DatatypeConfigurationException ex) {
      log.error("pullModel failed", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();

      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

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
      d = driver.commitDelta(deltaId).get();

      // We have an exception to report NOT_FOUND so null is NOT_MODIFIED.
      if (d == null) {
        return new ResponseEntity<>(HttpStatus.CONFLICT);
      }

      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Location", location.toASCIIString());

      log.info("[SenseRmController] commitDelta deltaId = {}, lastModified = {}", d.getId(), d.getLastModified());
      return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);

    } catch (NotFoundException ex) {
      log.error("getDelta could not find delta, deltaId = {}, ex = {}", deltaId, ex);
      final HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Location", location.toASCIIString());
      return new ResponseEntity<>(headers, HttpStatus.NOT_FOUND);
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      log.error("getDelta failed, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();

      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
