/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016 - 2019, The Regents
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
package net.es.sense.rm.measurements.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author hacksaw
 */
@Schema(implementation = MeasurementResource.class,
    name = "measurement",
    description = "This is an operational measurment resource.")
@XmlRootElement(name = "measurement")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(Include.NON_NULL)
@Entity
@Table(name = "measurements")
public class MeasurementResource implements Serializable {

  public MeasurementResource() {
    this.id = UUID.randomUUID().toString();
    this.generated = System.currentTimeMillis();
  }

  public MeasurementResource(String id, MeasurementType measurement, String resource,
          long generated, MetricType mtype, String mvalue) {
    this.id = id;
    this.measurement = measurement;
    this.resource = resource;
    this.generated = generated;
    this.mtype = mtype;
    this.mvalue = mvalue;
  }

  @Schema(name = "id", description = "Unique identifier for this log.", requiredMode = Schema.RequiredMode.REQUIRED)
  @Id
  @XmlElement(required = true)
  private String id;                      // Unique identifier for this log.

  @Schema(name = "measurement", description = "Operational measurement.", requiredMode = Schema.RequiredMode.REQUIRED)
  @Basic(optional = false)
  @XmlElement(required = true)
  private MeasurementType measurement; // Operational measurement.

  @Schema(name = "resource",
      description = "The resource identifier associated with the measurement (modelId, deltaId, etc).",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Basic(optional = false)
  @XmlElement(required = true)
  private String resource;             // The resource identifier associated with the measurement (modelId, deltaId, etc).

  @Schema(name = "generated",
      description = "Generation generated for the measurement.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Basic(optional = false)
  @XmlElement(required = true)
  private long generated;                   // Generation generated for the measurement.

  @Schema(name = "mtype", description = "Type of metric.", requiredMode = Schema.RequiredMode.REQUIRED)
  @Basic(optional = false)
  @XmlElement(required = true)
  private MetricType mtype;             // Type of metric.

  @Schema(name = "mvalue", description = "Value of metric.", requiredMode = Schema.RequiredMode.REQUIRED)
  @Basic(optional = false)
  @XmlElement(required = true)
  private String mvalue;                // Value of metric.

  /**
   * @return the id
   */
  @Schema(description = "Unique identifier for measurement resource.", requiredMode = Schema.RequiredMode.REQUIRED)
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setUuid(String id) {
    this.id = id;
  }

  /**
   * @return the measurement
   */
  @Schema(description = "The type of measurement contained in this resource.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  public MeasurementType getMeasurement() {
    return measurement;
  }

  /**
   * @param measurement the measurement to set
   */
  public void setMeasurement(MeasurementType measurement) {
    this.measurement = measurement;
  }

  /**
   * @return the resource
   */
  @Schema(description = "The SENSE resource associated with this measurement resource.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  public String getResource() {
    return resource;
  }

  /**
   * @param resource the resource to set
   */
  public void setResource(String resource) {
    this.resource = resource;
  }

  /**
   * @return the generated time
   */
  @Schema(description = "The time this measurement was generated.", requiredMode = Schema.RequiredMode.REQUIRED)
  public long getGenerated() {
    return generated;
  }

  /**
   * @param generated the generated time for the log to set
   */
  public void setGenerated(long generated) {
    this.generated = generated;
  }

  /**
   * @return the type
   */
  @Schema(description = "The type of metric associated with the measurement resource.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  public MetricType getMtype() {
    return mtype;
  }

  /**
   * @param mtype the type to set
   */
  public void setMtype(MetricType mtype) {
    this.mtype = mtype;
  }

  /**
   * @return the metric value
   */
  @Schema(description = "The value of metric associated with the measurement resource.",
      requiredMode = Schema.RequiredMode.REQUIRED)
  public String getMvalue() {
    return mvalue;
  }

  /**
   * @param mvalue the metric value to set
   */
  public void setMvalue(String mvalue) {
    this.mvalue = mvalue;
  }

  /**
   *
   * @return
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{ id: \"");
    sb.append(this.getId());
    sb.append("\"\n  measurement: \"");
    sb.append(this.getMeasurement());
    sb.append("\"\n  resource: \"");
    sb.append(this.getResource());
    sb.append("\"\n  generated: \"");
    sb.append(this.getGenerated());
    sb.append("\"\n  mtype: \"");
    sb.append(this.getMtype());
    sb.append("\"\n  mvalue: \"");
    sb.append(this.getMvalue());
    sb.append("\"}");

    return sb.toString();
  }
}
