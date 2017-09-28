
package net.es.sense.rm.api.config;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;

/**
 *
 * @author hacksaw
 */
public class SwaggerInfo {
  private static final ApiInfo apiInfo = new ApiInfoBuilder()
            .title("SENSE Services API")
            .description("This API provides access to programmable SENSE features.")
            .termsOfServiceUrl("SENSE Resource Manager Copyright (c) 2016, The Regents of the University of California, through Lawrence Berkeley National Laboratory (subject to receipt of any required approvals from the U.S. Dept. of Energy).  All rights reserved.")
            .license("Lawrence Berkeley National Labs BSD variant license")
            .licenseUrl("https://spdx.org/licenses/BSD-3-Clause-LBNL.html")
            .contact(new Contact("SENSE Development Team", "https://github.com/esnet/sense-rm", "sense@es.net"))
            .version("v1")
            .build();

  public static ApiInfo apiInfo() {
    return apiInfo;
  }
}
