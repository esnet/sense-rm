package net.es.sense.rm.driver.nsi.dds.db;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.util.XmlUtilities;
import net.es.nsi.dds.lib.jaxb.dds.ContentType;
import net.es.nsi.dds.lib.jaxb.dds.DocumentType;
import net.es.nsi.dds.lib.jaxb.dds.ObjectFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { DocumentService.class, DocumentServiceBean.class, DocumentRepository.class, Document.class, DbUnitTestConfiguration.class })
@DataJpaTest
@ActiveProfiles("test")
public class DocumentServiceTest {
  @Autowired
  private DocumentService documentService;

  @Autowired
  private DocumentRepository documents;

  private final ObjectFactory FACTORY = new ObjectFactory();

  private Document d1;
  private Document d2;
  private Document d3;

  private static long initDate(long value) {
    Date date = new Date();
    date.setTime(value);
    return date.getTime();
  }

  public void buildDatabase() throws JAXBException, IOException, DatatypeConfigurationException {
    // Clear the contents of the database so he have a fresh start.
    documents.deleteAll();

    DocumentType document = FACTORY.createDocumentType();
    document.setVersion(XmlUtilities.xmlGregorianCalendar("2017-09-06T10:15:13.096Z"));
    document.setExpires(XmlUtilities.xmlGregorianCalendar("2017-11-05T10:15:13.097Z"));
    document.setType("vnd.ogf.nsi.topology.v2+xml");
    document.setNsa("urn:ogf:network:es.net:2013:nsa");
    document.setId("urn:ogf:network:es.net:2013:");
    document.setHref("https://nsi-aggr-west.es.net/discovery/documents/urn%3Aogf%3Anetwork%3Aes.net%3A2013%3Ansa/vnd.ogf.nsi.topology.v2%2Bxml/urn%3Aogf%3Anetwork%3Aes.net%3A2013%3A");
    ContentType content = FACTORY.createContentType();
    content.setContentTransferEncoding("base64");
    content.setContentType("application/x-gzip");
    content.setValue("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");
    document.setContent(content);
    d1 = new Document(document);
    documents.save(d1);

    document = FACTORY.createDocumentType();
    document.setVersion(XmlUtilities.xmlGregorianCalendar("2017-09-06T10:15:05.536Z"));
    document.setExpires(XmlUtilities.xmlGregorianCalendar("2017-11-05T10:15:05.536Z"));
    document.setType("vnd.ogf.nsi.nsa.v1+xml");
    document.setNsa("urn:ogf:network:es.net:2013:nsa");
    document.setId("urn:ogf:network:es.net:2013:nsa");
    document.setHref("https://nsi-aggr-west.es.net/discovery/documents/urn%3Aogf%3Anetwork%3Aes.net%3A2013%3Ansa/vnd.ogf.nsi.nsa.v1%2Bxml/urn%3Aogf%3Anetwork%3Aes.net%3A2013%3Ansa");
    content = FACTORY.createContentType();
    content.setContentTransferEncoding("base64");
    content.setContentType("application/x-gzip");
    content.setValue("H4sIAAAAAAAAAH1U0W6bMBT9FcTrBDYQ0gQZuq7apr5s05JtUt9cY4hVsJFtaPv3uxhIskTrQ5SL7znn+vhem9y+to03cG2Ekrkfhdj3uGSqFLLO/V/7L8HGvy2INEkmDfUALA1ESe4frO0yhAw78JaaUNVVqHSNpBEoxtEK4RiVwjAF2m+wSv0jOc79XstMcFtlHdW0NRmkIJENjOoyWLlNvHZCc5P7IHYTRFGA032EsyjNcBqmyfrR90Q5CUHpTHL7ovRzxk0IYQYkt2H/ZM3p4G2A1xc64I62vPi8A6L3fXd/93Pn9T/uCHLLxKjKvlDNf09CBRh80qKseTBEBF1mibFU270A5v8KAumIIbRshbxX0lJmx2OOpzOYwl4sgRbF6LSHlYxtWPn0tMFBSdl4MjwJtiyiAd5gHGPMtnS9hs3PtDlalDoNrZ1jy1/tbDtYjH/bPXifnL2J6DBTeE7VfJg1wAXYabvR7gqnUbKPtmm8Sh5n/jE/fR+Jz0KebwO+xCDKnjZXdU/ISp4x7g9Cel979XxFWGDzn4GTGzt5wi4rLl2LgUunNiWn7ymeFWDARXNWmgH44zRpV8VnLDprJfq3yY1i1LpZaZSshe1LXgRRHIdxiuOEoNMqaQDoouQm3NystzFkl6URuAjN0/9QFu/dB9jUEUeEtFxXlIGQfet4QbuuEZMeGmTp7jOMeshMOMQh9B66w/UHo2hHkGOQg+ZVMT4DBt4BZcCsCad6CMxKzkaxHdeDYDBNDk3QWd2KU9tr7o1quX9dVKuGh3ATfXSJhYfmAstU2wp3p1Rv/SLBmKCZU5COw+38I+zBGyVzHyTfPSh4OOAnAlrXOniB+SXoKOFa6yDFX51bkHc8BQAA");
    document.setContent(content);
    d2 = new Document(document);
    documents.save(d2);

    document = FACTORY.createDocumentType();
    document.setVersion(XmlUtilities.xmlGregorianCalendar("2017-06-19T12:54:16+02:00"));
    document.setExpires(XmlUtilities.xmlGregorianCalendar("2018-06-19T13:12:59.026+02:00"));
    document.setType("vnd.ogf.nsi.nsa.v1+xml");
    document.setNsa("urn:ogf:network:netherlight.net:2013:nsa:bod");
    document.setId("urn:ogf:network:netherlight.net:2013:nsa:bod");
    document.setHref("https://nsi-aggr-west.es.net/discovery/documents/urn%3Aogf%3Anetwork%3Anetherlight.net%3A2013%3Ansa%3Abod/vnd.ogf.nsi.nsa.v1%2Bxml/urn%3Aogf%3Anetwork%3Anetherlight.net%3A2013%3Ansa%3Abod");
    content = FACTORY.createContentType();
    content.setContentTransferEncoding("base64");
    content.setContentType("application/x-gzip");
    content.setValue("H4sIAAAAAAAAAJVUwY6bMBD9lYgeo9jgQHbXclhVlapWqqoe0h56c8EQa8FGtmGTv+9gMMluWnV78jAz780bzxj2eGqb1SCMlVrtowTF0UqoQpdS1fvo++Hj5j56zJmyKVWWryBZWbDSfXR0rqMY2+IoWm6RriukTY2VlZjESYpjgktpCw3cZ/DyaAGTfdQbRaVwFe244a2lEIIAHQpuyk06igjJ26WS0qbskRIOK94K2/FC2LHUFicE18r+0iVoP3XSCLuPIHC/iXeb5OGQbGlCaPaAYrJbx4TGQC/LSQTIpkD5rM3TeB6FaWR9dGMZOnKPXVPPvNwRuO8CM9CmNAmscE+gLP/qeb6MPKvO6LIvHACZl50zqyv3zI34MfHlBN0hkjH82s+s48YdJGD+WhFQSxLjZSvVB60cL9w4MTJd52T2MhhG5uOFWrhR6Au9atrPbyAYdA+yFObdNSu0MDPMViAduwy2EyeX295UwLaBCsnrEhPWp03mNdqIYaaBnqC5tvPdxzvfe5Ymu58zfolP3wvwSaprJfAloZOeNzd1L5mVukJ84squDka3nTC3YkPqfECjfqwv84PXp9RyEMqzTsHpe7LDOU8Kv5xhowvu/C40WtXS9aXIU/SQpdk9LMzFxxpI81ZG0Dbb3UEweMa8wDIv+ucyf9PqX3YXCC9YJpUTpoLnlzN37kTOu66RUw08qNL/CmCNkNOdbnR9RgNZw3Nm2GezoxHVv1ZwE7D4hQgPZfhKwFu1FBZUoLDVa6t59z96rp/En2RUgrveiNXIuI9uCxvdCNR/ex9hP3D/M81/A9jzFMh8BQAA");
    document.setContent(content);
    d3 = new Document(document);
    documents.save(d3);
  }

