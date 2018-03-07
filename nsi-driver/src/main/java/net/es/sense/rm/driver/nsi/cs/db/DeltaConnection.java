package net.es.sense.rm.driver.nsi.cs.db;

import java.util.ArrayList;
import java.util.List;
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
  private final List<String> commits = new ArrayList<>();
  private final List<String> terminates = new ArrayList<>();
}
