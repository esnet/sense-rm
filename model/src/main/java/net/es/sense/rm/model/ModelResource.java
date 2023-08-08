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
package net.es.sense.rm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author hacksaw
 */
@Schema(implementation = ModelResource.class, name = "model", description = "This is a topology model resource.")
@XmlRootElement(name = "model")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(Include.NON_NULL)
public class ModelResource {
  @Schema(name = "id",
      description = "Unique identifier for the resource.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @XmlElement(required = true)
  private String id;                      // Unique identifier for the resource.

  @Schema(name = "href",
      description = "A URI reference to the resource.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @XmlElement(required = true)
  private String href;                    // A URI reference to the resource.

  @Schema(name = "creationTime",
      description = "The time the resource was created/modified.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @XmlElement(required = true)
  private String creationTime;            // The time the resource was created/modified.

  @Schema(name = "model",
      description = "The gzipped and base64 encoded model.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @XmlElement(required = true)
  private String model;                   // The gzipped and base64 encoded model.

  /**
   * @return the id
   */
  @Schema(name = "getId",
      description = "Unique identifier for the topology model resource.",
      requiredMode = Schema.RequiredMode.REQUIRED)
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
  @Schema(name = "getHref",
      description = "A URI reference to the resource.",
      requiredMode = Schema.RequiredMode.REQUIRED)
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
  @Schema(name = "getCreationTime",
      description = "The time the resource was created/modified.",
      requiredMode = Schema.RequiredMode.REQUIRED)
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
  @Schema(name = "getModel",
      description = "The gzipped and base64 encoded topology model resource.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getModel() {
    return model;
  }

  /**
   * @param model the model to set
   */
  public void setModel(String model) {
    this.model = model;
  }

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
}
