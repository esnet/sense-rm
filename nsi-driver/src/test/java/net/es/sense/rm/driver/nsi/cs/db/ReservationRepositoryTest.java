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
package net.es.sense.rm.driver.nsi.cs.db;

import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Some unit tests for the ReservationRepository class.
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ReservationRepository.class, Reservation.class, DbUnitTestConfiguration.class })
@DataJpaTest
@ActiveProfiles("test")
public class ReservationRepositoryTest {
  @Autowired
  private ReservationRepository reservations;

  static final String R1_CONNID = "1234";
  static final long R1_DISCOVERED = 1000000;
  static final String R1_TOPOLOGY = "Bob";

  static final String R2_CONNID = "5678";
  static final long R2_DISCOVERED = 2000000;
  static final String R2_TOPOLOGY = "Fred";

  /**
   * Build a small database for the unit tests.
   */
  public void buildSmallDatabase() {
    Reservation r1 = new Reservation();
    r1.setConnectionId(R1_CONNID);
    //r1.setDiscovered(10000);
    r1.setVersion(2);
    r1.setDiscovered(R1_DISCOVERED);
    r1.setTopologyId(R1_TOPOLOGY);
    r1.setProviderNsa("Bobby");
    reservations.save(r1);

    Reservation r2 = new Reservation();
    r2.setConnectionId(R2_CONNID);
    //r2.setDiscovered(20000);
    r2.setVersion(3);
    r2.setDiscovered(R2_DISCOVERED);
    r2.setTopologyId(R2_TOPOLOGY);
    r2.setProviderNsa("Boogy");
    reservations.save(r2);
  }

  /**
   * Do a set of empty database tests.
   */
  @Test
  public void emptyTest() {
    assertEquals(reservations.count(), 0);

    Long discovered = reservations.findNewestDiscovered();
    assertNull(discovered);

    Collection<Reservation> reserves = reservations.findFirst1ByOrderByDiscoveredDesc();
    assertEquals(0, reserves.size());

    reserves = reservations.findByConnectionId(R1_CONNID);
    assertNotNull(reserves);
    assertEquals(0, reserves.size());

    reserves = reservations.findByTopologyId(R2_TOPOLOGY);
    assertNotNull(reserves);
    assertEquals(0, reserves.size());
  }

  /**
   * Do a set of tests over a small number of database entries.
   */
  @Test
  public void basicTest() {
    buildSmallDatabase();

    assertEquals(reservations.count(), 2);

    Long discovered = reservations.findNewestDiscovered();
    assertNotNull(discovered);
    assertEquals(R2_DISCOVERED, (long) discovered);

    Collection<Reservation> reserves = reservations.findFirst1ByOrderByDiscoveredDesc();
    assertEquals(1, reserves.size());
    assertEquals(R2_CONNID, reserves.stream().findFirst().get().getConnectionId());

    reserves = reservations.findAllNewer(R1_DISCOVERED);
    assertEquals(((Collection<?>) reserves).size(), 1);
    assertEquals(R2_CONNID, reserves.iterator().next().getConnectionId());

    reserves = reservations.findByConnectionId(R1_CONNID);
    assertNotNull(reserves);
    assertEquals(1, reserves.size());

    reserves = reservations.findByTopologyId(R2_TOPOLOGY);
    assertNotNull(reserves);
    assertEquals(1, reserves.size());
    assertEquals(R2_CONNID, reserves.iterator().next().getConnectionId());

    reservations.deleteAll();
    assertEquals(0, reservations.count());
  }
}
