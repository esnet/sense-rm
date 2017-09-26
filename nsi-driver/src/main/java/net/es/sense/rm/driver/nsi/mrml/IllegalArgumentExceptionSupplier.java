/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.sense.rm.driver.nsi.mrml;

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
