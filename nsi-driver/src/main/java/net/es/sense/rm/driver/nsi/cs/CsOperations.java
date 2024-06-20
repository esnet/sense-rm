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
package net.es.sense.rm.driver.nsi.cs;

import jakarta.xml.ws.Holder;
import jakarta.xml.ws.soap.SOAPFaultException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.cs.lib.Client;
import net.es.nsi.cs.lib.ClientUtil;
import net.es.nsi.cs.lib.Helper;
import net.es.nsi.cs.lib.NsiHeader;
import net.es.sense.rm.driver.nsi.cs.db.Operation;
import net.es.sense.rm.driver.nsi.cs.db.OperationMapRepository;
import net.es.sense.rm.driver.nsi.cs.db.OperationType;
import net.es.sense.rm.driver.nsi.cs.db.StateType;
import net.es.sense.rm.driver.nsi.properties.NsiProperties;
import org.ogf.schemas.nsi._2013._12.connection.provider.Error;
import org.ogf.schemas.nsi._2013._12.connection.provider.ServiceException;
import org.ogf.schemas.nsi._2013._12.connection.types.*;
import org.ogf.schemas.nsi._2013._12.framework.headers.CommonHeaderType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * This is a refactoring error in progress - attempting to gather NSI CS operations
 * into a single class with associated semaphore locking for async callback
 * handling.
 *
 * @author hacksaw
 */

@Slf4j
public class CsOperations {
  private final NsiProperties nsiProperties;
  private final OperationMapRepository operationMap;
  private final List<String> correlationIds = new ArrayList<>();
  private final List<String> connectionIds = new ArrayList<>();
  private final List<Exception> exceptions = new ArrayList<>();
  private final List<String> successful = new ArrayList<>();
  private final List<String> failed = new ArrayList<>();

  private static final org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory CS_FACTORY
          = new org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory();
  private static final org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory P2PS_FACTORY
          = new org.ogf.schemas.nsi._2013._12.services.point2point.ObjectFactory();

  /**
   *
   * @param nsiProperties
   * @param operationMap
   */
  public CsOperations(NsiProperties nsiProperties, OperationMapRepository operationMap) {
    this.nsiProperties = nsiProperties;
    this.operationMap = operationMap;
  }

  /**
   *
   * @param correlationId
   * @return
   */
  public boolean addCorrelationId(String correlationId) {
    return this.correlationIds.add(correlationId);
  }

  /**
   *
   * @param correlationId
   * @return
   */
  public boolean removeCorrelationId(String correlationId) {
    return this.correlationIds.remove(correlationId);
  }

  /**
   *
   * @param connectionId
   * @return
   */
  public boolean addConnectionId(String connectionId) {
    return this.connectionIds.add(connectionId);
  }

  /**
   *
   * @param connectionId
   * @return
   */
  public boolean removeConnectionId(String connectionId) {
    return this.connectionIds.remove(connectionId);
  }

  /**
   * @return the exceptions
   */
  public List<Exception> getExceptions() {
    return exceptions;
  }

  /**
   * @return the successful
   */
  public List<String> getSuccessful() {
    return successful;
  }

  /**
   * Wait for queued NSI operations to complete by waiting on a shared semaphore
   * between this thread and the NSI CS callback thread.
   *
   * @return Will return an exception (the last encountered) if one has occurred.
   */
  public boolean confirm() {
    for (String id : correlationIds) {
      log.info("[CsOperations] waiting for completion of correlationId = {}", id);

      Operation op = operationMap.get(id);
      if (op == null) {
        log.error("[CsOperations] no operation in map for correlationId = {}", id);
        failed.add(id);
        exceptions.add(new IllegalArgumentException("no operation in map for correlationId = " + id));
      } else if (operationMap.wait(id)) {
        log.info("[CsOperations] operation {} completed, correlationId = {}", op.getOperation(), id);

        switch(op.getOperation()) {
          case reserve:
            check(op, StateType.reserved);
            break;

          case reserveCommit:
            check(op, StateType.committed);
            break;

          case provision:
            check(op, StateType.provisioned);
            break;

          case release:
            check(op, StateType.releasing);
            break;

          case terminate:
            check(op, StateType.terminated);
            break;

          default:
            check(op, StateType.unknown);
            break;
        }
      } else {
        log.error("[CsOperations] timeout, failed to get response for correlationId = {}", id);
        exceptions.add(new TimeoutException("Operation failed to reserve, correlationId = "
                  + id + ", state = " + op.getState()));
        failed.add(id);
      }
    }

    return failed.isEmpty();
  }

