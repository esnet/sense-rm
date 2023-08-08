package net.es.sense.rm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author hacksaw
 */
@Schema(implementation = DeltaRequest.class, name = "deltaRequest",
    description = "This is a propagate request for creation of a topology model delta resource.")
@XmlRootElement(name = "deltaRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeltaRequest {
  @Schema(name = "id",
      description = "The UUID identifying the delta being pushed into the RM.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @XmlElement(required = true)
  private String id;                 // The UUID identifying the delta being pushed into the RM.

  @Schema(name = "modelId",
      description = "The UUID identifying the topology model resource this delta request is targeting.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @XmlElement(required = true)
  private String modelId;            // The UUID identifying the topology model resource this delta request is targeting.

  @Schema(name = "reduction",
      description = "The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @XmlElement(required = false)
  private String reduction;          // The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.

  @Schema(name = "addition",
      description = "The gzipped and base64 encoded delta addition for topology model resource specified by modelId.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @XmlElement(required = false)
  private String addition;           // The gzipped and base64 encoded delta addition for topology model resource specified by modelId.

  /**
   * @return the modelId
   */
  @Schema(description = "The UUID identifying the delta being pushed into the RM.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  public String getId() {
    return id;
  }

  /**
   * @param id the modelId to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the modelId
   */
  @Schema(description = "The UUID of the root model version to which this delta will be applied.",
      requiredMode = Schema.RequiredMode.REQUIRED)
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
  @Schema(description = "The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
  @Schema(description = "The gzipped and base64 encoded delta addition for topology model resource specified by modelId.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getAddition() {
    return addition;
  }

  /**
   * @param addition the addition to set
   */
  public void setAddition(String addition) {
    this.addition = addition;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{ \n    modelId = ");
    sb.append(modelId);
    sb.append(",\n    addition = \"");
    sb.append(addition);
    sb.append("\",\n    reduction = \"");
    sb.append(reduction);
    sb.append("\"\n}\n");
    return sb.toString();
  }
}
