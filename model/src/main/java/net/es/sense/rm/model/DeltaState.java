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
package net.es.sense.rm.model;

import io.swagger.annotations.ApiModel;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author hacksaw
 */
@ApiModel(value="state", description="The state of the delta resource.")
@XmlType(name = "state")
@XmlEnum
public enum DeltaState {
    Accepting(0), Accepted(1), Committing(2), Committed(3), Failed(4);

  private final long value;

  DeltaState(long value) {
    this.value = value;
  }

  public long value() {
    return value;
  }
}
