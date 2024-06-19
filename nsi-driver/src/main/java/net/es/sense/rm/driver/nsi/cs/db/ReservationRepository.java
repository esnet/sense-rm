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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * This class models the CrudRepository class for the reservation database service.
 *
 * @author hacksaw
 */
@Repository
public interface ReservationRepository extends CrudRepository<Reservation, Long> {

  public Reservation findOneById(@Param("id") long id);
  public void deleteById(@Param("id") long id);
  public void deleteByUniqueId(@Param("uniqueId") String uniqueId);

  /**
   *
   * @param uniqueId
   * @return
   */
  public Reservation findByUniqueId(@Param("uniqueId") String uniqueId);

  /**
   *
   * @param connectionId
   * @return
   */
  public Collection<Reservation> findByConnectionId(@Param("connectionId") String connectionId);

  /**
   *
   * @param topologyId
   * @return
   */
  public Collection<Reservation> findByTopologyId(@Param ("topologyId") String topologyId);

  /**
   *
   * @param globalReservationId
   * @return
   */
  public Collection<Reservation> findByGlobalReservationId(@Param ("globalReservationId") String globalReservationId);

  /**
   *
   * @param providerNsa
   * @param connectionId
   * @return
   */
  public Collection<Reservation> findByProviderNsaAndConnectionId(
          @Param ("providerNsa") String providerNsa,
          @Param("connectionId") String connectionId);

  public Reservation findByProviderNsaAndConnectionIdAndVersion(
      @Param ("providerNsa") String providerNsa,
      @Param("connectionId") String connectionId,
      @Param("version") int version);

  /**
   * Lookup a reservation based on providerId and parentConnectionId.
   *
   * @param providerNsa
   * @param parentConnectionId
   * @return
   */
  public Collection<Reservation> findByProviderNsaAndParentConnectionId(
          @Param ("providerNsa") String providerNsa,
          @Param("parentConnectionId") String parentConnectionId);

    /**
   * Lookup a reservation based on parentConnectionId.
   *
   * @param parentConnectionId
   * @return
   */
  public Collection<Reservation> findByParentConnectionId(
          @Param("parentConnectionId") String parentConnectionId);

  /**
   *
   * @param providerNsa
   * @return
   */
  public Collection<Reservation> findByProviderNsa(
          @Param ("providerNsa") String providerNsa);

  /**
   * Get the discovered time of the newest reservation.
   *
   * @return
   */
  @Query("select max(m.discovered) from #{#entityName} m")
  public Long findNewestDiscovered();

  /**
   * Get the newest reservation.
   *
   * @return
   */
  public Collection<Reservation> findFirst1ByOrderByDiscoveredAsc();

  /**
   * Get the newest reservation.  Using spring hibernate's native query capabilities we
   * structure the query to "find First1 By Order By Discovered Desc", meaning return
   * the first matching entry ordered in descending sequence by sorting on the discovered
   * attribute.  Or in layman's terms, return the newest reservation.
   *
   * @return
   */
  public Collection<Reservation> findFirst1ByOrderByDiscoveredDesc();

  /**
   * Find all reservations newer than the specified lastModified time.
   *
   * @param lastModified
   * @return
   */
  @Query("select m from #{#entityName} m where m.discovered > :lastModified")
  public Collection<Reservation> findAllNewer(@Param("lastModified") long lastModified);

  /**
   * Find the newest reservation based on most recent discovered time.
   *
   * @return
   */
  @Query("select distinct m from #{#entityName} m where m.discovered = (select max(t.discovered) from #{#entityName} t)")
  public Reservation findNewest();

