package net.es.sense.rm.driver.nsi.messages;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author hacksaw
 */
@Data
@AllArgsConstructor
public class AuditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String topologyId;
}
