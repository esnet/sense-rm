package net.es.sense.rm.driver.nsi.mrml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author hacksaw
 */
public class DeltaHolder {
  private final String deltaId;
  private final Map<String, String> nsi2mrs = new HashMap<>();
  private final Map<String, String> mrs2nsi = new HashMap<>();
  private final List<ReserveHolder> reserveList = new ArrayList<>();

  public DeltaHolder(String deltaId) {
    this.deltaId = deltaId;
  }

  public String getDeltaId() {
    return deltaId;
  }

  public void addMapping(String nsi, String mrs) {
    nsi2mrs.put(nsi, mrs);
    mrs2nsi.put(mrs, nsi);
  }

  public String getNsiId(String mrs) {
    return mrs2nsi.get(mrs);
  }

  public String getMrsId(String nsi) {
    return nsi2mrs.get(nsi);
  }

  public Map<String, String> getNsi2mrs() {
    return nsi2mrs;
  }

  public Map<String, String> getMrs2nsi() {
    return mrs2nsi;
  }

  public boolean addReserve(ReserveHolder reserve) {
    return reserveList.add(reserve);
  }

  public List<ReserveHolder> getReserveList() {
    return reserveList;
  }
 }
