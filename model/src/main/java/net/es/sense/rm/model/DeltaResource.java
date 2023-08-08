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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Calendar;

/**
 *
 * @author hacksaw
 */
@Schema(implementation = DeltaResource.class, name = "delta", description = "This is a topology model delta resource.")
@XmlRootElement(name="delta")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeltaResource {
    @Schema(name = "id", description = "A UUID uniquely identifying the topology model delta resource.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @XmlElement(required=true)
    private String id;                      // A UUID uniquely identifying the topology model delta resource.

    @Schema(name = "href", description = "A URI reference to the resource.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @XmlElement(required=true)
    private String href;                    // A URI reference to the resource.

    @Schema(name = "lastModified",
        description = "The xsd:dateTime formatted date and time (ISO 8601) with time zone specified "
            + "representing the time of the creation, last modification, or state transition of the delta resource.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @XmlElement(required=true)
    private String lastModified = "1970-01-01T00:00:00Z"; // The xsd:dateTime formatted date and time (ISO 8601) with time zone specified representing the time of the creation, last modification, or state transition of the delta resource.

    @Schema(name = "modelId", description = "The UUID of the root model version to which this delta has been applied.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @XmlElement(required=true)
    private String modelId;                 // The UUID of the root model version to which this delta has been applied.

    @Schema(name = "state", description = "The current state of the delta resource.",
        requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = "Accepting, Accepted, Committing, Committed, Activating, Activated, Failed")
    @XmlElement(required=true)
    private DeltaState state;               // The current state of the delta resource. Will contain one of Accepting, Accepted, Committing, Committed, Activating, Activated, or Failed.

    @Schema(name = "reduction",
        description = "The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @XmlElement(required=false)
    private String reduction;               // The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.

    @Schema(name = "addition",
        description = "The gzipped and base64 encoded delta addition for topology model resource specified by modelId.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @XmlElement(required=false)
    private String addition;                // The gzipped and base64 encoded delta addition for topology model resource specified by modelId.

    @Schema(name = "result",
        description = "The gzipped and base64 encoded resulting topology model that will be created by this delta resource.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @XmlElement(required=true)
    private String result;                  // The gzipped and base64 encoded resulting topology model that will be created by this delta resource.

    /**
     * @return the id
     */
    @Schema(description = "Unique identifier for the topology model delta resource.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    @Schema(description = "The xsd:dateTime formatted date and time (ISO 8601) with time zone specified "
        + "representing the time of the creation, last modification, or state transition of the delta resource.",
        requiredMode = Schema.RequiredMode.REQUIRED)
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
    @Schema(hidden=true)
    public Calendar getLastModifiedAsCalendar() throws IllegalArgumentException {
        return jakarta.xml.bind.DatatypeConverter.parseDateTime(this.lastModified);
    }

    /**
     * @return the href
     */
    @Schema(description = "A direct URI reference to the delta resource.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    @Schema(description = "The UUID of the root model version to which this delta has been applied.",
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
     * @return the state
     */
    @Schema(description = "The current state of the delta resource.", requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = "Accepting, Accepted, Committing, Committed, Activating, Activated, Failed")
    public DeltaState getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(DeltaState state) {
        this.state = state;
    }

    public void setState(String state) {
        this.state = DeltaState.valueOf(state);
    }

    /**
     * @return the reduction
     */
    @Schema(description = "The gzipped and base64 encoded delta reduction for topology model resource "
        + "specified by modelId.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    @Schema(description = "The gzipped and base64 encoded delta addition for topology model resource "
        + "specified by modelId.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    @Schema(description = "The gzipped and base64 encoded resulting topology model that will be created "
        + "by this delta resource.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
        sb.append(this.getState());

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
