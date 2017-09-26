/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.sense.rm.driver.api;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
@lombok.Data
@lombok.Builder
public class Model implements Serializable {


  private String id;           // Unique identifier for the resource.
  private long   creationTime; // The time the resource was created/modifed.
  private String model;        // The gzipped and base64 encoded model.

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{ id: \"");
    sb.append(this.getId());
    sb.append("\"\n  creationTime: \"");
    sb.append(this.getCreationTime());
    sb.append("\"\n  model: \"");
    sb.append(this.getModel());
    sb.append("\"/n}");

    return sb.toString();
  }
}
