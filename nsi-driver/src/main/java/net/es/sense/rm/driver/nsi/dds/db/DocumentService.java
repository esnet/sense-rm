/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.sense.rm.driver.nsi.dds.db;

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface DocumentService {

  Document create(Document document);

  void delete(Document document);

  void delete(String id);

  Collection<Document> get();

  public Document get(String id);

  Collection<Document> get(long lastDiscovered);

  Collection<Document> getByDocumentId(String documentId);

  Collection<Document> getByNsa(String nsa);

  Collection<Document> getByNsaAndType(String nsa, String type);

  Collection<Document> getByNsaAndTypeAndDocumentId(String nsa, String type, String documentId);

  Collection<Document> getByType(String type);

  Collection<Document> getByTypeAndDocumentId(String type, String documentId);

  Collection<Document> getExpired(long expires);

  Document update(Document document);

}
