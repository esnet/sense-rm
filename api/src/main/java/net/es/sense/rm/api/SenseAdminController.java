/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016 - 2019, The Regents
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

import com.google.common.base.Strings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.api.common.HttpConstants;
import net.es.sense.rm.api.common.Resource;
import net.es.sense.rm.api.common.ResourceAnnotation;
import net.es.sense.rm.api.common.UrlTransform;
import net.es.sense.rm.api.config.SenseProperties;
import net.es.sense.rm.measurements.MeasurementController;
import net.es.sense.rm.measurements.MeasurementResults;
import net.es.sense.rm.model.DeltaResource;
import net.es.sense.rm.model.ModelResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author hacksaw
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/admin/v1")
@Api(tags = "SENSE Admin API")
@ResourceAnnotation(name = "admin", version = "v1")
public class SenseAdminController extends SenseController {

  @Autowired(required = true)
  SenseProperties config;

  @Autowired
  private MeasurementController measurementController;

  private UrlTransform utilities;

  @PostConstruct
  public void init() throws Exception {
    utilities = new UrlTransform(config.getProxy());
  }

  /*************************************************************************
   * GET /api/admin/v1
   *************************************************************************/

  /**
   * Returns a list of available SENSE service API resources.
   *
   * @return A RESTful response.
   * @throws java.net.MalformedURLException
   */
  @ApiOperation(
          value = "Get a list of supported SENSE Administration resources.",
          notes = "Returns a list of available SENSE Administration resource URL.",
          response = DeltaResource.class,
          responseContainer = "List")
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_MSG,
                    response = Resource.class),
            @ApiResponse(
                    code = HttpConstants.UNAUTHORIZED_CODE,
                    message = HttpConstants.UNAUTHORIZED_MSG,
                    response = net.es.sense.rm.api.common.Error.class),
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = net.es.sense.rm.api.common.Error.class),
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = net.es.sense.rm.api.common.Error.class),
          }
      )
  @RequestMapping(method = RequestMethod.GET, produces = { MediaType.APPLICATION_JSON_VALUE })
  @ResponseBody
  public List<Resource> getResources() throws MalformedURLException {
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
    List<Resource> resources = new ArrayList<>();
    Method[] methods = SenseAdminController.class.getMethods();
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
   * Returns a list of available logs relating to operational performance.
   *
   * Operation: GET /api/admin/v1/logs
   *
   * @param accept Provides media types that are acceptable for the response.
   *    At the moment 'application/json' is the supported response encoding.
   *
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since
   *    header requesting all models with creationTime after the specified
   *    date. The date must be specified in RFC 1123 format.
   * @param ifNoneMatch The HTTP request may contain the If-None-Match header
   *    specifying a previously provided resource ETag value.  If the resource
   *    version identified by the provided ETag value has not changed then a
   *    304 NOT_MODIFIED is returned, otherwise a new version of the resource
   *    is returned.
   * @return A RESTful response.
   */
  @ApiOperation(
          value = "Get a collection of available operational log resources.",
          notes = "Returns a list of available operational log resources.",
          response = ModelResource.class,
          responseContainer = "List")
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_LOGS_MSG,
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
                    response = net.es.sense.rm.api.common.Error.class,
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
                    response = net.es.sense.rm.api.common.Error.class,
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
                    response = net.es.sense.rm.api.common.Error.class,
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
                    response = net.es.sense.rm.api.common.Error.class,
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
                    response = net.es.sense.rm.api.common.Error.class,
                    responseHeaders = {
                      @ResponseHeader(
                              name = HttpConstants.CONTENT_TYPE_NAME,
                              description = HttpConstants.CONTENT_TYPE_DESC,
                              response = String.class)
                    }
            ),})
  @RequestMapping(
          value = "/measurements",
          method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  @ResourceAnnotation(name = "measurements", version = "v1")
  public ResponseEntity<?> getMeasurements(
          @RequestHeader(
                  value = HttpConstants.ACCEPT_NAME,
                  defaultValue = MediaType.APPLICATION_JSON_VALUE)
          @ApiParam(value = HttpConstants.ACCEPT_MSG, required = false) String accept,
          @RequestHeader(
                  value = HttpConstants.IF_MODIFIED_SINCE_NAME,
                  required = false)
          @ApiParam(value = HttpConstants.IF_MODIFIED_SINCE_MSG, required = false) String ifModifiedSince,
          @RequestParam(
                  value = HttpConstants.IF_NONE_MATCH_NAME,
                  defaultValue = "false")
          @ApiParam(value = HttpConstants.IF_NONE_MATCH_MSG, required = false) String ifNoneMatch) {

    // We need the request URL to build fully qualified resource URLs.
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseAdminController] GET operation = {}, accept = {}, If-Modified-Since = {}, If-None-Match = {}",
            location, accept, ifModifiedSince, ifNoneMatch);

    // Parse the If-Modified-Since header if it is present.
    long ifms = Common.parseIfModfiedSince(ifModifiedSince);

    // Populate the content location header with our URL location.
    final HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

    try {
      // Track matching metrics here.
      Optional<MeasurementResults> results;

      // Determine the type of query needed.
      boolean filter = true;
      if (!Strings.isNullOrEmpty(ifNoneMatch)) {
        // We have an ETAG from a previous query.
        results = measurementController.get(ifNoneMatch);
      } else if (ifms > 0) {
        // We have the If-Modified-Since header to base our search.
        results = measurementController.get(ifms);
      } else {
        results = measurementController.get();
        filter = false;
      }

      if (!results.isPresent()) {
        // There are no results.
        return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
      }

      // Add the HTTP headers for our response.
      headers.setETag(results.get().getUuid());
      headers.setLastModified(results.get().getLastModifiedTime());

      // The provided ETAG matches the current ETAG so return NOT MODIFIED.
      if (results.get().getQueue().isEmpty() && filter) {
        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
      }

      // The provided ETAG does not match the most recent so we need to
      // return a list of newer measurements.
      return new ResponseEntity<>(results.get().getQueue(), headers, HttpStatus.OK);

    } catch (IllegalArgumentException ex) {
      log.error("[SenseRmController] getMeasurements failed, ex = {}", ex);
      net.es.sense.rm.api.common.Error error = net.es.sense.rm.api.common.Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
