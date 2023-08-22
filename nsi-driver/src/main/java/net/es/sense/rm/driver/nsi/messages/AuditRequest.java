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
package net.es.sense.rm.driver.nsi.messages;

import akka.actor.ActorPath;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AuditRequest extends Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String topologyId;

    public AuditRequest() {
        super();
    }

    public AuditRequest(String initiator) {
        super(initiator);
    }

    public AuditRequest(String initiator, ActorPath path) {
        super(initiator, path);
    }

    public AuditRequest(String initiator, ActorPath path, String topologyId) {
        super(initiator, path);
        this.topologyId = topologyId;
    }

    public AuditRequest(String initiator, String topologyId) {
        super(initiator);
        this.topologyId = topologyId;
    }
}
