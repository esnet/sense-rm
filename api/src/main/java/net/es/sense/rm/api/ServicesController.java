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
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.api.common.Error;
import net.es.sense.rm.api.common.HttpConstants;
import net.es.sense.rm.api.common.Resource;
import net.es.sense.rm.api.common.ResourceAnnotation;
import net.es.sense.rm.api.common.UrlTransform;
import net.es.sense.rm.api.config.SenseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The SENSE RESTful web services.
 *
 * Used JAX-RS to define a RESTful API and SCR to define this class as an OSGi service.
 */
@SwaggerDefinition(
        basePath = "/api",
        info = @Info(
                description = "This API provides access to programmable SENSE features.",
                version = "v1",
                title = "SENSE Services API",
                termsOfService = "SENSE Resource Manager Copyright (c) 2016, The Regents of the "
                    + "University of California, through Lawrence Berkeley National Laboratory "
                    + "(subject to receipt of any required approvals from the U.S. Dept. of "
                    + "Energy).  All rights reserved.",
                contact = @Contact(
                        name = "SENSE Development Team",
                        email = "sense@es.net",
                        url = "https://github.com/esnet/sense-rm"),
                license = @License(
                        name = "Lawrence Berkeley National Labs BSD variant license",
                        url = "https://spdx.org/licenses/BSD-3-Clause-LBNL.html"))
)

@Slf4j
@RestController
@RequestMapping("/api")
@Api(tags = "SENSE Services API")
public class ServicesController extends SenseControllerErrorHandling {

  @Autowired(required = true)
  SenseProperties config;

  private UrlTransform utilities;

  @PostConstruct
  public void init() throws Exception {
    utilities = new UrlTransform(config.getProxy());
  }

  /**
   * Returns a list of available administrative service API resources.
   *
   * @return A RESTful response.
   * @throws java.net.MalformedURLException
   */
  @ApiOperation(
          value = "Get a list of available SENSE resources.",
          notes = "Returns a list of available SENSE resource URL.",
          response = Resource.class,
          responseContainer = "List"
      )
  @ApiResponses(
          value = {
            @ApiResponse(
                    code = HttpConstants.OK_CODE,
                    message = HttpConstants.OK_MSG,
                    response = Resource.class),
            @ApiResponse(
                    code = HttpConstants.UNAUTHORIZED_CODE,
                    message = HttpConstants.UNAUTHORIZED_MSG,
                    response = Error.class),
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = Error.class),
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = Error.class),
          }
  )
  @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})

  public List<Resource> getResources() throws MalformedURLException {
    final URI location = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUri();
    List<Resource> resources = new ArrayList<>();
    List<Class> classes = new ArrayList<>();
    classes.add(SenseRmController.class);
    classes.add(SenseAdminController.class);

    for (Class<?> c : classes) {
      ResourceAnnotation ra = (ResourceAnnotation) c.getAnnotation(ResourceAnnotation.class);
      RequestMapping rm = (RequestMapping) c.getAnnotation(RequestMapping.class);
      if (ra == null || rm == null) {
        continue;
      }

      Resource resource = new Resource();
      resource.setId(ra.name());
      UriComponentsBuilder path = utilities.getPath(location.toASCIIString());
      path.path(rm.value()[0]);
      resource.setHref(path.build().toUriString());
      resource.setVersion(ra.version());
      resources.add(resource);
    }

    return resources;
  }

  /**
   * Ping - return a 200 OK if requesting entity has been properly authenticated.
   *
   * @return
   */
  @ApiOperation(
          value = "Simple ping query that will return a 200 OK.",
          notes = "Returns nothing.")
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
                    response = Error.class),})
  @PreAuthorize("isAuthenticated()")
  @RequestMapping(value = "/ping", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResourceAnnotation(name = "ping", version = "v1")
  @ResponseBody
  public ResponseEntity<?> ping() {
    try {
      // We need the request URL to build fully qualified resource URLs.
      final URI location;
      try {
        location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
      } catch (Exception ex) {
        log.error("Exception caught in GET of /", ex);
        Error error = Error.builder()
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .error_description(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
      }

      log.info("[SenseRmController] GET operation = {}", location);

      // We will populate some HTTP response headers.
      final HttpHeaders headers = new HttpHeaders();
      headers.add(HttpHeaders.CONTENT_LOCATION, location.toASCIIString());

      return new ResponseEntity<>(headers, HttpStatus.OK);
    } catch (SecurityException ex) {
      log.error("[SenseRmController] Exception caught", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();
      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
