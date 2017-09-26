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
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.core.api.common.Error;
import net.es.sense.rm.core.api.common.HttpConstants;
import net.es.sense.rm.core.api.common.Resource;
import net.es.sense.rm.core.api.common.ResourceAnnotation;
import net.es.sense.rm.core.api.common.Utilities;
import net.es.sense.rm.core.api.config.SenseProperties;
import net.es.sense.rm.core.api.model.DeltaRequest;
import net.es.sense.rm.core.api.model.DeltaResource;
import net.es.sense.rm.core.api.model.ModelResource;
import net.es.sense.rm.driver.api.Driver;
import net.es.sense.rm.driver.api.Model;
import org.apache.http.client.utils.DateUtils;
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

  private Utilities utilities;
  private Driver driver;

  @PostConstruct
  public void init() throws Exception {
    log.error("SenseRmController: " + config.getProxy());
    utilities = new Utilities(config.getProxy());
    Class<?> forName = Class.forName(config.getDriver());
    driver = context.getBean(forName.asSubclass(Driver.class));
  }

  /**
   * *********************************************************************
   * GET /api/sense/v1
   **********************************************************************
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
   * ***********************************************************************
   * GET /api/sense/v1/models ***********************************************************************
   */
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
                  value = HttpConstants.MODEL_NAME,
                  defaultValue = HttpConstants.MODEL_TURTLE)
          @ApiParam(value = HttpConstants.MODEL_MSG, required = false) String model) {

    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
    Collection<Model> result;
    try {
      Date lastModified = DateUtils.parseDate(ifModifiedSince);
      log.info("[getModels] querying with params {} {} {}.", lastModified, current, model);
      result = driver.getModels(lastModified.getTime(), current, model).get();
    } catch (InterruptedException | ExecutionException ex) {
      log.error("pullModels failed, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();

      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    List<ModelResource> models = new ArrayList<>();
    try {
      for (Model m : result) {
        ModelResource resource = new ModelResource();
        resource.setId(m.getId());
        resource.setCreationTime(DateUtils.formatDate(new Date(m.getCreationTime())));
        resource.setHref(buildURL(location.toASCIIString(), m.getId()));
        resource.setModel(m.getModel());
        models.add(resource);
      }

    } catch (IOException ex) {
      log.error("pullModels failed to parse URL, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();

      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return new ResponseEntity<>(models, HttpStatus.OK);
  }

  private String buildURL(String root, String leaf) throws MalformedURLException {
    URL url;
    if (!root.endsWith("/")) {
      root = root + "/";
    }
    url = new URL(root);
    url = new URL(url, leaf);
    return url.toExternalForm();
  }

  /**
   ***********************************************************************
   * GET /api/sense/v1/models/{id}
   ***********************************************************************
   */
  /**
   * Returns the model identified by id within the Resource Manager.
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
          @PathVariable(HttpConstants.ID_NAME)
          @ApiParam(value = HttpConstants.ID_MSG, required = true) String id) {

    final URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();
    Model result;
    try {
      Date lastModified = DateUtils.parseDate(ifModifiedSince);
      result = driver.getModel(lastModified.getTime(), model, id).get();
    } catch (InterruptedException | ExecutionException ex) {
      log.error("pullModel failed, ex = {}", ex);
      Error error = Error.builder()
              .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
              .error_description(ex.getMessage())
              .build();

      return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    if (result == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    ModelResource resource = new ModelResource();
    resource.setId(result.getId());
    resource.setHref(location.toASCIIString());
    resource.setCreationTime(DateUtils.formatDate(new Date(result.getCreationTime())));
    resource.setModel(result.getModel());
    return new ResponseEntity<>(resource, HttpStatus.OK);
  }

  /**
   * ***********************************************************************
   * GET /api/sense/v1/models/{id}/deltas ***********************************************************************
   */
  /**
   * Returns a list of accepted delta resources associated with the specified SENSE topology model.
   *
   * @param uriInfo The incoming URI context used to invoke the service.
   * @param accept Provides media types that are acceptable for the response. At the moment 'application/json' is the
   * supported response encoding.
   * @param ifModifiedSince The HTTP request may contain the If-Modified-Since header requesting all models with
   * creationTime after the specified date. The date must be specified in RFC 1123 format.
   * @param summary If summary=true then a summary collection of delta resources will be returned including the delta
   * meta-data while excluding the addition, reduction, and result elements. Default value is summary=true.
   * @param model If model=turtle then the returned addition, reduction, and result elements will contain the full
   * topology model in a TURTLE representation. Default value is model=turtle.
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
  public DeltaResource getModelDelta(
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
          @PathVariable(HttpConstants.ID_NAME)
          @ApiParam(value = HttpConstants.ID_MSG, required = true) String id,
          @PathVariable(HttpConstants.DELTAID_NAME)
          @ApiParam(value = HttpConstants.DELTAID_MSG, required = true) String deltaId) {

    DeltaResource delta = new DeltaResource();
    delta.setId("922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setHref("http://localhost:8080/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setLastModified("2016-07-26T13:45:21.001Z");
    delta.setModelId("922f4388-c8f6-4014-b6ce-5482289b02ff");
    delta.setResult("H4sIACKIo1cAA+1Y32/aMBB+56+I6NtUx0loVZEV1K6bpkm0m5Y+7NWNTbCa2JEdCP3vZydULYUEh0CnQXlC+O6+O/ u7X1ylgozp3BJ4LH3rcpJlqQ9hnud23rO5iKDnOA50XKgEgAwnJEEnQ8vuXC30eB5XqXnQuYDqfEl+LnGVvAv/3I6CVQiFv FbF7ff7UKF4Hiice2IZmgMml5RZ8sq/0n9p82hcWFCHCtjtQacHH5AkS5qJkNWa6rDUdD2Y8ZTHPHoqtDudy6lgvpLzGclyL h59zBNE2YBIW/0y7FhvPqjw8X5h5LS40TuUEPyDYTqjeIpi6/OKltZ5IDFnkbzn1gqmxMxO0DyiEUp5qoGfj4YVxiZIfqGYCh JmlDMU/+IiW7W7FIvPOCYDOWUMhOLcT5XG4AJ60PVjyh4HKPbk8NTIBmIxSIRXmpgT4EAXOqWVT4YmwgkNX9w4V wZeu1EddEDEjIZkE4YyktNMgbCoyhhTj2Z1vwVK3PoZ3Fz/DqyvN3ddTYoq49o3bd6mLCNCffFsgqeHwZH1sS04gxmQua 3fbI1I8YIkm5xDr3xCInXmVPPAAEqztAaqtwQFtHQ7vBzJiQmeeoAW5KxwxJisP57VrOuRF2xB1ZZ37M9ixIBALCLrSG9ct 0dI8fy74NN02CQ5Yq2WPaVkE5KqaQ5UIRRRnWinq+51huIpkVbXA2dO/6ztjTZhUUXRWEnYHVUPcwY2zaOafHh553fKz dcErXCLyuuYIlnpEBYo4quVVvtzezO6K2Fd0AMG/arMWuoBLQTcWnqZ9tcNOahhX679/8muNX27IrrgWWBRbZvESFjIsV LdXYhHOYcVVAlylKb6LrtjFEvStnY2Gi4ONAn2Mxh9dJr3mYi2bDjmRWHHXaYe7N+wZk0b2FTH2rHC+EJ28NL7Se9aVp Ry9ZwwyNTDa8Uf627LdXcfM8CWk/4BTQBblaMDjb92ND2C+Gun+yNsz6ZbcdPuLBSQfLvwJ1QggDPmF6Uo5IyVt+rnCq es+AaNt7cbsh/jaxtn//6GsWYDgAEdvHddkj8Wv/3/+bCDnW+rf2DW7Xx/ASQO0KQcHgAA");
    return delta;
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

    log.error("!!!!!! HERE I AM !!!!!!");
    log.info("modelId = " + deltaRequest.getModelId());

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
   * meta-data while excluding the addition, reduction, and result elements. Default value is summary=true.
   * @param model If model=turtle then the returned addition, reduction, and result elements will contain the full
   * topology model in a TURTLE representation. Default value is model=turtle.
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
  public DeltaResource getDelta(
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
          @PathVariable(HttpConstants.DELTAID_NAME)
          @ApiParam(value = HttpConstants.DELTAID_MSG, required = true) String deltaId
  ) {

    DeltaResource delta = new DeltaResource();
    delta.setId("922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setHref("http://localhost:8080/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setLastModified("2016-07-26T13:45:21.001Z");
    delta.setModelId("922f4388-c8f6-4014-b6ce-5482289b02ff");
    delta.setResult("H4sIACKIo1cAA+1Y32/aMBB+56+I6NtUx0loVZEV1K6bpkm0m5Y+7NWNTbCa2JEdCP3vZydULYUEh0CnQXlC+O6+O/ u7X1ylgozp3BJ4LH3rcpJlqQ9hnud23rO5iKDnOA50XKgEgAwnJEEnQ8vuXC30eB5XqXnQuYDqfEl+LnGVvAv/3I6CVQiFv FbF7ff7UKF4Hiice2IZmgMml5RZ8sq/0n9p82hcWFCHCtjtQacHH5AkS5qJkNWa6rDUdD2Y8ZTHPHoqtDudy6lgvpLzGclyL h59zBNE2YBIW/0y7FhvPqjw8X5h5LS40TuUEPyDYTqjeIpi6/OKltZ5IDFnkbzn1gqmxMxO0DyiEUp5qoGfj4YVxiZIfqGYCh JmlDMU/+IiW7W7FIvPOCYDOWUMhOLcT5XG4AJ60PVjyh4HKPbk8NTIBmIxSIRXmpgT4EAXOqWVT4YmwgkNX9w4V wZeu1EddEDEjIZkE4YyktNMgbCoyhhTj2Z1vwVK3PoZ3Fz/DqyvN3ddTYoq49o3bd6mLCNCffFsgqeHwZH1sS04gxmQua 3fbI1I8YIkm5xDr3xCInXmVPPAAEqztAaqtwQFtHQ7vBzJiQmeeoAW5KxwxJisP57VrOuRF2xB1ZZ37M9ixIBALCLrSG9ct 0dI8fy74NN02CQ5Yq2WPaVkE5KqaQ5UIRRRnWinq+51huIpkVbXA2dO/6ztjTZhUUXRWEnYHVUPcwY2zaOafHh553fKz dcErXCLyuuYIlnpEBYo4quVVvtzezO6K2Fd0AMG/arMWuoBLQTcWnqZ9tcNOahhX679/8muNX27IrrgWWBRbZvESFjIsV LdXYhHOYcVVAlylKb6LrtjFEvStnY2Gi4ONAn2Mxh9dJr3mYi2bDjmRWHHXaYe7N+wZk0b2FTH2rHC+EJ28NL7Se9aVp Ry9ZwwyNTDa8Uf627LdXcfM8CWk/4BTQBblaMDjb92ND2C+Gun+yNsz6ZbcdPuLBSQfLvwJ1QggDPmF6Uo5IyVt+rnCq es+AaNt7cbsh/jaxtn//6GsWYDgAEdvHddkj8Wv/3/+bCDnW+rf2DW7Xx/ASQO0KQcHgAA");
    return delta;
  }

  /**
   * ***********************************************************************
   * POST /api/sense/v1/deltas *********************************************************************** @param accept
   *
   * @param deltaRequest
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
          @RequestBody
          @ApiParam(value = "A JSON structure-containing the model reduction and/or addition "
                  + " elements. If provided, the model reduction element is applied first, "
                  + " followed by the model addition element.", required = true) DeltaRequest deltaRequest
  ) throws URISyntaxException {

    log.error("!!!!!! HERE I AM !!!!!!");
    log.info("modelId = " + deltaRequest.getModelId());

    DeltaResource delta = new DeltaResource();
    delta.setId("922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setHref("http://localhost:8080/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200");
    delta.setLastModified("2016-07-26T13:45:21.001Z");
    delta.setModelId("922f4388-c8f6-4014-b6ce-5482289b02ff");
    delta.setResult("H4sIACKIo1cAA+1Y32/aMBB+56+I6NtUx0loVZEV1K6bpkm0m5Y+7NWNTbCa2JEdCP3vZydULYUEh0CnQXlC+O6+O/ u7X1ylgozp3BJ4LH3rcpJlqQ9hnud23rO5iKDnOA50XKgEgAwnJEEnQ8vuXC30eB5XqXnQuYDqfEl+LnGVvAv/3I6CVQiFv FbF7ff7UKF4Hiice2IZmgMml5RZ8sq/0n9p82hcWFCHCtjtQacHH5AkS5qJkNWa6rDUdD2Y8ZTHPHoqtDudy6lgvpLzGclyL h59zBNE2YBIW/0y7FhvPqjw8X5h5LS40TuUEPyDYTqjeIpi6/OKltZ5IDFnkbzn1gqmxMxO0DyiEUp5qoGfj4YVxiZIfqGYCh JmlDMU/+IiW7W7FIvPOCYDOWUMhOLcT5XG4AJ60PVjyh4HKPbk8NTIBmIxSIRXmpgT4EAXOqWVT4YmwgkNX9w4V wZeu1EddEDEjIZkE4YyktNMgbCoyhhTj2Z1vwVK3PoZ3Fz/DqyvN3ddTYoq49o3bd6mLCNCffFsgqeHwZH1sS04gxmQua 3fbI1I8YIkm5xDr3xCInXmVPPAAEqztAaqtwQFtHQ7vBzJiQmeeoAW5KxwxJisP57VrOuRF2xB1ZZ37M9ixIBALCLrSG9ct 0dI8fy74NN02CQ5Yq2WPaVkE5KqaQ5UIRRRnWinq+51huIpkVbXA2dO/6ztjTZhUUXRWEnYHVUPcwY2zaOafHh553fKz dcErXCLyuuYIlnpEBYo4quVVvtzezO6K2Fd0AMG/arMWuoBLQTcWnqZ9tcNOahhX679/8muNX27IrrgWWBRbZvESFjIsV LdXYhHOYcVVAlylKb6LrtjFEvStnY2Gi4ONAn2Mxh9dJr3mYi2bDjmRWHHXaYe7N+wZk0b2FTH2rHC+EJ28NL7Se9aVp Ry9ZwwyNTDa8Uf627LdXcfM8CWk/4BTQBblaMDjb92ND2C+Gun+yNsz6ZbcdPuLBSQfLvwJ1QggDPmF6Uo5IyVt+rnCq es+AaNt7cbsh/jaxtn//6GsWYDgAEdvHddkj8Wv/3/+bCDnW+rf2DW7Xx/ASQO0KQcHgAA");
    URI deltaURI = new URI("http://localhost:8080/sense/v1/deltas/922f4388-c8f6-4014-b6ce-5482289b0200");
    final HttpHeaders headers = new HttpHeaders();
    headers.setLocation(deltaURI);
    return new ResponseEntity<>(delta, headers, HttpStatus.CREATED);
  }
}
