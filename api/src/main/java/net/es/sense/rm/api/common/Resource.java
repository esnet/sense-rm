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


import io.swagger.v3.oas.annotations.media.Schema;

/**
 *
 * @author hacksaw
 */
@Schema(implementation = Resource.class, name="resource", description="This is a simple API version resource.")
public class Resource {
    @Schema(name = "id", description = "The API name identifier.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;                      // The API name identifier.

    @Schema(name = "href", description = "A URI reference to the resource.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    private String href;                    // A URI reference to the resource.

    @Schema(name = "version", description = "Version of the resource.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String version;                 // Version of the resource.

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{ id: ");
        sb.append(this.getId());
        sb.append("\n  href:");
        sb.append(this.getHref());
        sb.append("\n  version:");
        sb.append(this.getVersion());
        sb.append("/n}");

        return sb.toString();
    }

    /**
     * @return the id
     */
    @Schema(name = "getId", description = "Returns the resource identifier.",
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
    @Schema(name = "getHref", description = "Returns the resource URL.",
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
     * @return the version
     */
    @Schema(name = "getVersion", description = "Returns the version of the resource.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }
}