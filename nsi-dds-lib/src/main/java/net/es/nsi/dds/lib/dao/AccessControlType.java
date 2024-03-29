package net.es.nsi.dds.lib.dao;

import java.util.List;

/**
 *
 * @author hacksaw
 */
@lombok.Builder
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class AccessControlType {
  @lombok.Builder.Default
  protected boolean enabled = false;
  protected List<RuleType> rules;
}
