package net.es.sense.rm.driver.nsi.cs.db;

import java.util.Collection;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ProvisionStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author hacksaw
 */
@Repository
public interface ReservationRepository extends CrudRepository<Reservation, Long> {

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
  public Reservation findByProviderNsaAndConnectionId(
          @Param ("providerNsa") String providerNsa,
          @Param("connectionId") String connectionId);

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
   * Get the newest reservation.
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
   * @param reservationState
   * @param discovered
   * @return
   */
  @Modifying(clearAutomatically=true, flushAutomatically = true)
  @Query("update #{#entityName} r set r.reservationState = :reservationState, "
          + "r.discovered = :discovered "
          + "where r.id = :id")
  public int setReservationState(
          @Param("id") long id,
          @Param("reservationState") ReservationStateEnumType reservationState,
          @Param("discovered") long discovered);

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
}
