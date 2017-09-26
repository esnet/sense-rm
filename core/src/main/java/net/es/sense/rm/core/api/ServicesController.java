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

package net.es.sense.rm.core.api;

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
import net.es.sense.rm.core.api.common.Error;
import net.es.sense.rm.core.api.common.HttpConstants;
import net.es.sense.rm.core.api.common.Resource;
import net.es.sense.rm.core.api.common.ResourceAnnotation;
import net.es.sense.rm.core.api.common.Utilities;
import net.es.sense.rm.core.api.config.SenseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
public class ServicesController extends SenseController {

  @Autowired(required = true)
  SenseProperties config;

  private Utilities utilities;

  @PostConstruct
  public void init() throws Exception {
    log.error("ServicesController: " + config.getProxy());
    utilities = new Utilities(config.getProxy());
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
}
