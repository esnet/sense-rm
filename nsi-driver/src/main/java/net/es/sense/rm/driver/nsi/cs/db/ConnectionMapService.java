package net.es.sense.rm.driver.nsi.cs.db;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * This class defines the operations supported on the ConnectionMap repository.
 *
 * @author hacksaw
 */
@Service
public interface ConnectionMapService {

  public ConnectionMap store(ConnectionMap reservation);

  public void delete(ConnectionMap reservation);

  public void delete(long id);

  public void delete();

  public void deleteByUniqueId(@Param("uniqueId") String uniqueId);

  public Collection<ConnectionMap> get();

  public ConnectionMap get(long id);

  public ConnectionMap getByUniqueId(String uid);

  public Collection<ConnectionMap> getByDeltaId(String deltaId);

  public Collection<ConnectionMap> getBySwitchingSubnetId(String switchingSubnetId);

  public ConnectionMap getNewestBySwitchingSubnetId(String switchingSubnetId);

  public Collection<ConnectionMap> getByDeltaIdAndSwitchingSubnetId(String deltaId, String switchingSubnetId);

}