  /**
   *
   * @param id
   * @param connectionId
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.connectionId = :connectionId where r.id = :id")
  public int setConnectionId(@Param("id") long id, @Param("connectionId") String connectionId);

  /**
   *
   * @param id
   * @param reservationState
   * @param discovered
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.reservationState = :reservationState, "
          + "r.dirty = false, "
          + "r.discovered = :discovered "
          + "where r.id = :id")
  public int setReservationState(
          @Param("id") long id,
          @Param("reservationState") ReservationStateEnumType reservationState,
          @Param("discovered") long discovered);

  /**
   *
   * @param id
   * @param reservationState
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.reservationState = :reservationState, "
      + "r.dirty = false "
      + "where r.id = :id")
  public int setReservationState(
      @Param("id") long id,
      @Param("reservationState") ReservationStateEnumType reservationState);

  /**
   *
   * @param id
   * @param provisionState
   * @param discovered
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.provisionState = :provisionState, "
          + "r.discovered = :discovered "
          + "where r.id = :id")
  public int setProvisionState(
          @Param("id") long id,
          @Param("provisionState") ProvisionStateEnumType provisionState,
          @Param("discovered") long discovered);

  /**
   *
   * @param id
   * @param lifecycleState
   * @param discovered
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.lifecycleState = :lifecycleState, "
          + "r.discovered = :discovered "
          + "where r.id = :id")
  public int setLifecycleState(
          @Param("id") long id,
          @Param("lifecycleState") LifecycleStateEnumType lifecycleState,
          @Param("discovered") long discovered);

  /**
   *
   * @param id
   * @param reservationState
   * @param lifecycleState
   * @param discovered
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.reservationState = :reservationState, "
          + "r.lifecycleState = :lifecycleState, "
          + "r.discovered = :discovered "
          + "where r.id = :id")
  public int setReservationAndLifecycleState(
          @Param("id") long id,
          @Param("reservationState") ReservationStateEnumType reservationState,
          @Param("lifecycleState") LifecycleStateEnumType lifecycleState,
          @Param("discovered") long discovered);

  /**
   *
   * @param id
   * @param dataPlaneActive
   * @param discovered
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.dataPlaneActive = :dataPlaneActive, r.discovered = :discovered where r.id = :id")
  public int setDataPlaneActive(@Param("id") long id, @Param("dataPlaneActive") boolean dataPlaneActive, @Param("discovered") long discovered);

  /**
   *
   * @param id
   * @param reservationState
   * @param errorState
   * @param errorMessage
   * @param discovered
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.reservationState = :reservationState, "
          + "r.errorState = :errorState, "
          + "r.errorMessage = :errorMessage, "
          + "r.discovered = :discovered "
          + "where r.id = :id")
  public int setReserveFailed(
          @Param("id") long id,
          @Param("reservationState") ReservationStateEnumType reservationState,
          @Param("errorState") Reservation.ErrorState errorState,
          @Param("errorMessage") String errorMessage,
          @Param("discovered") long discovered);

  /**
   *
   * @param id
   * @param reservationState
   * @param lifecycleState
   * @param errorState
   * @param errorMessage
   * @param discovered
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.reservationState = :reservationState, "
          + "r.lifecycleState = :lifecycleState, "
          + "r.errorState = :errorState, "
          + "r.errorMessage = :errorMessage, "
          + "r.discovered = :discovered "
          + "where r.id = :id")
  public int setFailedState(
          @Param("id") long id,
          @Param("reservationState") ReservationStateEnumType reservationState,
          @Param("lifecycleState") LifecycleStateEnumType lifecycleState,
          @Param("errorState") Reservation.ErrorState errorState,
          @Param("errorMessage") String errorMessage,
          @Param("discovered") long discovered);

  /**
   *
   * @param id
   * @param errorState
   * @param errorMessage
   * @param discovered
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.errorState = :errorState, "
          + "r.errorMessage = :errorMessage, "
          + "r.discovered = :discovered "
          + "where r.id = :id")
  public int setErrorState(
          @Param("id") long id,
          @Param("errorState") Reservation.ErrorState errorState,
          @Param("errorMessage") String errorMessage,
          @Param("discovered") long discovered);

  /**
   *
   * @param id
   * @param dirty
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.dirty = :dirty where r.id = :id")
  public int setDirty(@Param("id") long id, @Param("dirty") boolean dirty);

  /**
   *
   * @param id
   * @param version
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.version = :version where r.id = :id")
  public int setVersion(@Param("id") long id, @Param("version") int version);

}
