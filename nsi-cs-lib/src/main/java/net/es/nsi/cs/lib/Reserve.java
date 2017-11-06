package net.es.nsi.cs.lib;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.common.util.XmlDate;
import org.ogf.schemas.nsi._2013._12.connection.types.ObjectFactory;
import org.ogf.schemas.nsi._2013._12.connection.types.ReservationRequestCriteriaType;
import org.ogf.schemas.nsi._2013._12.connection.types.ReserveType;
import org.ogf.schemas.nsi._2013._12.connection.types.ScheduleType;

/**
 *
 * @author hacksaw
 */
public class Reserve extends NsiOperation {

  private String description;
  private String sourceStpId;
  private String destStpId;
  private int serviceBandwidth;
  private long startTime;
  private long endTime;

  public ReserveType getReserveType() throws DatatypeConfigurationException {

    ObjectFactory factory = new ObjectFactory();
    ReserveType reserve = factory.createReserveType();

    reserve.setConnectionId(this.getConnectionId());
    reserve.setDescription(getDescription());
    reserve.setGlobalReservationId(getGlobalReservationId());

    ScheduleType sch = factory.createScheduleType();
    if (getStartTime() != 0) {
      XMLGregorianCalendar start = XmlDate.longToXMLGregorianCalendar(getStartTime());
      sch.setStartTime(factory.createScheduleTypeStartTime(start));
    }

    if (getEndTime() != 0) {
      XMLGregorianCalendar end = XmlDate.longToXMLGregorianCalendar(getEndTime());
      sch.setEndTime(factory.createScheduleTypeEndTime(end));
    }

    ReservationRequestCriteriaType criteriaType = factory.createReservationRequestCriteriaType();
    criteriaType.setSchedule(sch);
    criteriaType.setServiceType("poop");
    criteriaType.setVersion(0);
    criteriaType.getAny().add("Stick p2p structure here");
    reserve.setCriteria(criteriaType);

    return reserve;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the sourceStpId
   */
  public String getSourceStpId() {
    return sourceStpId;
  }

  /**
   * @param sourceStpId the sourceStpId to set
   */
  public void setSourceStpId(String sourceStpId) {
    this.sourceStpId = sourceStpId;
  }

  /**
   * @return the destStpId
   */
  public String getDestStpId() {
    return destStpId;
  }

  /**
   * @param destStpId the destStpId to set
   */
  public void setDestStpId(String destStpId) {
    this.destStpId = destStpId;
  }

  /**
   * @return the serviceBandwidth
   */
  public int getServiceBandwidth() {
    return serviceBandwidth;
  }

  /**
   * @param serviceBandwidth the serviceBandwidth to set
   */
  public void setServiceBandwidth(int serviceBandwidth) {
    this.serviceBandwidth = serviceBandwidth;
  }

  /**
   * @return the startTime
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * @param startTime the startTime to set
   */
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  /**
   * @return the endTime
   */
  public long getEndTime() {
    return endTime;
  }

  /**
   * @param endTime the endTime to set
   */
  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }


}
