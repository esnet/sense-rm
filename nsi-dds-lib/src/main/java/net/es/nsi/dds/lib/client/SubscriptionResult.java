/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.lib.client;

import lombok.Data;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.dds.lib.jaxb.dds.SubscriptionType;

/**
 *
 * @author hacksaw
 */
@Data
public class SubscriptionResult extends Result {
  SubscriptionType subscription;

  @Override
  public String toString() {
    return String.format("SubscriptionResult[status=%s, lastModified=%s, subscribe=%s]",
        this.getStatus(), this.getLastModified(),
        XmlUtilities.jaxbToXml(SubscriptionType.class, this.getSubscription()));
  }
}
