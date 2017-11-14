package net.es.sense.rm.driver.nsi.cs.db;

import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;

/**
 *
 * @author hacksaw
 */
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Data
@Entity
@Table(name = "reservations")
public class Reservation implements Serializable {
  @Id
  @GeneratedValue
  long id;

  // The time I first discovered this version of the connection.
  long discovered;

  // The providerNSA hosting this conenction.
  @Basic(optional=false)
  String providerNsa;

  // Optional global reservation identifier.
  @Basic(optional=true)
  String globalReservationId;

  @Basic(optional=true)
  String description;

  // Connection identifier of this connection on the uPA.
  @Basic(optional=false)
  String connectionId;

  // The network hosting this connection.
  @Basic(optional=true)
  String topologyId;

  // The serviceType of the connection.
  @Basic(optional=true)
  String serviceType;

  // The schedule parameters for this reservation.
  long startTime;
  long endTime;

  @Basic(optional=false)
  ReservationStateEnumType reservationState;

  @Basic(optional=false)
  LifecycleStateEnumType lifecycleState;

  boolean dataPlaneActive = false;

  // Version of the reservation on the uPA.
  long version;

  // The service element encoded in a string.
  @Lob
  @Basic(fetch=FetchType.LAZY, optional=true)
  String service;
}
