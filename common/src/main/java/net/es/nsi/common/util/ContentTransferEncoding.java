package net.es.nsi.common.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author hacksaw
 */
public class ContentTransferEncoding {
    public final static String BASE64 = "base64";
    public final static String QUOTEDPRINTABLE = "quoted-printable";
    public final static String _8BIT = "8bit";
    public final static String _7BIT = "7bit";
    public final static String BINARY = "binary";
    public final static String UUENCODE = "uuencode";

    public static InputStream decode(String contentTransferEncoding, String source) throws UnsupportedEncodingException, MessagingException {
        return MimeUtility.decode(new ByteArrayInputStream(source.getBytes()), contentTransferEncoding);
    }

    public static InputStream decode(String contentTransferEncoding, InputStream source) throws UnsupportedEncodingException, MessagingException {
        return MimeUtility.decode(source, contentTransferEncoding);
    }

    public static byte[] decode2ByteArray(String contentTransferEncoding, String source) throws UnsupportedEncodingException, MessagingException, IOException {
        return IOUtils.toByteArray(MimeUtility.decode(new ByteArrayInputStream(source.getBytes()), contentTransferEncoding));
    }

    public static byte[] decode2ByteArray(String contentTransferEncoding, InputStream source) throws UnsupportedEncodingException, MessagingException, IOException {
        return IOUtils.toByteArray(MimeUtility.decode(source, contentTransferEncoding));
    }

    public static OutputStream encode(String contentTransferEncoding, OutputStream os) throws MessagingException {
        return MimeUtility.encode(os, contentTransferEncoding);
    }
}