  @Test
  public void verify() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();

    Collection<Document> docs = documentService.get();

    // Check document count.
    Assert.assertEquals(3, docs.size());

    // Verify the first document exists.
    String id = Document.documentId(d1.getDocumentFull());
    Document findOne = documentService.get(id);
    Assert.assertEquals(id, findOne.getId());

    // Verify the document contents were not truncated.
    Assert.assertEquals(d1.getDocumentFull().getContent().getValue(), findOne.getDocumentFull().getContent().getValue());

    // Verify the second document exists.
    id = Document.documentId(d2.getDocumentFull());
    findOne = documentService.get(id);
    Assert.assertEquals(id, findOne.getId());

    // Verify the document contents were not truncated.
    Assert.assertEquals(d2.getDocumentFull().getContent().getValue(), findOne.getDocumentFull().getContent().getValue());
  }

  @Test
  public void update() throws DatatypeConfigurationException, JAXBException, IOException {
    // Set up test data.
    buildDatabase();

    String id = Document.documentId(d1.getDocumentFull());
    Document findOne = documentService.get(id);

    long now = System.currentTimeMillis();
    Assert.assertNotEquals(now, findOne.getLastDiscovered());

    findOne.setLastDiscovered(now);
    DocumentType documentT = findOne.getDocumentFull();
    XMLGregorianCalendar currentDate = XmlUtilities.longToXMLGregorianCalendar(now);
    Assert.assertNotEquals(currentDate, documentT.getVersion());

    documentT.setVersion(currentDate);
    findOne.setDocumentType(documentT);
    documentService.update(findOne);

    findOne = documentService.get(id);
    Assert.assertEquals(now, findOne.getLastDiscovered());
    documentT = findOne.getDocumentFull();
    Assert.assertEquals(currentDate, documentT.getVersion());
  }

  @Test
  public void getNewer() throws DatatypeConfigurationException, JAXBException, IOException {
    // Set up test data.
    buildDatabase();

    long searchTime = System.currentTimeMillis();
    Collection<Document> findNewer = documentService.get(searchTime);
    Assert.assertEquals(0, findNewer.size());

    String id = Document.documentId(d1.getDocumentFull());
    Document findOne = documents.findOne(id);

    long now = System.currentTimeMillis();
    findOne.setLastDiscovered(now);
    documents.save(findOne);
    findNewer = documentService.get(searchTime);
    Assert.assertEquals(1, findNewer.size());
  }

  @Test
  public void getByNsa() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();

    Collection<Document> results = documentService.getByNsa("urn:ogf:network:es.net:2013:nsa");
    Assert.assertEquals(2, results.size());

    results = documentService.getByNsa("urn:ogf:network:poopie.net:2013:poop");
    Assert.assertEquals(0, results.size());
  }

  @Test
  public void getByType() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();

    Collection<Document> results = documentService.getByType("vnd.ogf.nsi.topology.v2+xml");
    Assert.assertEquals(1, results.size());

    results = documentService.getByType("vnd.ogf.nsi.nsa.v1+xml");
    Assert.assertEquals(2, results.size());

    results = documentService.getByNsa("vnd.ogf.nsi.poop.v1+xml");
    Assert.assertEquals(0, results.size());
  }

  @Test
  public void getExpired() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();
    Date expires = XmlUtilities.xmlGregorianCalendarToDate(XmlUtilities.xmlGregorianCalendar("2017-12-05T10:15:05.536Z"));
    Collection<Document> expired = documentService.getExpired(expires.getTime());
    Assert.assertEquals(2, expired.size());

    for (Document document : expired) {
      documentService.delete(document.getId());
    }

    expired = documentService.getExpired(expires.getTime());
    Assert.assertEquals(0, expired.size());

    expired = documentService.get();
    Assert.assertEquals(1, expired.size());
  }

  @Test
  public void getByNsaAndType() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();

    Collection<Document> results = documentService.getByNsaAndType("urn:ogf:network:es.net:2013:nsa", "vnd.ogf.nsi.topology.v2+xml");
    Assert.assertEquals(1, results.size());
  }

  @Test
  public void getByNsaAndTypeAndDocumentId() throws JAXBException, IOException, DatatypeConfigurationException {
    // Set up test data.
    buildDatabase();

    Collection<Document> results = documentService.getByNsaAndTypeAndDocumentId("urn:ogf:network:es.net:2013:nsa", "vnd.ogf.nsi.topology.v2+xml", "urn:ogf:network:es.net:2013:");
    Assert.assertEquals(1, results.size());
  }

}
