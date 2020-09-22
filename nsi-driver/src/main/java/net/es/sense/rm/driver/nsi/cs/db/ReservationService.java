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

import java.util.Collection;

/**
 *
 * @author hacksaw
 */
public interface ReservationService {

  public long getLastDiscovered();

  public Reservation getNewest();

  public Reservation store(Reservation reservation);

  public void delete(Reservation reservation);

  public void delete(long id);

  public void delete();

  public Collection<Reservation> get();

  public Collection<Reservation> getByTopologyId(String topologyId);

  public Collection<Reservation> getByGlobalReservationId(String globalReservationId);

  public Reservation get(long id);

  public Collection<Reservation> get(String connectionId);

  public Collection<Reservation> getByProviderNsa(String providerNsa);

  public Reservation get(String providerNSA, String connectionId);

}
