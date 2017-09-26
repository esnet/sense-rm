
package net.es.sense.rm.core.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.core.api.common.HttpConstants;
import net.es.sense.rm.core.api.common.Resource;
import net.es.sense.rm.core.api.common.ResourceAnnotation;
import net.es.sense.rm.core.api.common.Utilities;
import net.es.sense.rm.core.api.config.SenseProperties;
import net.es.sense.rm.core.api.model.DeltaResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

  private Utilities utilities;

  @PostConstruct
  public void init() throws Exception {
    log.error("SenseAdminController: " + config.getProxy());
    utilities = new Utilities(config.getProxy());
  }

  /*************************************************************************
   * GET /api/admin/v1
   *************************************************************************/

  /**
   * Returns a list of available SENSE service API resources.
   *
   * @param uriInfo The incoming URI context used to invoke the service.
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
                    response = net.es.sense.rm.core.api.common.Error.class),
            @ApiResponse(
                    code = HttpConstants.FORBIDDEN_CODE,
                    message = HttpConstants.FORBIDDEN_MSG,
                    response = net.es.sense.rm.core.api.common.Error.class),
            @ApiResponse(
                    code = HttpConstants.INTERNAL_ERROR_CODE,
                    message = HttpConstants.INTERNAL_ERROR_MSG,
                    response = net.es.sense.rm.core.api.common.Error.class),
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
}
