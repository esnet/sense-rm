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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author hacksaw
 */
@lombok.Builder
@ApiModel(value = "error", description = "Error structure for REST interface.")
@XmlRootElement(name = "error")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Error {

  private String error;               // A short error description.
  private String error_description;   // longer description, human-readable.
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
  @ApiModelProperty(value = "A short error description.", required = true)
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
  @ApiModelProperty(value = "A Longer human-readable description of error.", required = true)
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
  @ApiModelProperty(value = "URI to a detailed error description on the API developer website.", required = false)
  public String getError_uri() {
    return error_uri;
  }

  /**
   * @param error_uri the error_uri to set
   */
  public void setError_uri(String error_uri) {
    this.error_uri = error_uri;
  }
}
