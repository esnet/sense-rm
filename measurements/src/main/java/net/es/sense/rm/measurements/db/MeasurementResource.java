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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author hacksaw
 */
@ApiModel(value = "measurement", description = "This is an operational measurment resource.")
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

  @Id
  @XmlElement(required = true)
  private String id;                      // Unique identifier for this log.

  @Basic(optional = false)
  @XmlElement(required = true)
  private MeasurementType measurement; // Operational measument.

  @Basic(optional = false)
  @XmlElement(required = true)
  private String resource;             // The resource identifier associated with the measurment (modelId, deltaId, etc).

  @Basic(optional = false)
  @XmlElement(required = true)
  private long generated;                   // Generation generated for the measurment.

  @Basic(optional = false)
  @XmlElement(required = true)
  private MetricType mtype;             // Type of metric.

  @Basic(optional = false)
  @XmlElement(required = true)
  private String mvalue;                // Value of metric.

  /**
   * @return the id
   */
  @ApiModelProperty(value = "Unique identifier for measurement resource.", required = true)
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
  @ApiModelProperty(value = "The type of measurement contained in this resource.", required = true)
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
  @ApiModelProperty(value = "The SENSE resource associted with this measurement resource.", required = true)
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
  @ApiModelProperty(value = "The time this measurement was generated.", required = true)
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
  @ApiModelProperty(value = "The type of metric associated with the measurement resource.", required = true)
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
  @ApiModelProperty(value = "The value of metric associated with the measurement resource.", required = true)
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
    sb.append("\"\n  time: \"");
    sb.append(this.getGenerated());
    sb.append("\"\n  type: \"");
    sb.append(this.getMtype());
    sb.append("\"\n  value: \"");
    sb.append(this.getMvalue());
    sb.append("\"/n}");

    return sb.toString();
  }
}