  /**
   *
   * @param op
   * @param st
   */
  private void check(Operation op, StateType st) {
    if (op.getState() != st) {
      log.error("[CsOperations] operation {} failed, correlationId = {}, state = {}",
              op.getOperation(), op.getCorrelationId(), op.getState(), op.getException());

      if (op.getException() != null) {
        exceptions.add(new ServiceException("Operation " + op.getOperation() +
                " failed, correlationId = " + op.getCorrelationId(), op.getException()));
      }
      else {
        exceptions.add(new IllegalArgumentException("Operation " + op.getOperation() +
                " failed, correlationId = " + op.getCorrelationId() + ", state = " + op.getState()));
      }

      failed.add(op.getCorrelationId());
    } else {
      log.info("[CsOperations] operation {} successful, correlationId = {}, state = {}",
              op.getOperation(), op.getCorrelationId(), op.getState(), op.getException());
      successful.add(op.getCorrelationId());
    }
  }

  /**
   *
   * @return
   * @throws Error
   */
  public QuerySummaryConfirmedType query() throws Error {
    String correlationId = Helper.getUUID();
    CommonHeaderType requestHeader = NsiHeader.builder()
            .correlationId(correlationId)
            .providerNSA(nsiProperties.getProviderNsaId())
            .requesterNSA(nsiProperties.getNsaId())
            .replyTo(nsiProperties.getRequesterConnectionURL())
            .build()
            .getRequestHeaderType();

    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;

    QueryType query = CS_FACTORY.createQueryType();
    try {
      log.debug("[CsOperations] Sending querySummarySync: providerNSA = {}, correlationId = {}",
              nsiProperties.getProviderNsaId(), correlationId);

      Client nsiClient = new Client(nsiProperties.getProviderConnectionURL());
      QuerySummaryConfirmedType querySummarySync = nsiClient.getProxy().querySummarySync(query, header);

      log.debug("[CsOperations] QuerySummaryConfirmed received, providerNSA = {}, correlationId = {}",
              header.value.getProviderNSA(), header.value.getCorrelationId());

      return querySummarySync;
    } catch (Error ex) {
      log.error("[CsOperations] querySummarySync exception on operation - {} {}",
              ex.getFaultInfo().getServiceException().getErrorId(),
              ex.getFaultInfo().getServiceException().getText());
      throw ex;
    }
  }

