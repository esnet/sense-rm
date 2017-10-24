/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.cs.lib;

import com.google.common.base.Strings;
import java.util.Map;
import javax.xml.namespace.QName;
import org.ogf.schemas.nsi._2013._12.framework.headers.CommonHeaderType;
import org.ogf.schemas.nsi._2013._12.framework.headers.ObjectFactory;
import org.ogf.schemas.nsi._2013._12.framework.headers.SessionSecurityAttrType;

/**
 *
 * @author hacksaw
 */
@lombok.Builder
public class NsiHeader {
  // Header fields.
  @lombok.Builder.Default
  private String protcolVersion = "application/vnd.ogf.nsi.cs.v2.provider+soap";
  private String correlationId;
  private String replyTo;
  private String requesterNSA;
  private String providerNSA;

  // sessionSecurityAttr fields.
  private String globalUserName;
  private String userRole;

  // otherAttributes extention field.
  private Map<QName, String> otherAttributes;

  public CommonHeaderType getRequestHeaderType() {
    ObjectFactory headerFactory = new ObjectFactory();
    CommonHeaderType header = headerFactory.createCommonHeaderType();
    header.setProtocolVersion(getProtcolVersion());
    header.setCorrelationId(getCorrelationId());
    header.setReplyTo(getReplyTo());
    header.setRequesterNSA(getRequesterNSA());
    header.setProviderNSA(getProviderNSA());
    if (!Strings.isNullOrEmpty(globalUserName)) {
      SessionSecurityAttrType ssa = headerFactory.createSessionSecurityAttrType();
      ssa.setName(getGlobalUserName());
      ssa.setType(getUserRole());
      header.getSessionSecurityAttr().add(ssa);
    }

    // Add otherAttributes at some point.
    return header;
  }

  /**
   * @return the protcolVersion
   */
  public String getProtcolVersion() {
    return protcolVersion;
  }

  /**
   * @param protcolVersion the protcolVersion to set
   */
  public void setProtcolVersion(String protcolVersion) {
    this.protcolVersion = protcolVersion;
  }

  /**
   * @return the correlationId
   */
  public String getCorrelationId() {
    return correlationId;
  }

  /**
   * @param correlationId the correlationId to set
   */
  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  /**
   * @return the replyTo
   */
  public String getReplyTo() {
    return replyTo;
  }

  /**
   * @param replyTo the replyTo to set
   */
  public void setReplyTo(String replyTo) {
    this.replyTo = replyTo;
  }

  /**
   * @return the requesterNSA
   */
  public String getRequesterNSA() {
    return requesterNSA;
  }

  /**
   * @param requesterNSA the requesterNSA to set
   */
  public void setRequesterNSA(String requesterNSA) {
    this.requesterNSA = requesterNSA;
  }

  /**
   * @return the providerNSA
   */
  public String getProviderNSA() {
    return providerNSA;
  }

  /**
   * @param providerNSA the providerNSA to set
   */
  public void setProviderNSA(String providerNSA) {
    this.providerNSA = providerNSA;
  }

  /**
   * @return the globalUserName
   */
  public String getGlobalUserName() {
    return globalUserName;
  }

  /**
   * @param globalUserName the globalUserName to set
   */
  public void setGlobalUserName(String globalUserName) {
    this.globalUserName = globalUserName;
  }

  /**
   * @return the userRole
   */
  public String getUserRole() {
    return userRole;
  }

  /**
   * @param userRole the userRole to set
   */
  public void setUserRole(String userRole) {
    this.userRole = userRole;
  }

  /**
   * @return the otherAttributes
   */
  public Map<QName, String> getOtherAttributes() {
    return otherAttributes;
  }

  /**
   * @param otherAttributes the otherAttributes to set
   */
  public void setOtherAttributes(Map<QName, String> otherAttributes) {
    this.otherAttributes = otherAttributes;
  }


}
