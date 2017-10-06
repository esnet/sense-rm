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
package net.es.sense.rm.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author hacksaw
 */
@ApiModel(value = "model", description = "This is a topology model resource.")
@XmlRootElement(name = "model")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(Include.NON_NULL)
public class ModelResource {

  @XmlElement(required = true)
  private String id;                      // Unique identifier for the resource.
  @XmlElement(required = true)
  private String href;                    // A URI reference to the resource.
  @XmlElement(required = true)
  private String creationTime;            // The time the resource was created/modifed.
  @XmlElement(required = true)
  private String model;                   // The gzipped and base64 encoded model.

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{ id: \"");
    sb.append(this.getId());
    sb.append("\"\n  href: \"");
    sb.append(this.getHref());
    sb.append("\"\n  creationTime: \"");
    sb.append(this.getCreationTime());
    sb.append("\"\n  model: \"");
    sb.append(this.getModel());
    sb.append("\"/n}");

    return sb.toString();
  }

  /**
   * @return the id
   */
  @ApiModelProperty(value = "Unique identifier for the topology model resource.", required = true)
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the href
   */
  @ApiModelProperty(value = "A URI reference to the resource.", required = true)
  public String getHref() {
    return href;
  }

  /**
   * @param href the href to set
   */
  public void setHref(String href) {
    this.href = href;
  }

  /**
   * @return the creationTime
   */
  @ApiModelProperty(value = "The time the resource was created/modifed.", required = true)
  public String getCreationTime() {
    return creationTime;
  }

  /**
   * @param creationTime the creationTime to set
   */
  public void setCreationTime(String creationTime) {
    this.creationTime = creationTime;
  }

  /**
   * @return the model
   */
  @ApiModelProperty(value = "The gzipped and base64 encoded topology model resource.", required = false)
  public String getModel() {
    return model;
  }

  /**
   * @param model the model to set
   */
  public void setModel(String model) {
    this.model = model;
  }
}
