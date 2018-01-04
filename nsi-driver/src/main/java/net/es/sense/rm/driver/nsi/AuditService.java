package net.es.sense.rm.driver.nsi;

/**
 *
 * @author hacksaw
 */
public interface AuditService {

  void audit();

  void audit(String topologyId);

}
