package net.es.sense.rm.driver.nsi.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
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
  public boolean isVersion(@Param ("topologyId") String topologyId, @Param("version") String version);

  //@Query("select m from #{#entityName} m where modelId = :modelId and m.version > :version")
  //public Model findByModelIdAndVersion(@Param ("modelId") String modelId, @Param("version") long version);

  public Model findByIdx(@Param("idx") long idx);

  @Modifying
  //@Query("delete from #{#entityName} m where m.idx = :idx")
  public void deleteByIdx(@Param("idx") long idx);

  @Modifying
  //@Query("delete from #{#entityName} m where m.idx = :idx")
  public void deleteByModelId(@Param("modelId") String modelId);

  public Model findByModelId(@Param("modelId") String modelId);

  public Iterable<Model> findByTopologyId(String topologyId);
  public Page<Model> findByTopologyId(String topologyId, Pageable pageable);

  public Long countByTopologyId(@Param("topologyId") String topologyId);

  @Query("select m from #{#entityName} m where m.created > :created")
  public Iterable<Model> findAllNewer(@Param("created") long created);

  @Query("select m from #{#entityName} m where m.created < :created")
  public Iterable<Model> findAllOlder(@Param("created") long created);

  @Query("select m from #{#entityName} m where m.modelId = :modelId and m.created > :created")
  public Model findModelIdNewerThanCreated(@Param ("modelId") String modelId, @Param("created") long created);

  @Query("select m from #{#entityName} m where m.topologyId = :topologyId and m.created = (select max(mm.created) from #{#entityName} mm where mm.topologyId = m.topologyId)")
  public Model findCurrentModelForTopologyId(@Param ("topologyId") String topologyId);

  @Query("select m from #{#entityName} m where m.topologyId = :topologyId and m.created > :created")
  public Iterable<Model> findTopologyIdNewerThanCreated(@Param ("topologyId") String topologyId, @Param("created") long created);

  @Query("select m from #{#entityName} m where m.topologyId = :topologyId and m.created < :created")
  public Iterable<Model> findTopologyIdOlderThanCreated(@Param ("topologyId") String topologyId, @Param("created") long created);

  @Modifying
  @Query("delete from #{#entityName} m where m.topologyId = :topologyId and m.created < :created")
  public void deleteByTopologyIdAndLessThanCreated(@Param ("topologyId") String topologyId, @Param("created") long created);

  Page<Model> findAll(Pageable pageable);
}