  /**
   * Issue an NSI reservation to the target NSA for the requested interface.
   *
   * @param r
   * @return
   * @throws ServiceException
   * @throws SOAPFaultException
   */
  public String reserve(ReserveType r) throws ServiceException, SOAPFaultException {
    String correlationId = Helper.getUUID();

    CommonHeaderType requestHeader = NsiHeader.builder()
            .correlationId(correlationId)
            .providerNSA(nsiProperties.getProviderNsaId())
            .requesterNSA(nsiProperties.getNsaId())
            .replyTo(nsiProperties.getRequesterConnectionURL())
            .build()
            .getRequestHeaderType();

    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;

    this.store(OperationType.reserve, StateType.reserving, correlationId);

    // Issue the NSI reservation request.
    try {
      log.debug("[CsOperations] issuing reserve operation correlationId = {}", correlationId);

      ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
      ReserveResponseType response = nsiClient.getProxy().reserve(r, header);

      String connectionId =  response.getConnectionId();
      connectionIds.add(connectionId);

      log.debug("[CsOperations] issued reserve operation correlationId = {}, connectionId = {}",
              correlationId, connectionId);

    } catch (ServiceException ex) {
      log.error("[CsOperations] Failed to send NSI CS reserve message, correlationId = {}, errorId = {}, text = {}",
              requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      this.delete(correlationId);
      throw ex;
    } catch (SOAPFaultException ex) {
      //TODO: Consider whether we should unwrap any NSI reservations that were successful.
      // For now just delete the correlationId we added.
      log.error("[CsOperations] Failed to send NSI CS reserve message, correlationId = {}, SOAP Fault = {}",
              requestHeader.getCorrelationId(), ex.getFault().toString());
      this.delete(correlationId);
      throw ex;
    }

    return correlationId;
  }

  /**
   *
   * @param connectionId
   * @return
   * @throws Exception
   */
  public String reserveCommit(String connectionId) throws Exception {
    String correlationId = Helper.getUUID();

    CommonHeaderType requestHeader = NsiHeader.builder()
        .correlationId(correlationId)
        .providerNSA(nsiProperties.getProviderNsaId())
        .requesterNSA(nsiProperties.getNsaId())
        .replyTo(nsiProperties.getRequesterConnectionURL())
        .build()
        .getRequestHeaderType();
    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;
    GenericRequestType commitBody = CS_FACTORY.createGenericRequestType();
    commitBody.setConnectionId(connectionId);

    this.store(OperationType.reserveCommit, StateType.committing, correlationId);

    try {
      log.debug("[CsOperations] issuing reserveCommit operation correlationId = {}, connectionId = {}",
              correlationId, connectionId);

      ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
      nsiClient.getProxy().reserveCommit(commitBody, header);

      log.debug("[CsOperations] issued reserveCommit operation correlationId = {}, connectionId = {}",
              correlationId, connectionId);

    } catch (ServiceException ex) {
      log.error("[CsOperations] commitDelta failed to send NSI CS reserveCommit message, correlationId = {}, errorId = {}, text = {}",
              requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      this.delete(correlationId);
      throw ex;
    } catch (SOAPFaultException soap) {
      log.error("[CsOperations] commitDelta failed to send NSI CS reserveCommit message, correlationId = {}, SOAP Fault {}",
              requestHeader.getCorrelationId(), soap.getFault().toString());
      this.delete(correlationId);
      throw soap;
    } catch (Exception ex) {
      log.error("[CsOperations] commitDelta failed to send NSI CS reserveCommit message, correlationId = {}",
              requestHeader.getCorrelationId(), ex);
      this.delete(correlationId);
      throw ex;
    }

    return correlationId;
  }

  /**
   *
   * @param connectionId
   * @return
   * @throws Exception
   */
  public String provision(String connectionId) throws Exception {
    String correlationId = Helper.getUUID();

    CommonHeaderType requestHeader = NsiHeader.builder()
            .correlationId(correlationId)
            .providerNSA(nsiProperties.getProviderNsaId())
            .requesterNSA(nsiProperties.getNsaId())
            .replyTo(nsiProperties.getRequesterConnectionURL())
            .build()
            .getRequestHeaderType();
    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;
    GenericRequestType commitBody = CS_FACTORY.createGenericRequestType();
    commitBody.setConnectionId(connectionId);

    this.store(OperationType.provision, StateType.provisioning, correlationId);

    try {
      log.debug("[CsOperations] issuing provision operation correlationId = {}, connectionId = {}",
              correlationId, connectionId);

      ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
      nsiClient.getProxy().provision(commitBody, header);

      log.debug("[CsOperations] issued provision operation correlationId = {}, connectionId = {}",
              correlationId, connectionId);

    } catch (ServiceException ex) {
      log.error("[CsOperations] Failed to send NSI CS provision message, correlationId = {}, errorId = {}, text = {}",
              requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      this.delete(correlationId);
      throw ex;
    } catch (SOAPFaultException soap) {
      log.error("[CsOperations] Failed to send NSI CS provision message, correlationId = {}, SOAP Fault {}",
              requestHeader.getCorrelationId(), soap.getFault().toString());
      this.delete(correlationId);
      throw soap;
    } catch (Exception ex) {
      log.error("[CsOperations] Failed to send NSI CS provision message, correlationId = {}",
              requestHeader.getCorrelationId(), ex);
      this.delete(correlationId);
      throw ex;
    }

    return correlationId;
  }

  /**
   *
   * @param connectionId
   * @return
   * @throws Exception
   */
  public String release(String connectionId) throws Exception {
    String correlationId = Helper.getUUID();

    CommonHeaderType requestHeader = NsiHeader.builder()
            .correlationId(correlationId)
            .providerNSA(nsiProperties.getProviderNsaId())
            .requesterNSA(nsiProperties.getNsaId())
            .replyTo(nsiProperties.getRequesterConnectionURL())
            .build()
            .getRequestHeaderType();
    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;
    GenericRequestType commitBody = CS_FACTORY.createGenericRequestType();
    commitBody.setConnectionId(connectionId);

    this.store(OperationType.release, StateType.releasing, correlationId);

    try {
      log.debug("[CsOperations] issuing release operation correlationId = {}, connectionId = {}",
              correlationId, connectionId);

      ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
      nsiClient.getProxy().release(commitBody, header);

      log.debug("[CsOperations] issued release operation correlationId = {}, connectionId = {}",
              correlationId, connectionId);

    } catch (ServiceException ex) {
      log.error("[CsOperations] Failed to send NSI CS release message, correlationId = {}, errorId = {}, text = {}",
              requestHeader.getCorrelationId(), ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      this.delete(correlationId);
      throw ex;
    } catch (SOAPFaultException soap) {
      log.error("[CsOperations] Failed to send NSI CS release message, correlationId = {}, SOAP Fault {}",
              requestHeader.getCorrelationId(), soap.getFault().toString());
      this.delete(correlationId);
      throw soap;
    } catch (Exception ex) {
      log.error("[CsOperations] Failed to send NSI CS release message, correlationId = {}",
              requestHeader.getCorrelationId(), ex);
      this.delete(correlationId);
      throw ex;
    }

    return correlationId;
  }

  /**
   *
   * @param connectionId
   * @return
   * @throws Exception
   */
  public String terminate(String connectionId) throws Exception {
    // If we have some changes apply them into the network.
    String correlationId = Helper.getUUID();
    CommonHeaderType requestHeader = NsiHeader.builder()
            .correlationId(correlationId)
            .providerNSA(nsiProperties.getProviderNsaId())
            .requesterNSA(nsiProperties.getNsaId())
            .replyTo(nsiProperties.getRequesterConnectionURL())
            .build()
            .getRequestHeaderType();

    Holder<CommonHeaderType> header = new Holder<>();
    header.value = requestHeader;

    GenericRequestType terminate = CS_FACTORY.createGenericRequestType();
    terminate.setConnectionId(connectionId);

    this.store(OperationType.terminate, StateType.terminating, correlationId);

    // Issue the NSI terminate request.
    try {
      log.debug("[CsOperations] issuing terminate operation correlationId = {}, connectionId = {}",
              correlationId, connectionId);
      ClientUtil nsiClient = new ClientUtil(nsiProperties.getProviderConnectionURL());
      nsiClient.getProxy().terminate(terminate, header);
      log.debug("[CsOperations] issued terminate operation correlationId = {}, connectionId = {}",
              correlationId, connectionId);
    } catch (ServiceException ex) {
      // Continue on this error but clean up this correlationId.
      log.error("[CsOperations] Failed to send NSI CS terminate message, correlationId = {}, errorId = {}, text = {}",
              correlationId, ex.getFaultInfo().getErrorId(), ex.getFaultInfo().getText());
      this.delete(correlationId);
      throw ex;
    } catch (SOAPFaultException ex) {
      // Continue on this error but clean up this correlationId.
      log.error("[CsOperations] Failed to send NSI CS terminate message, correlationId = {}, SOAP Fault = {}",
              correlationId, ex.getFault().getFaultCode());
      this.delete(correlationId);
      throw ex;
    } catch (Exception ex) {
      log.error("[CsOperations] Failed to send NSI CS terminate message, correlationId = {}",
              correlationId, ex);
      this.delete(correlationId);
      throw ex;
    }

    return correlationId;
  }

  /**
   *
   * @param operation
   * @param state
   * @param correlationId
   */
  private void store(OperationType operation, StateType state, String correlationId) {
    Operation op = new Operation();
    op.setOperation(operation);
    op.setState(state);
    op.setCorrelationId(correlationId);
    operationMap.store(op);
    this.addCorrelationId(correlationId);
  }

  /**
   *
   * @param correlationId
   */
  private void delete(String correlationId) {
    operationMap.delete(correlationId);
    this.removeCorrelationId(correlationId);
  }

  /**
   *
   */
  public void unwind() {
    for (String connectionId : connectionIds) {
      try {
        log.debug("[CsOperations] Unwinding connectionId {}.", connectionId);
        terminate(connectionId);
      } catch (Exception ex) {
        log.error("[CsOperations] Ignoring terminate exception", ex);
      }
    }
  }
}
