/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.lib.client;

import jakarta.ws.rs.core.Response.Status;
import lombok.Data;

/**
 *
 * @author hacksaw
 */
@Data
public class Result {
  Status status;
  long lastModified;
}
