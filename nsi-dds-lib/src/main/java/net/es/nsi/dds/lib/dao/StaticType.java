package net.es.nsi.dds.lib.dao;

/**
 * This class holds a static configuration path.
 *
 * @author hacksaw
 */
@lombok.Builder
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class StaticType {
  private String path;
  private String relative;
}
