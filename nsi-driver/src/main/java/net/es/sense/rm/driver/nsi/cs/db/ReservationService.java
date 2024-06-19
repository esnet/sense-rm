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

import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ProvisionStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
@Service
public interface ReservationService {

  public long getLastDiscovered();

  public Reservation getNewest();

  public Reservation findNewest();

  public Reservation store(Reservation reservation);

  public int setConnectionId(long id, String connectionId);

  public int setDataPlaneActive(long id, boolean dataPlaneActive, long discovered);

  public int setFailedState(long id, ReservationStateEnumType reservationState,
        LifecycleStateEnumType lifecycleState, Reservation.ErrorState errorState,
        String errorMessage, long discovered);

  public int setErrorState(long id, Reservation.ErrorState errorState, String errorMessage, long discovered);

  public int setReservationState(long id, ReservationStateEnumType reservationState, long discovered);

  public int setReservationState(long id, ReservationStateEnumType reservationState);

  public int setProvisionState(long id, ProvisionStateEnumType provisionState, long discovered);

  public int setLifecycleState(long id, LifecycleStateEnumType lifecycleState, long discovered);

  public int setReservationAndLifecycleState(long id, ReservationStateEnumType reservationState,
          LifecycleStateEnumType lifecycleState, long discovered);

  public int setReserveFailed(long id, ReservationStateEnumType reservationState,
          Reservation.ErrorState errorState, String errorMessage, long discovered);

  public void delete(Reservation reservation);

  public void delete(@Param("id") long id);

  public void delete();

  public void deleteByUniqueId(@Param("uniqueId") String uniqueId);

  public int setDirty(long id, boolean dirty);

  public int setVersion(long id, int version);

  public Collection<Reservation> get();

  public Collection<Reservation> getByTopologyId(String topologyId);

  public Collection<Reservation> getByGlobalReservationId(String globalReservationId);

  public Reservation getByUniqueId(String uniqueId);

  public Reservation get(long id);

  public Collection<Reservation> get(String connectionId);

  public Collection<Reservation> getByProviderNsa(String providerNsa);

  public Collection<Reservation> getByProviderNsaAndConnectionId(String providerNSA, String connectionId);

  public Reservation getByProviderNsaAndConnectionIdAndVersion(String providerNsa, String connectionId, int version);

  public Collection<Reservation> getByParentConnectionId(String parentConnectionId);

  public Collection<Reservation> getByParentConnectionId(String providerNsa, String parentConnectionId);

  public Collection<Reservation> getByAnyConnectionId(String providerNsa, String connectionId);
}
