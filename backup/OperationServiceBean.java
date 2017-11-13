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
public class OperationServiceBean implements OperationService {
  @Autowired
  private OperationRepository operationRepository;

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public Operation store(Operation operation) {
    if (Strings.isNullOrEmpty(operation.globalReservationId) || Strings.isNullOrEmpty(operation.getCorrelationId())) {
      return null;
    }
    return operationRepository.save(operation);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(Operation operation) {
    operationRepository.delete(operation);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete(long id) {
    operationRepository.delete(id);
  }

  @Transactional(propagation=Propagation.REQUIRED, readOnly=false)
  @Override
  public void delete() {
    for (Operation reservation : operationRepository.findAll()) {
      operationRepository.delete(reservation);
    }
  }

  @Override
  public Collection<Operation> get() {
    return Lists.newArrayList(operationRepository.findAll());
  }

  @Override
  public Collection<Operation> getByConnectionId(String connectionId) {
    return Lists.newArrayList(operationRepository.findByConnectionId(connectionId));
  }

  @Override
  public Collection<Operation> getByGlobalReservationId(String globalReservationId) {
    return Lists.newArrayList(operationRepository.findByGlobalReservationId(globalReservationId));
  }

  @Override
  public Operation getByCorrelationId(String correlationId) {
    return operationRepository.findByCorrelationId(correlationId);
  }

  @Override
  public Operation get(long id) {
    return operationRepository.findOne(id);
  }

  @Override
  public Collection<Operation> get(String connectionId) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
