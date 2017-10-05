package net.es.sense.rm.api.common;

import com.google.common.base.Strings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class Encoder {
  public static String encode(String doc) throws IOException {
    if (Strings.isNullOrEmpty(doc)) {
      return null;
    }

    byte[] compressed = compress(doc.getBytes(Charset.forName("UTF-8")));
    String encoded = Base64.getEncoder().encodeToString(compressed);
    return encoded;
  }

  private static byte[] compress(byte[] source) throws IOException {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream(source.length)) {
      return gzip(os, source).toByteArray();
    } catch (IOException io) {
      log.error("Failed to compress source", io);
      throw io;
    }
  }

  private static ByteArrayOutputStream gzip(ByteArrayOutputStream os, byte[] source) throws IOException {
    try (GZIPOutputStream gos = new GZIPOutputStream(os)) {
      gos.write(source);
      return os;
    } catch (IOException io) {
      log.error("Failed to gzip source stream", io);
      throw io;
    }
  }
}