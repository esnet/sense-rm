/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.lib.client;

import lombok.Data;
import net.es.nsi.dds.lib.jaxb.dds.SubscriptionType;

/**
 *
 * @author hacksaw
 */
@Data
public class SubscriptionResult extends Result {
  SubscriptionType subscription;
}
