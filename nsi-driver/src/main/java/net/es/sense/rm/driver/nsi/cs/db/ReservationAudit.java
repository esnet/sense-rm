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

import java.util.HashMap;
import java.util.Map;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;

/**
 * Provides an audit function to remove any connections that should not be in
 * the reservation database.
 *
 * @author hacksaw
 */
public class ReservationAudit {
  private class Entry {
    String providerId;
    String connectionId;

    public Entry() {

    }

    public Entry(String providerId, String connectionId) {
      this.providerId = providerId;
      this.connectionId = connectionId;
    }
  }

  private final ReservationService reservationService;
  private Map<String, Entry> auditable = new HashMap<>();

  public ReservationAudit(ReservationService reservationService) {
    this.reservationService = reservationService;
  }

  public void add(String providerId, String connectionId) {
    Entry entry = new Entry(providerId, connectionId);
    auditable.put(providerId + ":" + connectionId, entry);
  }

  /**
   * Find any NSI reservations in the database that are not in the current
   * list returned from the NSA. We need to be tricky since removing a
   * reservation also removes our ability to indicated a change that requires
   * a new topology model to be generated.
   */
  public void audit() {
    for (Reservation reservation : reservationService.get()) {
      String cid = reservation.getProviderNsa() + ":" + reservation.getConnectionId();
      if (auditable.get(cid) == null) {
        // First trick is toidentify a change by setting this non-existing
        // reservation to a TERMINATED state and store it in hopes of
        // triggering an audit.
        if (reservation.getLifecycleState() != LifecycleStateEnumType.TERMINATED) {
          reservation.setLifecycleState(LifecycleStateEnumType.TERMINATED);
          reservation.setDiscovered(System.currentTimeMillis());
          reservationService.store(reservation);
        } else {
          // This is probably our second time through so we can just delete it now.
          reservationService.delete(reservation);
        }
      }
    }
  }
}
