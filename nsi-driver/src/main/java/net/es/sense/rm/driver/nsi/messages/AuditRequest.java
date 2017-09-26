package net.es.sense.rm.driver.nsi.messages;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
@lombok.Data
@lombok.AllArgsConstructor
public class AuditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String topologyId;
}
