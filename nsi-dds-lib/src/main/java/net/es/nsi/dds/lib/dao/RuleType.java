package net.es.nsi.dds.lib.dao;

import java.util.List;

@lombok.Builder
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class RuleType {
  protected List<String> dn;
  protected List<String> nsaId;
  protected AccessControlPermission access;
}
