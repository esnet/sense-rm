package net.es.sense.rm.driver.nsi.dds.db;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface DocumentService {
  public long getLastDiscovered();

  public Document create(Document document);

  public void delete(Document document);

  public void delete(String id);

  public Collection<Document> get();

  public Document get(String id);

  public Collection<Document> get(long lastDiscovered);

  public Collection<Document> getByDocumentId(String documentId);

  public Collection<Document> getByNsa(String nsa);

  public Collection<Document> getByNsaAndType(String nsa, String type);

  public Collection<Document> getByNsaAndTypeAndDocumentId(String nsa, String type, String documentId);

  public Collection<Document> getByType(String type);

  public Collection<Document> getByTypeAndDocumentId(String type, String documentId);

  public Collection<Document> getExpired(long expires);

  public Document update(Document document);

}
