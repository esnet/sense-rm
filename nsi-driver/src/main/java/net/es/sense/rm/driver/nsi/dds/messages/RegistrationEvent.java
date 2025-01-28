package net.es.sense.rm.driver.nsi.dds.messages;

import java.io.Serial;
import java.io.Serializable;
import akka.actor.ActorPath;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.es.sense.rm.driver.nsi.messages.Message;

/**
 *
 * @author hacksaw
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RegistrationEvent extends Message implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  public enum Event {

    Audit, Register, Update, Delete
  };

  private Event event;
  private String url;

  public RegistrationEvent() {
    super();
  }

  public RegistrationEvent(String initiator) {
    super(initiator);
  }

  public RegistrationEvent(String initiator, ActorPath path) {
    super(initiator, path);
  }

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

  @Override
  public String toString() {
    return String.format("RegistrationEvent[event=%s, url=%s, initiator=%s, path=%s]",
        this.getEvent(), this.getUrl(), this.getInitiator(), this.getPath());
  }
}
