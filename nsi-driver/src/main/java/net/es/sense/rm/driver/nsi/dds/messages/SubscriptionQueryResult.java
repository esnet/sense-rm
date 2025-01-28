package net.es.sense.rm.driver.nsi.dds.messages;

import java.io.Serial;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import net.es.sense.rm.driver.nsi.dds.db.Subscription;
import net.es.sense.rm.driver.nsi.messages.Message;

/**
 *
 * @author hacksaw
 */
@EqualsAndHashCode(callSuper = true)
@lombok.Data
public class SubscriptionQueryResult extends Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Subscription subscription;

    @Override
    public String toString() {
        return String.format("SubscriptionQueryResult[initiator=%s, path=%s, subscription=%s]",
            this.getInitiator(), this.getPath(), this.getSubscription());
    }
}

