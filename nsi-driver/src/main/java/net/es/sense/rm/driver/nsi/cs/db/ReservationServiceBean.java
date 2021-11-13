package net.es.sense.rm.driver.nsi.cs.db;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.ogf.schemas.nsi._2013._12.connection.types.LifecycleStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ProvisionStateEnumType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationStateEnumType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Service
@Transactional(propagation=Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE, readOnly=true)
public class ReservationServiceBean implements ReservationService {

  @Autowired
  private ReservationRepository reservationRepository;

  @Override
  public long getLastDiscovered() {
    Long result = reservationRepository.findNewestDiscovered();
    log.debug("ReservationServiceBean: getLastDiscovered returned {}", result);
    return result != null ? result : 0;
  }

  @Override
  public Reservation getNewest() {
    Collection<Reservation> results = reservationRepository.findFirst1ByOrderByDiscoveredDesc();
    if (results != null) {
      Optional<Reservation> findFirst = results.stream().findFirst();
      if (findFirst.isPresent()) {
        return findFirst.get();
      }
    }
    return null;
  }

  @Override
  public Reservation findNewest() {
    return reservationRepository.findNewest();
  }

  @Override
  public Collection<Reservation> get() {
    return Lists.newArrayList(reservationRepository.findAll());
  }

  @Override
  public Collection<Reservation> getByTopologyId(String topologyId) {
    return reservationRepository.findByTopologyId(topologyId);
  }

  @Override
  public Collection<Reservation> getByGlobalReservationId(String globalReservationId) {
    return reservationRepository.findByGlobalReservationId(globalReservationId);
  }

  @Override
  public Reservation get(long id) {
    return reservationRepository.findOne(id);
  }

  @Override
  public Collection<Reservation> get(String connectionId) {
    return reservationRepository.findByConnectionId(connectionId);
  }

  @Override
  public Reservation get(String providerNsa, String connectionId) {
    return reservationRepository.findByProviderNsaAndConnectionId(providerNsa, connectionId);
  }

  @Override
  public Collection<Reservation> getByParentConnectionId(String providerNsa, String parentConnectionId) {
    return reservationRepository.findByProviderNsaAndParentConnectionId(providerNsa, parentConnectionId);
  }

  @Override
  public Collection<Reservation> getByParentConnectionId(String parentConnectionId) {
    return reservationRepository.findByParentConnectionId(parentConnectionId);
  }

  @Override
  public Collection<Reservation> getByAnyConnectionId(String providerNsa, String connectionId)  {
    Set<Reservation> reservations = new HashSet<>();
    Reservation r = reservationRepository.findByProviderNsaAndConnectionId(providerNsa, connectionId);
    if (r != null) {
      reservations.add(r);
    }

    Collection<Reservation> rc = reservationRepository.findByProviderNsaAndParentConnectionId(providerNsa, connectionId);
    if (rc != null) {
      reservations.addAll(rc);
    }

    return reservations;
  }

  @Override
  public Collection<Reservation> getByProviderNsa(String providerNsa) {
    return reservationRepository.findByProviderNsa(providerNsa);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public Reservation store(Reservation reservation) {
    if (Strings.isNullOrEmpty(reservation.getProviderNsa()) || Strings.isNullOrEmpty(reservation.getConnectionId())) {
      return null;
    }
    return reservationRepository.save(reservation);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(Reservation reservation) {
    reservationRepository.delete(reservation);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(long id) {
    reservationRepository.delete(id);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete() {
    for (Reservation reservation : reservationRepository.findAll()) {
      reservationRepository.delete(reservation);
    }
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public int setDataPlaneActive(long id, boolean dataPlaneActive, long discovered) {
    return reservationRepository.setDataPlaneActive(id, dataPlaneActive, discovered);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public int setFailedState(long id, ReservationStateEnumType reservationState,
        LifecycleStateEnumType lifecycleState, Reservation.ErrorState errorState,
        String errorMessage, long discovered) {
    return reservationRepository.setFailedState(id, reservationState,
            lifecycleState, errorState, errorMessage, discovered);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public int setErrorState(long id, Reservation.ErrorState errorState, String errorMessage, long discovered) {
    return reservationRepository.setErrorState(id, errorState, errorMessage, discovered);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public int setReservationState(long id, ReservationStateEnumType reservationState, long discovered) {
    return reservationRepository.setReservationState(id, reservationState, discovered);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public int setProvisionState(long id, ProvisionStateEnumType provisionState, long discovered) {
    return reservationRepository.setProvisionState(id, provisionState, discovered);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public int setLifecycleState(long id, LifecycleStateEnumType lifecycleState, long discovered) {
    return reservationRepository.setLifecycleState(id, lifecycleState, discovered);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public int setReservationAndLifecycleState(long id, ReservationStateEnumType reservationState,
          LifecycleStateEnumType lifecycleState, long discovered) {
    return reservationRepository.setReservationAndLifecycleState(id, reservationState, lifecycleState, discovered);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public int setReserveFailed(long id, ReservationStateEnumType reservationState,
        Reservation.ErrorState errorState, String errorMessage, long discovered) {
    return reservationRepository.setReserveFailed(id, reservationState, errorState, errorMessage, discovered);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public int setDirty(long id, boolean dirty) {
    return reservationRepository.setDirty(id, dirty);
  }
}
