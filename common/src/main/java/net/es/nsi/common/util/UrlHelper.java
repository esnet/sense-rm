package net.es.nsi.common.util;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class UrlHelper {

  /**
   * Returns true if the specified URI is absolute, and false otherwise.
   *
   * @param uri
   * @return
   */
  public static boolean isAbsolute(String uri) {
    try {
      final URI u = new URI(uri);
      if (u.isAbsolute()) {
        return true;
      }
    } catch (Exception ex) {
      log.debug("isAbsolute: invalid URI " + uri);
    }

    return false;
  }
}
