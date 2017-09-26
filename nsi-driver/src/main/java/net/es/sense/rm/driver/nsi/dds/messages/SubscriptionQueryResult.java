package net.es.sense.rm.driver.nsi.dds.messages;

import java.io.Serializable;
import net.es.sense.rm.driver.nsi.dds.db.Subscription;

/**
 *
 * @author hacksaw
 */
@lombok.Data
public class SubscriptionQueryResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private Subscription subscription;
}

