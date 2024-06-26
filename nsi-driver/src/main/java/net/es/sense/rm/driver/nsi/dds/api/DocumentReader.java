package net.es.sense.rm.driver.nsi.dds.api;

import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.common.util.Decoder;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.dds.lib.jaxb.dds.ContentType;
import net.es.nsi.dds.lib.jaxb.nml.NmlTopologyType;
import net.es.nsi.dds.lib.jaxb.nsa.NsaType;
import net.es.sense.rm.driver.nsi.dds.DdsProvider;
import net.es.sense.rm.driver.nsi.dds.db.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Service
public class DocumentReader {
  private final DdsProvider ddsProvider;

  @Autowired
  public DocumentReader(DdsProvider ddsProvider) {
    this.ddsProvider = ddsProvider;
  }

  public long getLastDiscovered() {
    return ddsProvider.getLastDiscovered();
  }

  public Collection<NsaType> getNsaAll() {
    return decode(NsaType.class, ddsProvider.getDocumentsByType(Nsi.NSI_DOC_TYPE_NSA_V1));
  }

  public Collection<NsaType> getNsaById(String id) {
    return decode(NsaType.class, ddsProvider.getDocumentsByTypeAndId(Nsi.NSI_DOC_TYPE_NSA_V1, id));
  }

  public NsaType getNsa(String id) {
    return decodeNSA(ddsProvider.getDocument(id, Nsi.NSI_DOC_TYPE_NSA_V1, id));
  }

  public Collection<NmlTopologyType> getNmlTopologyAll() {
    return decode(NmlTopologyType.class, ddsProvider.getDocumentsByType(Nsi.NSI_DOC_TYPE_TOPOLOGY_V2));
  }

  public Collection<NmlTopologyType> getTopologyById(String id) {
    return decode(NmlTopologyType.class, ddsProvider.getDocumentsByTypeAndId(Nsi.NSI_DOC_TYPE_TOPOLOGY_V2, id));
  }

  private <T> Collection<T> decode(Class<T> xmlClass, Iterable<Document> documents) {
    Collection<T> list = new ArrayList<>();
    for (Document doc : documents) {
      try {
        ContentType content = doc.getDocumentFull().getContent();
        InputStream is = Decoder.decode(content.getContentTransferEncoding(), content.getContentType(),
                content.getValue());
        list.add(XmlUtilities.xmlToJaxb(xmlClass, is));
      } catch (IOException | JAXBException ex) {
        log.error("[DocumentReader] could not decode document id = {}", doc.getId(), ex);
      }
    }

    return list;
  }

  private NsaType decodeNSA(Document document) {
      try {
        ContentType content = document.getDocumentFull().getContent();
        InputStream is = Decoder.decode(content.getContentTransferEncoding(), content.getContentType(),
            content.getValue());
        return XmlUtilities.xmlToJaxb(NsaType.class, is);
      } catch (IOException | JAXBException ex) {
        log.error("[DocumentReader] could not decode NSA document id = {}", document.getId(), ex);
      }

      return null;
  }
}
