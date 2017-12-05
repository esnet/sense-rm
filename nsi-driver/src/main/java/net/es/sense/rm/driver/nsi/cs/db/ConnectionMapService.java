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

  public ConnectionMap getByDescription(String description);

  public Collection<ConnectionMap> getByDeltaId(String deltaId);

  public Collection<ConnectionMap> getBySwitchingSubnetId(String switchingSubnetId);

  public Collection<ConnectionMap> getByDeltaIdAndSwitchingSubnetId(String deltaId, String switchingSubnetId);

}
