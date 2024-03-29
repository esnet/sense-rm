package net.es.sense.rm.measurements.db;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
@Repository
public interface MeasurementRepository extends CrudRepository<MeasurementResource, String> {

  public MeasurementResource findOneById(@Param("id") String id);

  //public void deleteById(@Param("id") String id);

  /**
   * Find all measurements newer than the specified lastModified time.
   *
   * @param lastModified
   * @return
   */
  @Query("select m from #{#entityName} m where m.generated > :lastModified order by m.generated desc")
  public Collection<MeasurementResource> findAllNewer(@Param("lastModified") long lastModified);

  public Collection<MeasurementResource> findAllByOrderByGeneratedAsc();

  public Collection<MeasurementResource> findLast10ByOrderByGeneratedAsc();

  public Collection<MeasurementResource> findFirst1ByOrderByGeneratedAsc();

  public Collection<MeasurementResource> findFirst1ByOrderByGeneratedDesc();

}
