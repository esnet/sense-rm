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
package net.es.sense.rm.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author hacksaw
 */
@lombok.Builder
@Schema(implementation = Error.class, name = "error", description = "Error structure for REST interface.")
@XmlRootElement(name = "error")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Error {

  @Schema(name = "error", description = "A short error description.", accessMode = Schema.AccessMode.READ_ONLY)
  private String error;               // A short error description.

  @Schema(name = "error_description", description = "Longer error description, human-readable.",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String error_description;   // longer description, human-readable.

  @Schema(name = "error_uri", description = "URI to a detailed error description on the API developer website.",
      accessMode = Schema.AccessMode.READ_ONLY)
  private String error_uri;           // URI to a detailed error description on the API developer website.

  public Error() {}

  public Error(String error, String error_description, String error_uri) {
    this.error = error;
    this.error_description = error_description;
    this.error_uri = error_uri;
  }

  /**
   * @return the error
   */
  @Schema(description = "A short error description.", requiredMode = Schema.RequiredMode.REQUIRED)
  public String getError() {
    return error;
  }

  /**
   * @param error the error to set
   */
  public void setError(String error) {
    this.error = error;
  }

  /**
   * @return the error_description
   */
  @Schema(description = "A Longer human-readable description of error.", requiredMode = Schema.RequiredMode.REQUIRED)
  public String getError_description() {
    return error_description;
  }

  /**
   * @param error_description the error_description to set
   */
  public void setError_description(String error_description) {
    this.error_description = error_description;
  }

  /**
   * @return the error_uri
   */
  @Schema(description = "URI to a detailed error description on the API developer website.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getError_uri() {
    return error_uri;
  }

  /**
   * @param error_uri the error_uri to set
   */
  public void setError_uri(String error_uri) {
    this.error_uri = error_uri;
  }

  @Override
  public String toString() {
    return String.format(
            "{ \n    \"error\" = \"%s\",\n     \"error_description\" = \"%s\",\n    \"error_uri\" = \"%s\"\n}",
            this.error, this.error_description, this.error_uri);
  }
}
