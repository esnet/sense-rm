package net.es.sense.rm.driver.nsi.dds.db;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author hacksaw
 */
@Repository
public interface DocumentRepository extends CrudRepository<Document, String> {

  @Query("select d from #{#entityName} d where d.lastDiscovered > :lastDiscovered")
  public Iterable<Document> findNewer(@Param("lastDiscovered") long lastDiscovered);

  public Iterable<Document> findByNsa(String nsa);

  public Iterable<Document> findByType(String type);

  public Iterable<Document> findByDocumentId(String documentId);

  public Iterable<Document> findByNsaAndType(String nsa, String type);

  public Iterable<Document> findByNsaAndTypeAndDocumentId(String nsa, String type, String documentId);

  public Iterable<Document> findByTypeAndDocumentId(String type, String documentId);

  @Query("select d from #{#entityName} d where d.expires <= :expires")
  public Iterable<Document> findExpired(@Param("expires") long expires);

  @Query("select max(m.lastDiscovered) from #{#entityName} m")
  public Long findLastDiscovered();
}
