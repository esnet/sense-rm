/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */

package net.es.sense.rm.api.common;

import com.google.common.base.Strings;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author hacksaw
 */
public class Utilities {

  // Transform patterns.
  private static final Pattern PATTERN_FROM = Pattern.compile("\\(.*?\\|");
  private static final Pattern PATTERN_TO = Pattern.compile("\\|.*?\\)");
  private static final Pattern PATTERN_URI = Pattern.compile(
          "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

  private final String uriTransform;

  /**
   *
   * @param uriTransform
   */
  public Utilities(String uriTransform) {
    this.uriTransform = uriTransform;
  }

  /**
   *
   * @param uri
   * @return
   * @throws java.net.MalformedURLException
   */
  public UriComponentsBuilder getPath(String uri)
          throws MalformedURLException {
    if (Strings.isNullOrEmpty(uriTransform)) {
      return UriComponentsBuilder.fromHttpUrl(uri);
    }

    // We want to manipulate the URL using string matching.
    String fromUri = getFromURI(uriTransform);
    String toUri = getToURI(uriTransform);

    // Remove the URI prefix if one was provided.
    if (!Strings.isNullOrEmpty(fromUri)) {
      if (!uri.startsWith(fromUri)) {
        // We do not have a matching URI prefix so return full URL.
        return UriComponentsBuilder.fromHttpUrl(uri);
      }

      uri = uri.replaceFirst(fromUri, "");
    }

    // Add the URI prefix if one was provided.
    if (!Strings.isNullOrEmpty(toUri)) {
      uri = toUri + uri;
    }

    return UriComponentsBuilder.fromHttpUrl(uri);
  }

  /**
   * Get the fromURI component from the specified transform.
   *
   * @param transform Transform from which to extract the fromURI.
   * @return
   */
  private String getFromURI(String transform) {
    if (transform == null) {
      return transform;
    }
    return getUri(PATTERN_FROM, transform);
  }

  /**
   * Get the toURI component from the specified transform.
   *
   * @param transform Transform from which to extract the toURI.
   * @return
   */
  private String getToURI(String transform) {
    if (transform == null) {
      return transform;
    }
    return getUri(PATTERN_TO, transform);
  }

  /**
   * Get the URI from the specified transform using the provided pattern.
   *
   * @param pattern Regex pattern to apply to transform.
   * @param transform Transform requiring URI extraction.
   * @return The extracted URI.
   */
  private String getUri(Pattern pattern, String transform) {
    if (transform == null) {
      return transform;
    }
    Matcher matcher = pattern.matcher(transform);

    if (matcher.find()) {
      String uri = matcher.group().subSequence(1, matcher.group().length() - 1).toString().trim();
      Matcher matcherUri = PATTERN_URI.matcher(uri);
      if (matcherUri.find()) {
        return matcherUri.group();
      }
    }

    return null;
  }
}
