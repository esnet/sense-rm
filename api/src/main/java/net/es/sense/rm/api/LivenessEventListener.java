package net.es.sense.rm.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * This class is invoked when there is a change in application liveness availability.
 */
@Slf4j
@Component
public class LivenessEventListener {
  @Autowired
  private ApplicationAvailability applicationAvailability;

  /**
   * Catch an availability event.  Currently we just log the change.
   *
   * @param event
   */
  @EventListener
  public void onEvent(AvailabilityChangeEvent<LivenessState> event) {
    switch (event.getState()) {
      case BROKEN -> log.info("[LivenessEventListener] Liveness start set to BROKEN");
      case CORRECT -> log.info("[LivenessEventListener] Liveness start set to CORRECT");
      default -> log.info("[LivenessEventListener] Liveness invalid state {}", event.getState());
    }

    log.debug("[LivenessEventListener] ReadinessState {}, LivenessState {}",
        applicationAvailability.getReadinessState(), applicationAvailability.getLivenessState());
  }
}