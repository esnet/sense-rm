/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2020, The Regents
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

import lombok.extern.slf4j.Slf4j;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides an audit function to remove any connections that should not be in
 * the reservation database.
 *
 * @author hacksaw
 */
@Slf4j
public class ReservationAudit {
  private final List<String> auditable = new ArrayList<>();

  public void add(String providerId, String connectionId, int version) {
    auditable.add(providerId + ":" + connectionId + ":" + version);
  }

  /**
   * Find any NSI reservations in the database that are not in the current
   * list returned from the NSA.  We need to be tricky since removing a
   * reservation also removes our ability to indicated a change that requires
   * a new topology model to be generated.
   *
   * @param reservationService
   */
  public void audit(ReservationService reservationService) {
    for (Reservation reservation : reservationService.get()) {
      String cid = reservation.getProviderNsa() + ":" + reservation.getConnectionId() + ":" + reservation.getVersion();
      log.debug("[ReservationAudit] auditing cid = {}", cid);
      if (auditable.contains(cid)) {
        log.debug("[ReservationAudit] found cid = {}", cid);
      } else {
        log.debug("[ReservationAudit] not found in DB cid = {}", cid);
        // First thing we do is mark this missing reservation as dirty just
        // in case this is a race condition and the reservation was created
        // but not yet in the query results.
        if (!reservation.isDirty()) {
          log.debug("[ReservationAudit] marking dirty cid = {}", reservation.getConnectionId());
          reservationService.setDirty(reservation.getId(), true);
        } else if (reservation.getLifecycleState() != LifecycleStateEnumType.TERMINATED) {
          /// Now change the dirty non-existing reservation to a TERMINATED
          // state and store it in hopes of triggering an audit.
          log.debug("[ReservationAudit] transitioning cid = {}, from {} to {}",
                  reservation.getConnectionId(), reservation.getLifecycleState(),
                  LifecycleStateEnumType.TERMINATED);

          reservationService.setLifecycleState(reservation.getId(), LifecycleStateEnumType.TERMINATED,
                  System.currentTimeMillis());
        } else {
          // This is probably our third time through so we can just delete it now.
          log.debug("[ReservationAudit] deleting cid = {}, lifecycleState = {}",
                  reservation.getConnectionId(), reservation.getLifecycleState());
          reservationService.delete(reservation);
        }
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("ReservationAudit {\n");
    for (String key : auditable) {
      result.append(key);
      result.append(",\n");
    }
    result.append("}");
    return result.toString();
  }
}
