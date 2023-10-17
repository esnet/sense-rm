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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.api.common.HttpConstants;
import net.es.sense.rm.api.common.Resource;
import net.es.sense.rm.api.common.ResourceAnnotation;
import net.es.sense.rm.api.common.UrlTransform;
import net.es.sense.rm.api.config.SenseProperties;
import net.es.sense.rm.measurements.MeasurementController;
import net.es.sense.rm.measurements.MeasurementResults;
import net.es.sense.rm.model.ModelResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class implements the administration controller interface for the SENSE-RM
 * that provides REST endpoints to monitor the running process.
 *
 * @author hacksaw
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/admin/v1")
@Tag(name = "SENSE Admin API", description = "SENSE-RM administration management API")
@ResourceAnnotation(name = "admin", version = "v1")
public class SenseAdminController {
  private final SenseProperties config;
  private final MeasurementController measurementController;
  private UrlTransform utilities;

  /**
   * Constructor to inject bean parameters.
   *
   * @param config The SENSE configuration bean.
   * @param measurementController Reference to the measurement controller for statistics.
   */
  public SenseAdminController(SenseProperties config, MeasurementController measurementController) {
    this.config = config;
    this.measurementController = measurementController;
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
  @Operation(
          summary = "Get a list of supported SENSE Administration resources.",
          description = "Returns a list of available SENSE Administration resource URL.",
          tags = { "getResources", "get" },
          method = "GET")
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = HttpConstants.OK_CODE,
                  description = HttpConstants.OK_MSG,
                  content = @Content(array = @ArraySchema(schema = @Schema(implementation = Resource.class)),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(
                  responseCode = HttpConstants.UNAUTHORIZED_CODE,
                  description = HttpConstants.UNAUTHORIZED_MSG,
                  content = @Content(schema = @Schema(implementation = net.es.sense.rm.api.common.Error.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(
                  responseCode = HttpConstants.FORBIDDEN_CODE,
                  description = HttpConstants.FORBIDDEN_MSG,
                  content = @Content(schema = @Schema(implementation = net.es.sense.rm.api.common.Error.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(
                  responseCode = HttpConstants.INTERNAL_ERROR_CODE,
                  description = HttpConstants.INTERNAL_ERROR_MSG,
                  content = @Content(schema = @Schema(implementation = net.es.sense.rm.api.common.Error.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)),
  })
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
   * Returns build version information for the SENSE-NSI-RM.
   *
   * @return The build information in a JSON structure, or an Error if information cannot be found.
   */
  @Operation(
          summary = "Get SENSE-RM version information.",
          description = "Returns a map of git related version information.",
          tags = { "getVersion", "get" },
          method = "GET")
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = HttpConstants.OK_CODE,
                  description = HttpConstants.OK_MSG,
                  content = @Content(schema = @Schema(implementation = String.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(
                  responseCode = HttpConstants.UNAUTHORIZED_CODE,
                  description = HttpConstants.UNAUTHORIZED_MSG,
                  content = @Content(schema = @Schema(implementation = net.es.sense.rm.api.common.Error.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)),
          @ApiResponse(
                  responseCode = HttpConstants.INTERNAL_ERROR_CODE,
                  description = HttpConstants.INTERNAL_ERROR_MSG,
                  content = @Content(schema = @Schema(implementation = net.es.sense.rm.api.common.Error.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)),
  })
  @RequestMapping(
            value = "/version",
            method = RequestMethod.GET,
            produces = { MediaType.APPLICATION_JSON_VALUE })
  @ResponseBody
  @ResourceAnnotation(name = "version", version = "v1")
  public ResponseEntity<?> getVersion() {
    return readGitProperties();
  }

  /**
   * Read the Git build information from properties file.
   *
   * @return HTTP Response structure containing results.
   */
  private ResponseEntity<?> readGitProperties() {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream("git.properties");
    try {
      return new ResponseEntity<>(readFromInputStream(inputStream), HttpStatus.OK);
    } catch (IOException ex) {
      log.error("[SenseAdminController] Version information could not be retrieved.", ex);
      net.es.sense.rm.api.common.Error error = net.es.sense.rm.api.common.Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Read String contents from InputStream.
   *
   * @param inputStream
   * @return
   * @throws IOException
   */
  private String readFromInputStream(InputStream inputStream) throws IOException {
    StringBuilder resultStringBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = br.readLine()) != null) {
        resultStringBuilder.append(line).append("\n");
      }
    }
    return resultStringBuilder.toString();
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
  @Operation(
          summary = "Get a collection of available operational log resources.",
          description = "Returns a list of available operational log resources.",
          tags = { "getMeasurements", "get" },
          method = "GET")
  @ApiResponses(value = {
          @ApiResponse(
                  responseCode = HttpConstants.OK_CODE,
                  description = HttpConstants.OK_MEASURE_MSG,
                  headers = {
                          @Header(name = HttpConstants.CONTENT_TYPE_NAME,
                                  description = HttpConstants.CONTENT_TYPE_DESC,
                                  schema = @Schema(implementation = String.class)),
                  },
                  content = @Content(array = @ArraySchema(schema = @Schema(implementation = ModelResource.class)),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
                  responseCode = HttpConstants.NOT_MODIFIED,
                  description = HttpConstants.NOT_MODIFIED_MSG,
                  headers = {
                          @Header(name = HttpConstants.CONTENT_TYPE_NAME,
                                  description = HttpConstants.CONTENT_TYPE_DESC,
                                  schema = @Schema(implementation = String.class)),
                          @Header(name = HttpConstants.LAST_MODIFIED_NAME,
                                  description = HttpConstants.LAST_MODIFIED_DESC,
                                  schema = @Schema(implementation = String.class))
                  },
                  content = @Content(schema = @Schema(implementation = net.es.sense.rm.api.common.Error.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
                  responseCode = HttpConstants.BAD_REQUEST_CODE,
                  description = HttpConstants.BAD_REQUEST_MSG,
                  headers = {
                          @Header(name = HttpConstants.CONTENT_TYPE_NAME,
                                  description = HttpConstants.CONTENT_TYPE_DESC,
                                  schema = @Schema(implementation = String.class))
                  },
                  content = @Content(schema = @Schema(implementation = net.es.sense.rm.api.common.Error.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
                  responseCode = HttpConstants.UNAUTHORIZED_CODE,
                  description = HttpConstants.UNAUTHORIZED_MSG,
                  headers = {
                          @Header(name = HttpConstants.CONTENT_TYPE_NAME,
                                  description = HttpConstants.CONTENT_TYPE_DESC,
                                  schema = @Schema(implementation = String.class))
                  },
                  content = @Content(schema = @Schema(implementation = net.es.sense.rm.api.common.Error.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
                  responseCode = HttpConstants.FORBIDDEN_CODE,
                  description = HttpConstants.FORBIDDEN_MSG,
                  headers = {
                          @Header(name = HttpConstants.CONTENT_TYPE_NAME,
                                  description = HttpConstants.CONTENT_TYPE_DESC,
                                  schema = @Schema(implementation = String.class))
                  },
                  content = @Content(schema = @Schema(implementation = net.es.sense.rm.api.common.Error.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)
          ),
          @ApiResponse(
                  responseCode = HttpConstants.INTERNAL_ERROR_CODE,
                  description = HttpConstants.INTERNAL_ERROR_MSG,
                  headers = {
                          @Header(name = HttpConstants.CONTENT_TYPE_NAME,
                                  description = HttpConstants.CONTENT_TYPE_DESC,
                                  schema = @Schema(implementation = String.class))
                  },
                  content = @Content(schema = @Schema(implementation = net.es.sense.rm.api.common.Error.class),
                          mediaType = MediaType.APPLICATION_JSON_VALUE)
          )
  })
  @RequestMapping(
          value = "/measurements",
          method = RequestMethod.GET,
          produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  @ResourceAnnotation(name = "measurements", version = "v1")
  public ResponseEntity<?> getMeasurements(
          @RequestHeader(value = HttpConstants.ACCEPT_NAME, defaultValue = MediaType.APPLICATION_JSON_VALUE)
          @Parameter(description = HttpConstants.ACCEPT_MSG, required = false) String accept,
          @RequestHeader(value = HttpConstants.IF_MODIFIED_SINCE_NAME, required = false)
          @Parameter(description = HttpConstants.IF_MODIFIED_SINCE_MSG, required = false) String ifModifiedSince,
          @RequestHeader(value = HttpConstants.IF_NONE_MATCH_NAME, required = false)
          @Parameter(description = HttpConstants.IF_NONE_MATCH_MSG, required = false) String ifNoneMatch) {

    // We need the request URL to build fully qualified resource URLs.
    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    log.info("[SenseAdminController] GET operation = {}, accept = {}, If-Modified-Since = {}, If-None-Match = {}",
            location, accept, ifModifiedSince, ifNoneMatch);

    // Parse the If-Modified-Since header if it is present.
    long ifms = Common.parseIfModifiedSince(ifModifiedSince);

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

      if (results.isEmpty()) {
        // There are no results.
        return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
      }

      // Add the HTTP headers for our response.
      headers.setETag("\"" + results.get().getUuid() + "\"");
      headers.setLastModified(results.get().getLastModifiedTime());

      // The provided ETAG matches the current ETAG so return NOT MODIFIED.
      if (results.get().getQueue().isEmpty() && filter) {
        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
      }

      // The provided ETAG does not match the most recent so we need to
      // return a list of newer measurements.
      return new ResponseEntity<>(results.get().getQueue(), headers, HttpStatus.OK);

    } catch (IllegalArgumentException ex) {
      log.error("[SenseRmController] getMeasurements failed", ex);
      net.es.sense.rm.api.common.Error error = net.es.sense.rm.api.common.Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
