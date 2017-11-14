package net.es.sense.rm.driver.nsi.cs.db;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Service
@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
public class ConnectionMapServiceBean implements ConnectionMapService {

  @Autowired
  private ConnectionMapRepository connectionMapRepository;

  @Override
  public Collection<ConnectionMap> get() {
    return Lists.newArrayList(connectionMapRepository.findAll());
  }

  @Override
  public ConnectionMap get(long id) {
    return connectionMapRepository.findOne(id);
  }

  @Override
  public ConnectionMap getByGlobalReservationId(String globalReservationId) {
    return connectionMapRepository.findByGlobalReservationId(globalReservationId);
  }

  @Override
  public ConnectionMap getSwitchingSubnetId(String switchingSubnetId){
    return connectionMapRepository.findBySwitchingSubnetId(switchingSubnetId);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public ConnectionMap store(ConnectionMap connectionMap) {
    if (Strings.isNullOrEmpty(connectionMap.getGlobalReservationId())
            || Strings.isNullOrEmpty(connectionMap.getSwitchingSubnetId())) {
      return null;
    }

    ConnectionMap conn = connectionMapRepository.findByGlobalReservationId(connectionMap.globalReservationId);
    if (conn != null) {
      connectionMap.setId(conn.getId());
    }

    return connectionMapRepository.save(connectionMap);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(ConnectionMap connectionMap) {
    connectionMapRepository.delete(connectionMap);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(long id) {
    connectionMapRepository.delete(id);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete() {
    for (ConnectionMap connectionMap : connectionMapRepository.findAll()) {
      connectionMapRepository.delete(connectionMap);
    }
  }
}
