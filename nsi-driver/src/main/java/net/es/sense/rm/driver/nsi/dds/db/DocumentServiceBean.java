package net.es.sense.rm.driver.nsi.dds.db;

import com.google.common.collect.Lists;
import java.util.Collection;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author hacksaw
 */
@Service
@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
public class DocumentServiceBean implements DocumentService {

  @Autowired
  private DocumentRepository documentRepository;

  @Override
  public Collection<Document> get() {
    return Lists.newArrayList(documentRepository.findAll());
  }

  @Override
  public Document get(String id) {
    if (Strings.isNullOrEmpty(id)) {
      return null;
    }
    return documentRepository.findOne(id);
  }

  @Override
  public Collection<Document> get(long lastDiscovered) {
    return Lists.newArrayList(documentRepository.findNewer(lastDiscovered));
  }

  @Override
  public Collection<Document> getByNsa(String nsa) {
    if (Strings.isNullOrEmpty(nsa)) {
      return null;
    }
    return Lists.newArrayList(documentRepository.findByNsa(nsa));
  }

  @Override
  public Collection<Document> getByType(String type) {
    if (Strings.isNullOrEmpty(type)) {
      return null;
    }

    return Lists.newArrayList(documentRepository.findByType(type));
  }

  @Override
  public Collection<Document> getByDocumentId(String documentId) {
    if (Strings.isNullOrEmpty(documentId)) {
      return null;
    }

    return Lists.newArrayList(documentRepository.findByDocumentId(documentId));
  }

  @Override
  public Collection<Document> getByNsaAndType(String nsa, String type) {
    if (Strings.isNullOrEmpty(nsa) || Strings.isNullOrEmpty(type)) {
      return null;
    }

    return Lists.newArrayList(documentRepository.findByNsaAndType(nsa, type));
  }

  @Override
  public Collection<Document> getByNsaAndTypeAndDocumentId(String nsa, String type, String documentId) {
    if (Strings.isNullOrEmpty(nsa) || Strings.isNullOrEmpty(type) || Strings.isNullOrEmpty(documentId)) {
      return null;
    }

    return Lists.newArrayList(documentRepository.findByNsaAndTypeAndDocumentId(nsa, type, documentId));
  }

  @Override
  public Collection<Document> getByTypeAndDocumentId(String type, String documentId) {
    if (Strings.isNullOrEmpty(type) || Strings.isNullOrEmpty(documentId)) {
      return null;
    }

    return Lists.newArrayList(documentRepository.findByTypeAndDocumentId(type, documentId));
  }


  @Override
  public Collection<Document> getExpired(long expires) {
    return Lists.newArrayList(documentRepository.findExpired(expires));
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  @Override
  public Document create(Document document) {
    if (Strings.isNullOrEmpty(document.getId())) {
      return null;
    }
    return documentRepository.save(document);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  @Override
  public Document update(Document document) {
    Document findOne = documentRepository.findOne(document.getId());
    if (findOne == null) {
      return null;
    }
    return documentRepository.save(document);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  @Override
  public void delete(Document document) {
    documentRepository.delete(document);
  }

  @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
  @Override
  public void delete(String id) {
    documentRepository.delete(id);
  }
}
