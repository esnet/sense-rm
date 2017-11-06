package net.es.nsi.cs.lib;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author hacksaw
 */
@Slf4j
public final class SimpleStp {
  public static final String NSI_URN_SEPARATOR = ":";
  public static final String NSI_LABEL_SEPARATOR = "?";
  public static final int NSI_NETWORK_LENGTH = 6;

  private String networkId;
  private String localId;
  private Set<SimpleLabel> labels = new HashSet<>();

  private static Pattern questionPattern = Pattern.compile("\\?");
  private static Pattern colonPattern = Pattern.compile(NSI_URN_SEPARATOR);

  public SimpleStp() {
    networkId = null;
    localId = null;
  }

  public SimpleStp(String stpId, Set<SimpleLabel> labels) throws IllegalArgumentException {
    parseId(stpId);
    this.labels = labels;
  }

  public SimpleStp(String stpId, SimpleLabel label) throws IllegalArgumentException {
    parseId(stpId);
    this.labels.add(label);
  }

  public SimpleStp(String stpId) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(stpId)) {
      // An empty string gets an emtpy STP.
      return;
    }

    String[] question = questionPattern.split(stpId);
    parseId(question[0]);

    // If a question mark is present then we have to process attached label.
    if (question.length > 1) {
      // We need to parse the label.
      this.labels = SimpleLabels.fromString(question[1]);
    }
  }

  public void addStpId(String stpId) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(stpId)) {
      return;
    }

    String[] question = questionPattern.split(stpId);

    if (networkId == null) {
      parseId(question[0]);
    }

    // If a question mark is present then we have to process attached label.
    if (question.length > 1) {
      // We need to parse the label.
      try {
        this.labels.addAll(SimpleLabels.fromString(question[1]));
      } catch (IllegalArgumentException ex) {
        //log.error("SimpleStp: stpId=" + stpId, ex);
        throw new IllegalArgumentException("Invalid STP label " + stpId);
      }
    }
  }

  public boolean isUnderSpecified() {
    return labels.size() != 1;
  }

  public static String parseNetworkId(String id) {
    StringBuilder sb = new StringBuilder();
    String[] components = colonPattern.split(id);

    for (int i = 0; i < NSI_NETWORK_LENGTH && i < components.length; i++) {
      sb.append(components[i]);
      sb.append(NSI_URN_SEPARATOR);
    }

    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }

  public static String parseLocalId(String id) throws IllegalArgumentException {
    StringBuilder sb = new StringBuilder();
    String[] components = colonPattern.split(id);

    if (components.length <= NSI_NETWORK_LENGTH) {
      throw new IllegalArgumentException("STP missing local id " + id);
    }

    for (int i = NSI_NETWORK_LENGTH; i < components.length; i++) {
      sb.append(components[i]);
      sb.append(NSI_URN_SEPARATOR);
    }

    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }

  private void parseId(String id) throws IllegalArgumentException {
    StringBuilder nsb = new StringBuilder();
    String[] components = colonPattern.split(id);

    if (components.length <= NSI_NETWORK_LENGTH) {
      //log.error("SimpleStp: STP missing local identifier component, stpId=" + id);
      throw new IllegalArgumentException("STP missing local id " + id);
    }

    for (int i = 0; i < NSI_NETWORK_LENGTH && i < components.length; i++) {
      nsb.append(components[i]);
      nsb.append(NSI_URN_SEPARATOR);
    }

    nsb.deleteCharAt(nsb.length() - 1);
    this.networkId = nsb.toString();

    StringBuilder lsb = new StringBuilder();
    for (int i = NSI_NETWORK_LENGTH; i < components.length; i++) {
      lsb.append(components[i]);
      lsb.append(NSI_URN_SEPARATOR);
    }

    lsb.deleteCharAt(lsb.length() - 1);
    this.localId = lsb.toString();
  }

  /**
   * @return the id
   */
  public String getLocalId() {
    return localId;
  }

  /**
   * @param id the id to set
   */
  public void setLocalId(String id) {
    this.localId = id;
  }

  /**
   * @return the id
   */
  public String getNetworkId() {
    return networkId;
  }

  /**
   * @param id the id to set
   */
  public void setNetworkId(String id) {
    this.networkId = id;
  }

  public String getStpId() {
    StringBuilder sb = new StringBuilder(networkId);
    sb.append(NSI_URN_SEPARATOR);
    sb.append(localId);

    if (!labels.isEmpty()) {
      sb.append(NSI_LABEL_SEPARATOR);
      sb.append(SimpleLabels.toString(labels));
    }

    return sb.toString();
  }

  public String getMrmlId() {
    StringBuilder sb = new StringBuilder(networkId);
    sb.append(NSI_URN_SEPARATOR);
    sb.append(localId);
    sb.append(NSI_URN_SEPARATOR);

    labels.stream().findFirst().ifPresent(l -> {
        sb.append(l.getType());
        sb.append(NSI_URN_SEPARATOR);
        sb.append(l.getValue());
      });

    return sb.toString();
  }


  /**
   * Return the STP identifier component consisting of the networkId and localId portion.
   *
   * @return
   */
  public String getId() {
    StringBuilder sb = new StringBuilder(networkId);
    sb.append(NSI_URN_SEPARATOR);
    sb.append(localId);
    return sb.toString();
  }

  /**
   * Parse the provide STP identifier and return the STP identifier component consisting of the networkId and localId
   * portion.
   *
   * @param stpId
   * @return
   */
  public static String getId(String stpId) {
    String[] components = questionPattern.split(stpId);
    return components[0];
  }

  public List<String> getMemberStpId() {
    List<String> result = new ArrayList<>();
    if (labels.isEmpty()) {
      result.add(getId());
    } else {
      labels.stream().map((label) -> {
        StringBuilder sb = new StringBuilder(getId());
        sb.append(NSI_LABEL_SEPARATOR);
        sb.append(label.toString());
        return sb;
      }).forEach((sb) -> {
        result.add(sb.toString());
      });
    }
    return result;
  }

  /**
   * @return the labels
   */
  public Set<SimpleLabel> getLabels() {
    return Collections.unmodifiableSet(labels);
  }

  /**
   * @param labels the labels to set
   */
  public void setLabels(Set<SimpleLabel> labels) {
    this.labels.clear();
    this.labels.addAll(labels);
  }

  /**
   * Perform an intersection of the labels within this STP with the Set of provided labels.
   *
   * @param labels
   * @return true if the label set changed as a result of the intersection.
   */
  public boolean intersectLabels(Set<SimpleLabel> labels) {
    return this.labels.retainAll(labels);
  }

  public void addLabel(SimpleLabel label) {
    this.labels.add(label);
  }

  public void addLabels(Set<SimpleLabel> labels) {
    this.labels.addAll(labels);
  }

  public static SimpleStp getSimpleStp(String stpId) {
    return new SimpleStp(stpId);
  }

  @Override
  public String toString() {
    return this.getStpId();
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }

    if ((object == null) || (object.getClass() != this.getClass())) {
      return false;
    }

    SimpleStp that = (SimpleStp) object;
    String thisStpId = this.getStpId();
    String thatStpId = that.getStpId();

    if (Strings.isNullOrEmpty(thisStpId) && Strings.isNullOrEmpty(thatStpId)) {
      return true;
    }

    if (Strings.isNullOrEmpty(thisStpId) && !Strings.isNullOrEmpty(thatStpId)) {
      return false;
    }

    if (!Strings.isNullOrEmpty(thisStpId) && Strings.isNullOrEmpty(thatStpId)) {
      return false;
    }

    return thisStpId.equalsIgnoreCase(thatStpId);
  }

  @Override
  public int hashCode() {
    String thisStpId = this.getStpId();
    final int prime = 31;
    int result = 69;
    result = prime * result
            + ((thisStpId == null) ? 0 : thisStpId.hashCode());
    return result;
  }
}
