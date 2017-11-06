package net.es.sense.rm.driver.nsi.cs.db;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface ReservationService {

  public Reservation store(Reservation reservation);

  public void delete(Reservation reservation);

  public void delete(long id);

  public void delete();

  public Collection<Reservation> get();

  public Collection<Reservation> getByTopologyId(String topologyId);

  public Reservation get(long id);

  public Collection<Reservation> get(String connectionId);

  public Reservation get(String providerNSA, String connectionId);

}
