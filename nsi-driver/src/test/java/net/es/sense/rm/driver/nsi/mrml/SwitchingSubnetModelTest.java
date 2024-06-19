package net.es.sense.rm.driver.nsi.mrml;

import lombok.extern.slf4j.Slf4j;
import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import org.assertj.core.util.Sets;
import org.junit.Test;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ProvisionStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class SwitchingSubnetModelTest {

  @Test
  public void getActiveReservations() {
    List<Reservation> reservations = Arrays.asList(
        new Reservation(1,
            "1",
            System.currentTimeMillis(),
            false,
            "urn:ogf:network:es.net:2013:nsa",
            "urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db10:vt+l2-policy-Connection_1:vlan+3989",
            "uniqueId: 1",
            null,
            "connid: 1",
            "urn:ogf:network:es.net:2013::topology",
            "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE",
            0L,
            0L,
            ReservationStateEnumType.RESERVE_START,
            ProvisionStateEnumType.PROVISIONED,
            LifecycleStateEnumType.CREATED,
            false,
            0,
            Reservation.ErrorState.NONE,
            null,
            null
            ),
        new Reservation(2,
            "2",
            System.currentTimeMillis(),
            false,
            "urn:ogf:network:es.net:2013:nsa",
            "urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db12:vt+l2-policy-Connection_1:vlan+3900",
            "uniqueId: 2",
            null,
            "connid: 2",
            "urn:ogf:network:es.net:2013::topology",
            "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE",
            0L,
            0L,
            ReservationStateEnumType.RESERVE_START,
            ProvisionStateEnumType.PROVISIONED,
            LifecycleStateEnumType.CREATED,
            false,
            0,
            Reservation.ErrorState.NONE,
            null,
            null
            ),
        new Reservation(3,
            "3",
            System.currentTimeMillis(),
            false,
            "urn:ogf:network:es.net:2013:nsa",
            "urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db10:vt+l2-policy-Connection_1:vlan+3989",
            "uniqueId: 3",
            null,
            "connid: 1",
            "urn:ogf:network:es.net:2013::topology",
            "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE",
            0L,
            0L,
            ReservationStateEnumType.RESERVE_HELD,
            ProvisionStateEnumType.PROVISIONED,
            LifecycleStateEnumType.CREATED,
            false,
            1,
            Reservation.ErrorState.NONE,
            null,
            null
            ),
        new Reservation(4,
            "4",
            System.currentTimeMillis(),
            false,
            "urn:ogf:network:es.net:2013:nsa",
            "urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db12:vt+l2-policy-Connection_1:vlan+3900",
            "uniqueId: 4",
            null,
            "connid: 2",
            "urn:ogf:network:es.net:2013::topology",
            "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE",
            0L,
            0L,
            ReservationStateEnumType.RESERVE_START,
            ProvisionStateEnumType.PROVISIONED,
            LifecycleStateEnumType.CREATED,
            false,
            1,
            Reservation.ErrorState.NONE,
            null,
            null
            ),
        new Reservation(5,
            "5",
            System.currentTimeMillis(),
            false,
            "urn:ogf:network:es.net:2013:nsa",
            "urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db15:vt+l2-policy-Connection_1:vlan+3901",
            "uniqueId: 5",
            null,
            "connid: 3",
            "urn:ogf:network:es.net:2013::topology",
            "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE",
            0L,
            0L,
            ReservationStateEnumType.RESERVE_START,
            ProvisionStateEnumType.PROVISIONED,
            LifecycleStateEnumType.CREATED,
            false,
            5,
            Reservation.ErrorState.NONE,
            null,
            null
            ),
        new Reservation(6,
            "6",
            System.currentTimeMillis(),
            false,
            "urn:ogf:network:es.net:2013:nsa",
            "urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db69:vt+l2-policy-Connection_1:vlan+3969",
            "uniqueId: 6",
            null,
            "connid: 4",
            "urn:ogf:network:es.net:2013::topology",
            "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE",
            0L,
            0L,
            ReservationStateEnumType.RESERVE_START,
            ProvisionStateEnumType.PROVISIONED,
            LifecycleStateEnumType.TERMINATING,
            false,
            1,
            Reservation.ErrorState.NONE,
            null,
            null
        ),
        new Reservation(7,
            "7",
            System.currentTimeMillis(),
            false,
            "urn:ogf:network:es.net:2013:nsa",
            "urn:ogf:network:es.net:2013:topology:ServiceDomain:EVTS.A-GOLE:conn+efb2907a-6f11-4f1e-a0a7-8e46cf75db70:vt+l2-policy-Connection_1:vlan+3970",
            "uniqueId: 7",
            null,
            "connid: 7",
            "urn:ogf:network:es.net:2013::topology",
            "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE",
            0L,
            0L,
            ReservationStateEnumType.RESERVE_START,
            ProvisionStateEnumType.PROVISIONED,
            LifecycleStateEnumType.TERMINATED,
            false,
            1,
            Reservation.ErrorState.NONE,
            null,
            null
        )
    );

    List<Reservation> results = reservations.stream()
        .filter(r -> r.getReservationState() == ReservationStateEnumType.RESERVE_START && (r.getLifecycleState() == LifecycleStateEnumType.CREATED || r.getLifecycleState() == LifecycleStateEnumType.TERMINATING))
        .collect(Collectors.groupingBy(Reservation::getConnectionId, Collectors.maxBy(Comparator.comparing(Reservation::getVersion))))
        .values().stream()
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();

    assertEquals(4, results.size());
    assertEquals(results.stream().map(Reservation::getUniqueId).collect(Collectors.toSet()), Sets.set("1", "4", "5", "6"));
  }
}
