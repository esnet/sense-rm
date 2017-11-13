package net.es.sense.rm.driver.nsi.db;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author hacksaw
 */
@Repository
public interface DeltaRepository extends CrudRepository<Delta, String> {

  public Delta deleteByDeltaId(@Param("deltaId") String deltaId);

  public Delta findByDeltaId(@Param("deltaId") String deltaId);

  public Delta findById(@Param("id") long id);

  @Query("select m from #{#entityName} m where deltaId = :deltaId and m.lastModified > :lastModified")
  public Delta findByDeltaId(@Param("deltaId") String deltaId, @Param("lastModified") long lastModified);

  @Query("select m from #{#entityName} m where deltaId = :deltaId and m.modelId = :modelId and m.lastModified > :lastModified")
  public Delta findByDeltaIdAndModelId(
          @Param("deltaId") String deltaId,
          @Param("modelId") String modelId,
          @Param("lastModified") long lastModified);

  @Query("select m from #{#entityName} m where m.lastModified > :lastModified")
  public Iterable<Delta> findAllNewer(@Param("lastModified") long lastModified);
}
