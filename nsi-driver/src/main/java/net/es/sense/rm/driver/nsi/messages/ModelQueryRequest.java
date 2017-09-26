/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.sense.rm.driver.nsi.messages;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
@lombok.Data
@lombok.Builder
public class ModelQueryRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  private ModelQueryType type;
  private long lastModified;
  private String topologyId;
  private String id;
  private boolean current;
}
