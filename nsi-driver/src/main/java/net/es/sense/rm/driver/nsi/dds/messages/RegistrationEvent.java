package net.es.sense.rm.driver.nsi.dds.messages;

import java.io.Serializable;

/**
 *
 * @author hacksaw
 */
public class RegistrationEvent implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum Event {

    Audit, Register, Update, Delete
  };

  private Event event;
  private String url;

  /**
   * @return the event
   */
  public Event getEvent() {
    return event;
  }

  /**
   * @param event the event to set
   */
  public void setEvent(Event event) {
    this.event = event;
  }

  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }
}
