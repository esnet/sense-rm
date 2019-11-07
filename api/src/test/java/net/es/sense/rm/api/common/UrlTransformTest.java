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

import lombok.extern.slf4j.Slf4j;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class UrlTransformTest {

  @Test
  public void test() throws Exception {
    UrlTransform utilities = new UrlTransform("(http://localhost:8401|https://nsi0.snvaca.pacificwave.net/sense-rm)");
    UriComponentsBuilder path = utilities.getPath("http://localhost:8401/api/sense/v1/models");
    assertEquals(path.toUriString(), "https://nsi0.snvaca.pacificwave.net/sense-rm/api/sense/v1/models");
  }
}
