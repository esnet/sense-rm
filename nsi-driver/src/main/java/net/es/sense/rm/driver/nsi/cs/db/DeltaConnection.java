package net.es.sense.rm.driver.nsi.cs.db;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;

/**
 * Maintain a mapping from the delta request Id to the NSI connectionIds
 * related to the delta.
 *
 * @author hacksaw
 */
@Data
public class DeltaConnection {
  private String deltaId;
  private final Set<String> commits = new HashSet<>();
  private final Set<String> terminates = new HashSet<>();
}
