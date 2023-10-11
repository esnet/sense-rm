package net.es.sense.rm.driver.nsi.cs.db;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 *
 * @author hacksaw
 */
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Data
@Entity
@Table(name = "operations")
public class Operation implements Serializable {
  @Id
  @GeneratedValue
  long id;

  @Basic(optional=false)
  String correlationId;

  // Global reservation identifier we use to uniquely identify our reservation.
  @Basic(optional=false)
  String globalReservationId;

  // Connection identifier returned by the PA.
  @Basic(optional=true)
  String connectionId;

  @Basic(optional=false)
  OperationType op;
}
