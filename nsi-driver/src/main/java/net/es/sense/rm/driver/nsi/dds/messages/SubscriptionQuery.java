package net.es.sense.rm.driver.nsi.dds.messages;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
@lombok.Data
public class SubscriptionQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private String url;
}
