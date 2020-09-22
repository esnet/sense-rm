package net.es.sense.rm.driver.nsi.cs.db;

import java.util.Collection;
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
          @Param ("providerNSA") String providerNsa,
          @Param("connectionId") String connectionId);

  /**
   *
   * @param providerNsa
   * @return
   */
  public Collection<Reservation> findByProviderNsa(
          @Param ("providerNSA") String providerNsa);

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

  public Collection<Reservation> findFirst1ByOrderByDiscoveredDesc();


  /**
   * Find all reservations newer than the specified lastModified time.
   *
   * @param lastModified
   * @return
   */
  @Query("select m from #{#entityName} m where m.discovered > :lastModified")
  public Collection<Reservation> findAllNewer(@Param("lastModified") long lastModified);
}
