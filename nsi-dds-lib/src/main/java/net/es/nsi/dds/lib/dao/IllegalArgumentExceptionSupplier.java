package net.es.nsi.dds.lib.dao;

import java.util.Optional;
import java.util.function.Supplier;

/**
 *
 * @author hacksaw
 */
public class IllegalArgumentExceptionSupplier implements Supplier<IllegalArgumentException> {

  private Optional<String> error = Optional.empty();

  public IllegalArgumentExceptionSupplier(String error) {
    this.error = Optional.ofNullable(error);
  }

  public IllegalArgumentExceptionSupplier() {
  }

  @Override
  public IllegalArgumentException get() {
    return new IllegalArgumentException(error.orElse("Required member data missing"));

  }
}
