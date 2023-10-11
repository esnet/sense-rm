package net.es.sense.rm.driver.api;

import net.es.sense.rm.model.ModelResource;

import java.util.Optional;

/**
 *
 * @author hacksaw
 */
public class ModelResponse extends ResourceResponse {
  Optional<ModelResource> model = Optional.empty();

  public ModelResponse() {
    super();
  }

  public void setModel(ModelResource model) {
    this.model = Optional.ofNullable(model);
  }

  public void setModel(Optional<ModelResource> model) {
    this.model = model;
  }

  public Optional<ModelResource> getModel() {
    return this.model;
  }
}
