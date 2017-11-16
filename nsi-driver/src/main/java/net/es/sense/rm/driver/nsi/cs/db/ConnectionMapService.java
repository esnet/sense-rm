package net.es.sense.rm.driver.nsi.cs.db;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface ConnectionMapService {

  public ConnectionMap store(ConnectionMap reservation);

  public void delete(ConnectionMap reservation);

  public void delete(long id);

  public void delete();

  public Collection<ConnectionMap> get();

  public ConnectionMap get(long id);

  public Collection<ConnectionMap> getByGlobalReservationId(String globalReservationId);

  public ConnectionMap getBySwitchingSubnetId(String switchingSubnetId);

  public ConnectionMap getByGlobalReservationIdAndSwitchingSubnetId(String globalReservationId, String switchingSubnetId);

}
