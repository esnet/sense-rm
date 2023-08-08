/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.sense.rm.api;

import com.google.common.base.Strings;
import net.es.sense.rm.driver.api.ResourceResponse;
import org.apache.http.client.utils.DateUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Date;

/**
 *
 * @author hacksaw
 */
public class Common {
  public static ResponseEntity<?> toResponseEntity(HttpHeaders headers, ResourceResponse rr) {
    if (rr == null) {
      return new ResponseEntity<>(headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    switch (rr.getStatus()) {
      case NOT_MODIFIED:
        return new ResponseEntity<>(headers, HttpStatus.NOT_MODIFIED);

        default:
          net.es.sense.rm.api.common.Error.ErrorBuilder eb = net.es.sense.rm.api.common.Error.builder().error(rr.getStatus().getReasonPhrase());
          rr.getError().ifPresent(e -> eb.error_description(e));
          return new ResponseEntity<>(eb.build(), HttpStatus.valueOf(rr.getStatus().getStatusCode()));
    }
  }

  public static long parseIfModfiedSince(String ifModifiedSince) {
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
