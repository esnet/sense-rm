package net.es.sense.rm.driver.nsi.db;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author hacksaw
 */
@Repository
public interface DeltaRepository extends CrudRepository<Delta, String> {

}
