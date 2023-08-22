package net.es.sense.rm.driver.nsi.messages;

import akka.actor.ActorPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper=true)
@Data
public class StartMsg extends Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public StartMsg() {
        super();
    }

    public StartMsg(String initiator) {
        super(initiator);
    }

    public StartMsg(String initiator, ActorPath path) {
        super(initiator, path);
    }
}
