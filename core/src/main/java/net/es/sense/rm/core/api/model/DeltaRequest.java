package net.es.sense.rm.core.api.model;

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
@ApiModel(value = "deltaRequest", description = "This is a propagate request for creation of a topology model delta resource.")
@XmlRootElement(name = "deltaRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeltaRequest {

  @XmlElement(required = false)
  private String modelId;                 // The UUID identifying the topology model resource this delta request is targetting.
  @XmlElement(required = false)
  private String reduction;               // The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.
  @XmlElement(required = false)
  private String addition;                // The gzipped and base64 encoded delta addition for topology model resource specified by modelId.

  /**
   * @return the modelId
   */
  @ApiModelProperty(value = "The UUID of the root model version to which this delta will be applied.", required = false)
  public String getModelId() {
    return modelId;
  }

  /**
   * @param modelId the modelId to set
   */
  public void setModelId(String modelId) {
    this.modelId = modelId;
  }

  /**
   * @return the reduction
   */
  @ApiModelProperty(value = "The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.", required = false)
  public String getReduction() {
    return reduction;
  }

  /**
   * @param reduction the reduction to set
   */
  public void setReduction(String reduction) {
    this.reduction = reduction;
  }

  /**
   * @return the addition
   */
  @ApiModelProperty(value = "The gzipped and base64 encoded delta addition for topology model resource specified by modelId.", required = false)
  public String getAddition() {
    return addition;
  }

  /**
   * @param addition the addition to set
   */
  public void setAddition(String addition) {
    this.addition = addition;
  }
}
