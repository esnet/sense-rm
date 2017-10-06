/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.lib.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author hacksaw
 */
public class ContentType {
    public final static String XGZIP = "application/x-gzip";
    public final static String XML = "application/xml";
    public final static String TEXT = "text/plain";

    public static InputStream decode(String contentType, InputStream is) throws IOException {
        if (XGZIP.equalsIgnoreCase(contentType)) {
            return new GZIPInputStream(is);
        }
        else {
            return is;
        }
    }

    public static byte[] decode2ByteArray(String contentType, InputStream is) throws IOException {
        if (XGZIP.equalsIgnoreCase(contentType)) {
            return IOUtils.toByteArray(new GZIPInputStream(is));
        }
        else {
            return IOUtils.toByteArray(is);
        }
    }

    public static String decode2String(String contentType, InputStream is) throws IOException {
        if (XGZIP.equalsIgnoreCase(contentType)) {
            return IOUtils.toString(new GZIPInputStream(is), Charset.defaultCharset());
        }
        else {
            return IOUtils.toString(is, Charset.defaultCharset());
        }
    }

    public static OutputStream encode(String contentType, OutputStream os) throws IOException {
        if (XGZIP.equalsIgnoreCase(contentType)) {
            return new GZIPOutputStream(os);
        }
        else {
            return os;
        }
    }
}
