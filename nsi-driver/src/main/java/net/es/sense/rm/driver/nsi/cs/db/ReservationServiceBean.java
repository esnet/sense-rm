package net.es.sense.rm.driver.nsi.cs.db;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author hacksaw
 */
@Slf4j
@Service
@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
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
}
