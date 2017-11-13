package net.es.nsi.common.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
    } catch (URISyntaxException ex) {
      log.debug("isAbsolute: invalid URI " + uri);
    }

    return false;
  }

  public static String append(String base, String postfix) throws MalformedURLException {
    URL baseURL = new URL(base);
    URL result = new URL(baseURL.getProtocol(), baseURL.getHost(), baseURL.getPort(), baseURL.getFile() +"/"+ postfix, null);
    return result.toExternalForm();
  }
}
