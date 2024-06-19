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
package net.es.sense.rm.driver.nsi.db;

import jakarta.persistence.*;
import net.es.sense.rm.model.DeltaState;

import java.io.Serializable;

/**
 * A delta object for storage.  A SENSE delta object represents a topology change request
 * that we map into an NSI-CS reservation operation.
 * 
 * @author hacksaw
 */
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Data
@Entity
@Table(name = "delta")
public class Delta implements Serializable {

  @Id
  @GeneratedValue
  private long idx;                 // Internal database index.

  @Basic(optional = false)
  private String deltaId;           // The unique uuid identifying the delta within the RM.

  @Basic(optional = false)
  private long lastModified = 0;    // Representing the time of the creation, last modification, or _state
                                    // transition of the delta resource.

  @Basic(optional = false)
  private String modelId;           // The UUID of the root model version to which this delta has been applied.

  @Basic(optional = false)
  private DeltaState _state;         // The current _state of the delta resource. Will contain one of
                                     // Accepting, Accepted, Committing, Committed, Activating, Activated,
                                     // or Failed.

  @Lob
  @Basic(fetch=FetchType.LAZY, optional=true)
  private String reduction;         // The delta reduction for topology model resource specified by modelId.

  @Lob
  @Basic(fetch=FetchType.LAZY, optional=true)
  private String addition;          // The delta addition for topology model resource specified by modelId.

  @Lob
  @Basic(fetch=FetchType.LAZY, optional=true)
  private String _result;            // resulting topology model that will be created by this delta resource.

  public DeltaState getState() {
    return _state;
  }

  public String getResult() {
    return _result;
  }

  public void setState(DeltaState state) {
    this._state = state;
  }

  public void setResult(String result) {
    this._result = result;
  }
}
