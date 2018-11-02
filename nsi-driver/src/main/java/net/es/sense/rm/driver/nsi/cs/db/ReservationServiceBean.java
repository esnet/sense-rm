package net.es.sense.rm.driver.nsi.cs.db;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.Collection;
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
    Reservation findNewest = reservationRepository.findNewestReservation();
    if (findNewest == null) {
      return 0;
    }
    return findNewest.getDiscovered();
  }

  @Override
  public Collection<Reservation> get() {
    return Lists.newArrayList(reservationRepository.findAll());
  }

  @Override
  public Collection<Reservation> getByTopologyId(String topologyId) {
    return Lists.newArrayList(reservationRepository.findByTopologyId(topologyId));
  }

  @Override
  public Collection<Reservation> getByGlobalReservationId(String globalReservationId) {
    return Lists.newArrayList(reservationRepository.findByGlobalReservationId(globalReservationId));
  }

  @Override
  public Reservation get(long id) {
    return reservationRepository.findOne(id);
  }

  @Override
  public Collection<Reservation> get(String connectionId) {
    return Lists.newArrayList(reservationRepository.findByConnectionId(connectionId));
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
