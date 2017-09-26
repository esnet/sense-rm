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
package net.es.sense.rm.driver.api;

import java.util.Date;

/**
 *
 * @author hacksaw
 */
@lombok.Data
@lombok.Builder
public class Delta {
    private String id;                      // A UUID uniquely identifying the topology model delta resource.
    @lombok.Builder.Default
    private long lastModified = 0; // The xsd:dateTime formatted date and time (ISO 8601) with time zone specified representing the time of the creation, last modification, or state transition of the delta resource.
    private String modelId;                 // The UUID of the root model version to which this delta has been applied.
    private DeltaState state;               // The current state of the delta resource. Will contain one of Accepting, Accepted, Committing, Committed, Activating, Activated, or Failed.
    private String reduction;               // The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.
    private String addition;                // The gzipped and base64 encoded delta addition for topology model resource specified by modelId.
    private String result;                  // The gzipped and base64 encoded resulting topology model that will be created by this delta resource.

    /**
     *
     * @return Calendar representing the stored lastModified string.
     * @throws IllegalArgumentException if string parameter does not conform to lexical value space defined in XML Schema Part 2: Datatypes for xsd:dateTime.
     */
    public Date getLastModifiedAsDate() throws IllegalArgumentException {
      return new Date(lastModified);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{ id: \"");
        sb.append(this.getId());

        sb.append("\"\n  lastModified: \"");
        sb.append(new Date(this.getLastModified()));

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
