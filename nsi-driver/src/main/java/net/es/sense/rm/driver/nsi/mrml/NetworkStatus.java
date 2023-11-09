package net.es.sense.rm.driver.nsi.mrml;

import net.es.sense.rm.driver.nsi.cs.db.Reservation;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ProvisionStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;

/**
 * This class performs NSI CS reservation state evaluations to come up with a
 * single NetworkStatus state for the MRML model.
 */
public class NetworkStatus {
  /**
   * Computes and returns the NetworkStatus state for the specified reservation.
   *
   * @param reservation
   * @return
   */
  public static NetworkStatusEnum parse(Reservation reservation) {
    // States - default to unknown
    NetworkStatusEnum status = NetworkStatusEnum.UNKNOWN;
    // activating
    if (stateCase(reservation, ReservationStateEnumType.RESERVE_START, ProvisionStateEnumType.PROVISIONING,
        null, false)) {
      status = NetworkStatusEnum.ACTIVATING;
    }
    // activated
    else if (stateCase(reservation, ReservationStateEnumType.RESERVE_START, ProvisionStateEnumType.PROVISIONED,
        LifecycleStateEnumType.CREATED, true)) {
      status = NetworkStatusEnum.ACTIVATED;
    }
    // activate-error
    else if (stateCase(reservation, ReservationStateEnumType.RESERVE_START, ProvisionStateEnumType.PROVISIONED,
        LifecycleStateEnumType.FAILED, null)) {
      status = NetworkStatusEnum.ACTIVATE_ERROR;
    }
    // deactivating
    else if (stateCase(reservation, null, ProvisionStateEnumType.PROVISIONED,
        LifecycleStateEnumType.TERMINATING, null)) {
      status = NetworkStatusEnum.DEACTIVATING;
    }
    // deactivate-error
    else if (stateCase(reservation, ReservationStateEnumType.RESERVE_FAILED, null,
        null, false) || stateCase(reservation, null,
        null, LifecycleStateEnumType.FAILED, false)) {
      status = NetworkStatusEnum.DEACTIVATE_ERROR;
    }
    // deactivated
    else if (stateCase(reservation, null, ProvisionStateEnumType.PROVISIONED,
        LifecycleStateEnumType.TERMINATED,false) || stateCase(reservation,
        ReservationStateEnumType.RESERVE_HELD, ProvisionStateEnumType.RELEASED,
        LifecycleStateEnumType.CREATED, false) || stateCase(reservation,
        ReservationStateEnumType.RESERVE_START, ProvisionStateEnumType.PROVISIONED,
        LifecycleStateEnumType.CREATED, false)) {
      status = NetworkStatusEnum.DEACTIVATED;
    }
    // error

    //
    return status;
  }

  /**
   * Performs reservation state comparison.
   *
   * @param reservation
   * @param res
   * @param prov
   * @param life
   * @param dataPlane
   * @return
   */
  private static boolean stateCase(Reservation reservation, ReservationStateEnumType res,
                                   ProvisionStateEnumType prov, LifecycleStateEnumType life,
                                   Boolean dataPlane) {
    boolean test = dataPlane == null || reservation.isDataPlaneActive() == dataPlane;
    if (res != null && reservation.getReservationState() != null && !reservation.getReservationState().equals(res)) {
      test = false;
    }
    if (prov != null && !reservation.getProvisionState().equals(prov)) {
      test = false;
    }
    if (life != null && !reservation.getLifecycleState().equals(life)) {
      test = false;
    }

    return test;
  }
}
