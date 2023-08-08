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
package net.es.sense.rm.driver.nsi.cs.db;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class models a mapping of an MRML delta to an NSI reservation.
 *
 * @author hacksaw
 */
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Data
@Entity
@Table(name = "connectionmap")
public class ConnectionMap implements Serializable {
  @Id
  @GeneratedValue
  @Column(nullable = false)
  long id;

  long lastAudit = 0;

  @Basic(optional=false)
  String description;

  @Basic(optional=false)
  String deltaId;

  @Basic(optional=false)
  String switchingSubnetId;

  @Basic(optional=false)
  String serviceType;

  @Basic(optional=false)
  String existsDuringId;

  @ElementCollection(fetch=FetchType.EAGER, targetClass=StpMapping.class)
  List<StpMapping> map = new ArrayList<>();

  public Optional<StpMapping> findMapping(String stpId) {
    return map.stream().filter(s -> s.getStpId().equalsIgnoreCase(stpId)).findFirst();
  }
}
