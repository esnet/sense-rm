package net.es.sense.rm.driver.nsi.cs.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author hacksaw
 */
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Data
@Entity
@Table(name = "connectionmap")
public class ConnectionMap implements Serializable {
  @Id
  @GeneratedValue
  long id;

  long lastAudit = 0;

  @Basic(optional=false)
  String description;

  @Basic(optional=false)
  String deltaId;

  @Basic(optional=false)
  String switchingSubnetId;

  @ElementCollection(fetch=FetchType.EAGER, targetClass=StpMapping.class)
  List<StpMapping> map = new ArrayList<>();

  public Optional<StpMapping> findMapping(String stpId) {
    return map.stream().filter(s -> s.getStpId().equalsIgnoreCase(stpId)).findFirst();
  }
}
