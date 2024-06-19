package net.es.sense.rm.driver.nsi.mrml;

import jakarta.ws.rs.NotFoundException;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * This class is used to supply a NotFoundException in a lambda expression.
 *
 * @author hacksaw
 */
public class NotFoundExceptionSupplier implements Supplier<NotFoundException> {

  private Optional<String> error = Optional.empty();

  public NotFoundExceptionSupplier(String error) {
    this.error = Optional.ofNullable(error);
  }

  public NotFoundExceptionSupplier() {
  }

  @Override
  public NotFoundException get() {
    return new NotFoundException(error.orElse("Required member data missing"));

  }
}
