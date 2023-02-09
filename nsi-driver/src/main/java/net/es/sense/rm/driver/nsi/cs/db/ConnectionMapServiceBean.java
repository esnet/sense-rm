package net.es.sense.rm.driver.nsi.cs.db;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Service
@Transactional(propagation=Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE, readOnly=true)
public class ConnectionMapServiceBean implements ConnectionMapService {

  @Autowired
  private ConnectionMapRepository connectionMapRepository;

  @Override
  public Collection<ConnectionMap> get() {
    return Lists.newArrayList(connectionMapRepository.findAll());
  }

  @Override
  public ConnectionMap get(long id) {
    return connectionMapRepository.findOneById(id);
  }

  @Override
  public ConnectionMap getByDescription(String description) {
    return connectionMapRepository.findByDescription(description);
  }

  @Override
  public Collection<ConnectionMap> getByDeltaId(String deltaId) {
    return Lists.newArrayList(connectionMapRepository.findByDeltaId(deltaId));
  }

  @Override
  public Collection<ConnectionMap> getBySwitchingSubnetId(String switchingSubnetId){
    return Lists.newArrayList(connectionMapRepository.findBySwitchingSubnetId(switchingSubnetId));
  }

  @Override
  public Collection<ConnectionMap> getByDeltaIdAndSwitchingSubnetId(String deltaId, String switchingSubnetId) {
    return Lists.newArrayList(connectionMapRepository.findByDeltaIdAndSwitchingSubnetId(deltaId, switchingSubnetId));
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public ConnectionMap store(ConnectionMap connectionMap) {
    if (Strings.isNullOrEmpty(connectionMap.getDescription())
            || Strings.isNullOrEmpty(connectionMap.getDeltaId())
            || Strings.isNullOrEmpty(connectionMap.getSwitchingSubnetId())) {
      return null;
    }

    ConnectionMap conn = connectionMapRepository.findByDescription(connectionMap.description);
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
    Optional.ofNullable(connectionMapRepository.findOneById(id)).ifPresent(conn -> {
      connectionMapRepository.delete(conn);
    });
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete() {
    for (ConnectionMap connectionMap : connectionMapRepository.findAll()) {
      connectionMapRepository.delete(connectionMap);
    }
  }
}
