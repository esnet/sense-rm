package net.es.sense.rm.driver.nsi.dds.db;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author hacksaw
 */
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Data
@Entity
@Table(name = "subscriptions")
public class Subscription implements Serializable {
  private static final long serialVersionUID = -3009157732242241606L;

  @Id
  @Basic(optional=false)
  private String ddsURL;

  @Basic(optional=false)
  private String href;
  private long created = System.currentTimeMillis();
  private long lastModified = 0;
  private long lastAudit = 0;
  private long lastSuccessfulAudit = 0;

  @Override
  public String toString() {
    return String.format(
            "Subscription[ddsURL=%s, href=%s, created=%s, lastModified=%s, lastAudit=%s, lastSuccessfulAudit=%s]",
            ddsURL, href, new Date(created), new Date(lastModified), new Date(lastAudit),
            new Date(lastSuccessfulAudit));
  }
}
