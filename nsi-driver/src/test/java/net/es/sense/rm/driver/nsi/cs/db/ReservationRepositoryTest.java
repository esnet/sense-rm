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

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ProvisionStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

import static java.util.Comparator.comparing;
import static org.junit.Assert.*;

/**
 * Some unit tests for the ReservationRepository class.
 *
 * @author hacksaw
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ReservationRepository.class, Reservation.class, DbUnitTestConfiguration.class })
@AutoConfigureTestDatabase
@EnableAutoConfiguration
@ActiveProfiles("test")
@Transactional
public class ReservationRepositoryTest {
  @Autowired
  private ReservationRepository reservationRepository;

  static final String R1_CONNID = "1234";
  static final long R1_DISCOVERED = 1000000;
  static final String R1_TOPOLOGY = "Bob";

  static final String R2_CONNID = "5678";
  static final long R2_DISCOVERED = 1635365303430L;
  static final String R2_TOPOLOGY = "Fred";

  static final String R4_CONNID = "85ca15f6-5232-47bc-8cfe-3883cf1cb241";
  static final long R4_DISCOVERED = 1635365303600L;

  /**
   * Build a small database for the unit tests.
   */
  public void buildSmallDatabase() {
    assertEquals(0, reservationRepository.count());

    Reservation r1 = new Reservation();
    r1.setConnectionId(R1_CONNID);
    //r1.setDiscovered(10000);
    r1.setVersion(2);
    r1.setUniqueId(UUID.randomUUID().toString());
    r1.setDiscovered(R1_DISCOVERED);
    r1.setTopologyId(R1_TOPOLOGY);
    r1.setProviderNsa("Bobby");
    r1.setErrorState(Reservation.ErrorState.NSIERROREVENT);
    reservationRepository.save(r1);

    Reservation r2 = new Reservation();
    r2.setConnectionId(R2_CONNID);
    //r2.setDiscovered(20000);
    r2.setVersion(3);
    r2.setUniqueId(UUID.randomUUID().toString());
    r2.setDiscovered(R2_DISCOVERED);
    r2.setTopologyId(R2_TOPOLOGY);
    r2.setProviderNsa("Boogy");
    reservationRepository.save(r2);

    Reservation r3 = new Reservation();
    r3.setDiscovered(1635365303428L);
    r3.setProviderNsa("urn:ogf:network:netherlight.net:2013:nsa:safnari");
    r3.setUniqueId(UUID.randomUUID().toString());
    r3.setGlobalReservationId("urn:ogf:network:surf.nl:2020:production:switch:EVTS.A-GOLE:conn+460af7e5-a4b0-4989-8360-491d87a31afb:resource+links-Connection_1:vlan+2025");
    r3.setDescription("deltaId+e25a5f28-d5de-4603-848d-4456cf5e6582:uuid+f1ed34f9-9c15-485a-a9fc-5054217487e0");
    r3.setConnectionId("85ca15f6-5232-47bc-8cfe-3883cf1cb241");
    r3.setTopologyId("urn:ogf:network:surf.nl:2020:production");
    r3.setServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");
    r3.setStartTime(0L);
    r3.setEndTime(1666901297403L);
    r3.setReservationState(ReservationStateEnumType.RESERVE_START);
    r3.setProvisionState(ProvisionStateEnumType.RELEASED);
    r3.setLifecycleState(LifecycleStateEnumType.CREATED);
    r3.setDataPlaneActive(false);
    r3.setVersion(0);
    r3.setUniqueId(UUID.randomUUID().toString());
    r3.setService("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<ns7:p2ps xmlns:ns6=\"http://schemas.ogf.org/nsi/2013/12/framework/types\" " +
        "xmlns:ns5=\"http://www.w3.org/2001/04/xmlenc#\" xmlns:ns8=\"http://schemas.ogf.org/nsi/2013/12/framework/headers\" xmlns:ns7=\"http://schemas.ogf.org/nsi/2013/12/services/point2point\" xmlns:ns2=\"http://schemas.ogf.org/nsi/2013/12/connection/types\" xmlns:ns4=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:ns3=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
        "    <capacity>1000</capacity>\n" +
        "    <directionality>Bidirectional</directionality>\n" +
        "    <symmetricPath>true</symmetricPath>\n" +
        "    <sourceSTP>urn:ogf:network:surf.nl:2020:production:netherlight.cern-1?vlan=2025</sourceSTP>\n" +
        "    <destSTP>urn:ogf:network:surf.nl:2020:production:netherlight.canarie-1?vlan=2025</destSTP>\n" +
        "</ns7:p2ps>");
    reservationRepository.save(r3);

    Reservation r4 = new Reservation();
    r4.setDiscovered(1635365303600L);
    r4.setProviderNsa("urn:ogf:network:netherlight.net:2013:nsa:safnari");
    r4.setUniqueId(UUID.randomUUID().toString());
    r4.setGlobalReservationId("urn:ogf:network:surf.nl:2020:production:switch:EVTS.A-GOLE:conn+460af7e5-a4b0-4989-8360-491d87a31afb:resource+links-Connection_1:vlan+2025");
    r4.setDescription("deltaId+e25a5f28-d5de-4603-848d-4456cf5e6582:uuid+f1ed34f9-9c15-485a-a9fc-5054217487e0");
    r4.setConnectionId("85ca15f6-5232-47bc-8cfe-3883cf1cb241");
    r4.setTopologyId("urn:ogf:network:surf.nl:2020:production");
    r4.setServiceType("http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE");
    r4.setStartTime(0L);
    r4.setEndTime(1666901297403L);
    r4.setReservationState(ReservationStateEnumType.RESERVE_START);
    r4.setProvisionState(ProvisionStateEnumType.RELEASED);
    r4.setLifecycleState(LifecycleStateEnumType.CREATED);
    r4.setDataPlaneActive(false);
    r4.setVersion(1);
    r4.setUniqueId(UUID.randomUUID().toString());
    r4.setService("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<ns7:p2ps xmlns:ns6=\"http://schemas.ogf.org/nsi/2013/12/framework/types\" " +
        "xmlns:ns5=\"http://www.w3.org/2001/04/xmlenc#\" xmlns:ns8=\"http://schemas.ogf.org/nsi/2013/12/framework/headers\" xmlns:ns7=\"http://schemas.ogf.org/nsi/2013/12/services/point2point\" xmlns:ns2=\"http://schemas.ogf.org/nsi/2013/12/connection/types\" xmlns:ns4=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:ns3=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
        "    <capacity>10000</capacity>\n" +
        "    <directionality>Bidirectional</directionality>\n" +
        "    <symmetricPath>true</symmetricPath>\n" +
        "    <sourceSTP>urn:ogf:network:surf.nl:2020:production:netherlight.cern-1?vlan=2025</sourceSTP>\n" +
        "    <destSTP>urn:ogf:network:surf.nl:2020:production:netherlight.canarie-1?vlan=2025</destSTP>\n" +
        "</ns7:p2ps>");
    reservationRepository.save(r4);

    assertEquals(4, reservationRepository.count());
  }

  /**
   * Do a set of empty database tests.
   */
  @Test
  @Order(1)
  public void emptyTest() {
    assertEquals(0, reservationRepository.count());

    Long discovered = reservationRepository.findNewestDiscovered();
    assertNull(discovered);

    Collection<Reservation> reserves = reservationRepository.findFirst1ByOrderByDiscoveredDesc();
    assertEquals(0, reserves.size());

    reserves = reservationRepository.findByConnectionId(R1_CONNID);
    assertNotNull(reserves);
    assertEquals(0, reserves.size());

    reserves = reservationRepository.findByTopologyId(R2_TOPOLOGY);
    assertNotNull(reserves);
    assertEquals(0, reserves.size());
  }

  /**
   * Do various get tests.
   */
  @Test
  @Order(2)
  public void getTests() {
    buildSmallDatabase();

    assertEquals(4, reservationRepository.count());

    Long discovered = reservationRepository.findNewestDiscovered();
    assertNotNull(discovered);
    assertEquals(R4_DISCOVERED, (long) discovered);

    Collection<Reservation> reserves = reservationRepository.findFirst1ByOrderByDiscoveredDesc();
    assertEquals(1, reserves.size());
    assertEquals(R4_CONNID, reserves.stream().findFirst().get().getConnectionId());

    reserves = reservationRepository.findAllNewer(R1_DISCOVERED);
    assertEquals(3, ((Collection<?>) reserves).size());
    assertEquals(R2_CONNID, reserves.iterator().next().getConnectionId());

    reserves = reservationRepository.findByConnectionId(R1_CONNID);
    assertNotNull(reserves);
    assertEquals(1, reserves.size());

    reserves = reservationRepository.findByTopologyId(R2_TOPOLOGY);
    assertNotNull(reserves);
    assertEquals(1, reserves.size());
    assertEquals(R2_CONNID, reserves.iterator().next().getConnectionId());

    // Test retrieval of large error message.
    reserves = reservationRepository.findByConnectionId("85ca15f6-5232-47bc-8cfe-3883cf1cb241");
    assertNotNull(reserves);
    assertEquals(2, reserves.size());
    Reservation reservation = reserves.iterator().next();
    assertEquals("85ca15f6-5232-47bc-8cfe-3883cf1cb241", reservation.getConnectionId());
    reservation = reserves.iterator().next();
    assertEquals("85ca15f6-5232-47bc-8cfe-3883cf1cb241", reservation.getConnectionId());

    reservationRepository.deleteAll();
    assertEquals(0, reservationRepository.count());
  }


  /**
   * Do a set of tests over a small number of database entries.
   */
  @Test
  @Order(3)
  public void transactionTest() {
    buildSmallDatabase();

    // Check to make sure the defaults are correct.
    Collection<Reservation> reserves = reservationRepository.findByConnectionId("85ca15f6-5232-47bc-8cfe-3883cf1cb241");
    assertNotNull(reserves);
    assertEquals(2, reserves.size());
    Reservation reservation = reserves.stream().max(comparing(Reservation::getVersion)).orElse(null);
    assertNotNull(reservation);
    assertEquals("85ca15f6-5232-47bc-8cfe-3883cf1cb241", reservation.getConnectionId());
    assertEquals(1, reservation.getVersion());
    assertEquals(ReservationStateEnumType.RESERVE_START, reservation.getReservationState());
    assertEquals(ProvisionStateEnumType.RELEASED, reservation.getProvisionState());
    assertEquals(LifecycleStateEnumType.CREATED, reservation.getLifecycleState());
    assertFalse(reservation.isDataPlaneActive());
    assertEquals(Reservation.ErrorState.NONE, reservation.errorState);

    // Update dataplaneStateChange
    long current = System.currentTimeMillis();
    int count = reservationRepository.setDataPlaneActive(reservation.getId(), true, current);
    assertEquals(1, count);

    // Check to see if the update was completed.
    reserves = reservationRepository.findByConnectionId("85ca15f6-5232-47bc-8cfe-3883cf1cb241");
    assertNotNull(reserves);
    assertEquals(2, reserves.size());
    reservation = reserves.stream().max(comparing(Reservation::getVersion)).orElse(null);
    assertNotNull(reservation);
    assertEquals("85ca15f6-5232-47bc-8cfe-3883cf1cb241", reservation.getConnectionId());
    assertEquals(1, reservation.getVersion());
    assertTrue(reservation.isDataPlaneActive());
    assertEquals(current, reservation.getDiscovered());

    // Now test big adding a big error message.
    current = System.currentTimeMillis();
    count = reservationRepository.setFailedState(reservation.getId(),
            ReservationStateEnumType.RESERVE_FAILED,
            LifecycleStateEnumType.FAILED,
            Reservation.ErrorState.NSIERROREVENT,
            "errorEvent, \n" +
"    cid = 85ca15f6-5232-47bc-8cfe-3883cf1cb241, \n" +
"    errorEvent = <?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
"<ns2:errorEvent xmlns:ns6=\"http://schemas.ogf.org/nsi/2013/12/framework/types\" xmlns:ns5=\"http://www.w3.org/2001/04/xmlenc#\" xmlns:ns8=\"http://schemas.ogf.org/nsi/2013/12/framework/headers\" xmlns:ns7=\"http://schemas.ogf.org/nsi/2013/12/services/point2point\" xmlns:ns2=\"http://schemas.ogf.org/nsi/2013/12/connection/types\" xmlns:ns4=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:ns3=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
"    <connectionId>85ca15f6-5232-47bc-8cfe-3883cf1cb241</connectionId>\n" +
"    <notificationId>1</notificationId>\n" +
"    <timeStamp>2021-10-27T20:08:23.310880Z</timeStamp>\n" +
"    <event>activateFailed</event>\n" +
"    <originatingConnectionId></originatingConnectionId>\n" +
"    <originatingNSA>None</originatingNSA>\n" +
"</ns2:errorEvent>",
            current);
    assertEquals(1, count);

    // Test retrieval of large error message.
    reserves = reservationRepository.findByConnectionId("85ca15f6-5232-47bc-8cfe-3883cf1cb241");
    assertNotNull(reserves);
    assertEquals(2, reserves.size());
    reservation = reserves.stream().max(comparing(Reservation::getVersion)).orElse(null);
    Reservation old_reservation = reserves.stream().min(comparing(Reservation::getVersion)).orElse(null);
    assertNotNull(reservation);
    assertNotNull(old_reservation);
    assertEquals("85ca15f6-5232-47bc-8cfe-3883cf1cb241", reservation.getConnectionId());
    assertEquals(Reservation.ErrorState.NSIERROREVENT, reservation.errorState);
    assertEquals(868, reservation.getErrorMessage().length());
    assertEquals(current, reservation.getDiscovered());
    assertEquals(Reservation.ErrorState.NONE, old_reservation.errorState);
    assertNull(old_reservation.getErrorMessage());

    // setErrorState
    current = System.currentTimeMillis();
    count = reservationRepository.setErrorState(reservation.getId(), Reservation.ErrorState.NSIERROR,
            "BobbyBoogie", current);
    assertEquals(1, count);

    reservation = reservationRepository.findNewest();
    assertNotNull(reservation);
    assertEquals("85ca15f6-5232-47bc-8cfe-3883cf1cb241", reservation.getConnectionId());
    assertEquals(Reservation.ErrorState.NSIERROR, reservation.getErrorState());
    assertEquals("BobbyBoogie", reservation.getErrorMessage());
    assertEquals(current, reservation.getDiscovered());

    // setReservationState
    current = System.currentTimeMillis();
    count = reservationRepository.setReservationState(reservation.getId(),
            ReservationStateEnumType.RESERVE_CHECKING, current);
    assertEquals(1, count);

    reservation = reservationRepository.findNewest();
    assertNotNull(reservation);
    assertEquals("85ca15f6-5232-47bc-8cfe-3883cf1cb241", reservation.getConnectionId());
    assertEquals(ReservationStateEnumType.RESERVE_CHECKING, reservation.getReservationState());
    assertEquals(current, reservation.getDiscovered());

    // setProvisionState
    current = System.currentTimeMillis();
    count = reservationRepository.setProvisionState(reservation.getId(),
            ProvisionStateEnumType.PROVISIONING, current);
    assertEquals(1, count);

    reservation = reservationRepository.findNewest();
    assertNotNull(reservation);
    assertEquals("85ca15f6-5232-47bc-8cfe-3883cf1cb241", reservation.getConnectionId());
    assertEquals(ProvisionStateEnumType.PROVISIONING, reservation.getProvisionState());
    assertEquals(current, reservation.getDiscovered());

    // setLifecycleState
    current = System.currentTimeMillis();
    count = reservationRepository.setLifecycleState(reservation.getId(),
            LifecycleStateEnumType.PASSED_END_TIME, current);
    assertEquals(1, count);

    reservation = reservationRepository.findNewest();
    assertNotNull(reservation);
    assertEquals("85ca15f6-5232-47bc-8cfe-3883cf1cb241", reservation.getConnectionId());
    assertEquals(LifecycleStateEnumType.PASSED_END_TIME, reservation.getLifecycleState());
    assertEquals(current, reservation.getDiscovered());

    // Delete the database entries.
    reservationRepository.deleteAll();
    assertEquals(0, reservationRepository.count());
  }

  /**
   * Test behaviour when multiple instanced of the same reservations
   * exist with different versions.
   */
  @Test
  @Order(4)
  public void versionTest() {
    buildSmallDatabase();

    Collection<Reservation> byGlobalReservationId = reservationRepository.findByGlobalReservationId("urn:ogf:network:surf.nl:2020:production:switch:EVTS.A-GOLE:conn+460af7e5-a4b0-4989-8360-491d87a31afb:resource+links-Connection_1:vlan+2025");
    assertEquals(2, byGlobalReservationId.size());
  }
}
