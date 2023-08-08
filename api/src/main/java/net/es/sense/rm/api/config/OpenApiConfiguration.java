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
package net.es.sense.rm.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OpenApiConfiguration {

  @Bean
  public OpenAPI customOpenAPI() {
    log.info("Initializing swagger.");
    return new OpenAPI().info(apiInfo());
  }

  private Info apiInfo() {
    return new Info()
        .title("SENSE Services API")
        .description("This API provides access to programmable SENSE features.")
        .termsOfService("SENSE Resource Manager Copyright (c) 2016, The Regents of the University of California, through Lawrence Berkeley National Laboratory (subject to receipt of any required approvals from the U.S. Dept. of Energy).  All rights reserved.")
        .version("v1")
        .contact(apiContact())
        .license(apiLicence());
  }

  private License apiLicence() {
    return new License()
        .name("The 3-Clause BSD License")
        .url("https://opensource.org/license/bsd-3-clause/");
  }

  private Contact apiContact() {
    return new Contact()
        .name("SENSE Development Team")
        .email("sense@es.net")
        .url("https://github.com/esnet/sense-rm");
  }
}
