package net.es.sense.rm.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * This class is a listener for readiness events.
 */
@Slf4j
@Component
public class ReadinessEventListener {
  @Autowired
  private ApplicationAvailability applicationAvailability;

  /**
   * Event listener for the ReadinessState.
   *
   * @param event
   */
  @EventListener
  public void onEvent(AvailabilityChangeEvent<ReadinessState> event) {
    switch (event.getState()) {
      case ACCEPTING_TRAFFIC -> log.info("[ReadinessEventListener] Readiness set to ACCEPTING_TRAFFIC");
      case REFUSING_TRAFFIC -> log.info("[ReadinessEventListener] Readiness set to REFUSING_TRAFFIC");
      default -> log.info("[ReadinessEventListener] Readiness invalid state {}", event.getState());
    }

    log.debug("[ReadinessEventListener] ReadinessState {}, LivenessState {}",
        applicationAvailability.getReadinessState(), applicationAvailability.getLivenessState());
  }
}