package net.es.sense.rm.driver.api;

/**
 *
 * @author hacksaw
 */
@lombok.Data
@lombok.Builder
public class DeltaRequest {

  private String modelId;    // The UUID identifying the topology model resource this delta request is targetting.
  private String reduction;  // The gzipped and base64 encoded delta reduction for topology model resource specified by modelId.
  private String addition;   // The gzipped and base64 encoded delta addition for topology model resource specified by modelId.

}
