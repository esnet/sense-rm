/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.sense.rm.api;

import com.google.common.base.Strings;
import jakarta.ws.rs.core.Response;
import net.es.sense.rm.driver.api.ResourceResponse;
import org.apache.http.client.utils.DateUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.Objects;

/**
 * Contains common HTTP message manipulation methods for controllers.
 *
 * @author hacksaw
 */
public class Common {
  /**
   * Format a response entity.
   *
   * @param headers Headers to include in the response entity.
   * @param rr The resource response to include in the entity.
   * @return A formatted ResponseEntity.
   */
  public static ResponseEntity<?> toResponseEntity(HttpHeaders headers, ResourceResponse rr) {
    if (rr == null) {
      return new ResponseEntity<>(headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    if (Objects.requireNonNull(rr.getStatus()) == Response.Status.NOT_MODIFIED) {
      return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);
    }
    net.es.sense.rm.api.common.Error.ErrorBuilder eb = net.es.sense.rm.api.common.Error.builder()
        .error(rr.getStatus().getReasonPhrase());
    rr.getError().ifPresent(eb::error_description);
    return new ResponseEntity<>(eb.build(), HttpStatus.valueOf(rr.getStatus().getStatusCode()));
  }

  /**
   * Parse the ifModifiedSince HTTP header into a time long.
   *
   * @param ifModifiedSince The ifModifiedSince header string.
   * @return The time value of ifModifiedSince as a long.
   */
  public static long parseIfModifiedSince(String ifModifiedSince) {
    long ifms = 0;
    if (!Strings.isNullOrEmpty(ifModifiedSince)) {
      Date lastModified = DateUtils.parseDate(ifModifiedSince);
      if (lastModified != null) {
        ifms = lastModified.getTime();
      }
    }

    return ifms;
  }
}
