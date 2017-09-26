package net.es.nsi.dds.lib.util;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.mail.MessagingException;
import javax.xml.parsers.ParserConfigurationException;
import net.es.nsi.dds.lib.jaxb.DomParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author hacksaw
 */
public class Decoder {
    private final static Logger log = LoggerFactory.getLogger(Decoder.class);

    public static InputStream decode(String contentTransferEncoding,
            String contentType, String source) throws IOException  {
        if (Strings.isNullOrEmpty(contentTransferEncoding)) {
            contentTransferEncoding = ContentTransferEncoding._7BIT;
        }

        if (Strings.isNullOrEmpty(contentType)) {
            contentType = ContentType.TEXT;
        }

        try (InputStream ctes = ContentTransferEncoding.decode(contentTransferEncoding, source)) {
            return ContentType.decode(contentType, ctes);
        } catch (UnsupportedEncodingException | MessagingException ex) {
            log.error("decode2Dom: failed to parse document", ex);
            throw new IOException(ex.getMessage(), ex.getCause());
        }
    }

    public static Document decode2dom(String contentTransferEncoding,
            String contentType, String source) throws IOException  {

        try (InputStream dis = decode(contentTransferEncoding, contentType, source)) {
            return DomParser.xml2Dom(dis);
        } catch (IOException | ParserConfigurationException | SAXException ex) {
            log.error("decode2Dom: failed to parse document", ex);
            throw new IOException(ex.getMessage(), ex.getCause());
        }
    }

    public static String decode2string(String contentTransferEncoding,
            String contentType, String source) throws IOException {

        try (InputStream dis = decode(contentTransferEncoding, contentType, source)) {
            return ContentType.decode2String(contentType, dis);
        }
    }

    public static byte[] decode2ByteArray(String contentTransferEncoding,
            String contentType, String source) throws IOException {

        try (InputStream dis = decode(contentTransferEncoding, contentType, source)) {
            return ContentType.decode2ByteArray(contentType, dis);
        }
    }
}
