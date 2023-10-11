package net.es.sense.rm.driver.nsi.messages;

import akka.actor.ActorPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * This is timer event message.
 *
 * @author hacksaw
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper=true)
@Data
public class TimerMsg extends Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public TimerMsg() {
        super();
    }

    public TimerMsg(String initiator) {
        super(initiator);
    }

    public TimerMsg(String initiator, ActorPath path) {
        super(initiator, path);
    }
}
