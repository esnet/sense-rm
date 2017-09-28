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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Calendar;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author hacksaw
 */
@ApiModel(value="delta", description="This is a topology model delta resource.")
@XmlRootElement(name="delta")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeltaResource {
    @XmlElement(required=true)
    private String id;                      // A UUID uniquely identifying the topology model delta resource.
    @XmlElement(required=true)
    private String href;                    // A URI reference to the resource.
    @XmlElement(required=true)
    private String lastModified = "1970-01-01T00:00:00Z"; // The xsd:dateTime formatted date and time (ISO 8601) with time zone specified representing the time of the creation, last modification, or state transition of the delta resource.
    @XmlElement(required=true)
    private String modelId;                 // The UUID of the root model version to which this delta has been applied.
    @XmlElement(required=true)
    private DeltaState state;               // The current state of the delta resource. Will contain one of Accepting, Accepted, Committing, Committed, Activating, Activated, or Failed.
    @XmlElement(required=false)
    private String reduction;               // The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.
    @XmlElement(required=false)
    private String addition;                // The gzipped and base64 encoded delta addition for topology model resource specified by modelId.
    @XmlElement(required=true)
    private String result;                  // The gzipped and base64 encoded resulting topology model that will be created by this delta resource.

    /**
     * @return the id
     */
    @ApiModelProperty(value = "Unique identifier for the topology model delta resource.", required=true)
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
     * @return the lastModified
     */
    @ApiModelProperty(value = "The xsd:dateTime formatted date and time (ISO 8601) with time zone specified representing the time of the creation, last modification, or state transition of the delta resource.", required=true)
    public String getLastModified() {
        return lastModified;
    }

    /**
     * @param lastModified the lastModified to set
     */
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    /**
     *
     * @return Calendar representing the stored lastModified string.
     * @throws IllegalArgumentException if string parameter does not conform to lexical value space defined in XML Schema Part 2: Datatypes for xsd:dateTime.
     */
    @ApiModelProperty(hidden=true)
    public Calendar getLastModifiedAsCalendar() throws IllegalArgumentException {
        return javax.xml.bind.DatatypeConverter.parseDateTime(this.lastModified);
    }

    /**
     * @return the href
     */
    @ApiModelProperty(value = "A direct URI reference to the delta resource.", required=true)
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
     * @return the modelId
     */
    @ApiModelProperty(value = "The UUID of the root model version to which this delta has been applied.", required=true)
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
     * @return the state
     */
    @ApiModelProperty(value = "The current state of the delta resource.", required=true, allowableValues = "Accepting, Accepted, Committing, Committed, Activating, Activated, Failed")
    public DeltaState getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(DeltaState state) {
        this.state = state;
    }

    /**
     * @return the reduction
     */
    @ApiModelProperty(value = "The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.", required=false)
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
    @ApiModelProperty(value = "The gzipped and base64 encoded delta addition for topology model resource specified by modelId.", required=false)
    public String getAddition() {
        return addition;
    }

    /**
     * @param addition the addition to set
     */
    public void setAddition(String addition) {
        this.addition = addition;
    }

    /**
     * @return the result
     */
    @ApiModelProperty(value = "The gzipped and base64 encoded resulting topology model that will be created by this delta resource.", required=false)
    public String getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{ id: \"");
        sb.append(this.getId());

        sb.append("\"\n  href: \"");
        sb.append(this.getHref());

        sb.append("\"\n  lastModified: \"");
        sb.append(this.getLastModified());

        sb.append("\"\n  modelId: \"");
        sb.append(this.getModelId());

        sb.append("\"\n  state: \"");
        sb.append(this.getState().name());

        sb.append("\"\n  reduction: \"");
        sb.append(this.getReduction());

        sb.append("\"\n  addition: \"");
        sb.append(this.getAddition());

        sb.append("\"\n  result: \"");
        sb.append(this.getResult());

        sb.append("\"/n}");

        return sb.toString();
    }
}
