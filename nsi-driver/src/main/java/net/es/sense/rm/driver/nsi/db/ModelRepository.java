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
public interface ModelRepository extends CrudRepository<Model, String> {

  @Query("select count(m) > 0 from #{#entityName} m where m.topologyId = :topologyId and m.version = :version")
  public boolean isVersion(@Param ("topologyId") String topologyId, @Param("version") long version);

  @Query("select m from #{#entityName} m where modelId = :modelId and m.version > :version")
  public Model findByModelIdAndVersion(@Param ("modelId") String modelId, @Param("version") long version);

  public Model findByModelId(String modelId);

  public Iterable<Model> findByTopologyId(String topologyId);

  @Query("select m from #{#entityName} m where m.version > :version")
  public Iterable<Model> findAllNewer(@Param("version") long version);

  @Query("select m from #{#entityName} m where modelId = :modelId and m.version > :version")
  public Model findModelIdNewerThanVersion(@Param ("modelId") String modelId, @Param("version") long version);

  @Query("select m from #{#entityName} m where m.topologyId = :topologyId and m.version = (select max(mm.version) from #{#entityName} mm where mm.topologyId = m.topologyId)")
  public Model findCurrentModelForTopologyId(@Param ("topologyId") String topologyId);

  @Query("select m from #{#entityName} m where topologyId = :topologyId and m.version > :version")
  public Iterable<Model> findTopologyIdNewerThanVersion(@Param ("topologyId") String topologyId, @Param("version") long version);

}
