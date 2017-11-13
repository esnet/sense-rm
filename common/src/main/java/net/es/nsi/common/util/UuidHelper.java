/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.common.util;

import java.util.UUID;

/**
 *
 * @author hacksaw
 */
public class UuidHelper {
  private final static String URN_UUID = "urn:uuid:";

  public static String getUUID() {
    return URN_UUID + UUID.randomUUID().toString();
  }
}
