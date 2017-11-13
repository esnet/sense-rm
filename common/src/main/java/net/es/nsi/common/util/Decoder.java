package net.es.nsi.common.util;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import javax.mail.MessagingException;
import javax.xml.parsers.ParserConfigurationException;
import net.es.nsi.common.jaxb.DomParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author hacksaw
 */
public class Decoder {

  private final static Logger LOG = LoggerFactory.getLogger(Decoder.class);

  public static String decode(String doc) throws IOException {
    if (Strings.isNullOrEmpty(doc)) {
      return null;
    }

    return decode2string(ContentTransferEncoding.BASE64, ContentType.XGZIP, doc);
  }

  public static InputStream decode(String contentTransferEncoding,
          String contentType, String source) throws IOException {
    if (Strings.isNullOrEmpty(contentTransferEncoding)) {
      contentTransferEncoding = ContentTransferEncoding._7BIT;
    }

    if (Strings.isNullOrEmpty(contentType)) {
      contentType = ContentType.TEXT;
    }

    try (InputStream ctes = ContentTransferEncoding.decode(contentTransferEncoding, source)) {
      return ContentType.decode(contentType, ctes);
    } catch (UnsupportedEncodingException | MessagingException ex) {
      LOG.error("decode2Dom: failed to parse document", ex);
      throw new IOException(ex.getMessage(), ex.getCause());
    }
  }

  public static Document decode2dom(String contentTransferEncoding,
          String contentType, String source) throws IOException {

    try (InputStream dis = decode(contentTransferEncoding, contentType, source)) {
      return DomParser.xml2Dom(dis);
    } catch (IOException | ParserConfigurationException | SAXException ex) {
      LOG.error("decode2Dom: failed to parse document", ex);
      throw new IOException(ex.getMessage(), ex.getCause());
    }
  }

  public static String decode2string(String contentTransferEncoding,
          String contentType, String source) throws IOException {

    try (InputStream dis = decode(contentTransferEncoding, contentType, source)) {
      return IOUtils.toString(dis, Charset.forName("UTF-8"));
    }
  }

  public static byte[] decode2ByteArray(String contentTransferEncoding,
          String contentType, String source) throws IOException {

    try (InputStream dis = decode(contentTransferEncoding, contentType, source)) {
      return ContentType.decode2ByteArray(contentType, dis);
    }
  }
}
