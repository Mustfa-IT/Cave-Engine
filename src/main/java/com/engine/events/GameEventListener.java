package com.engine.events;

/**
 * Interface for listening to game events
 */
@FunctionalInterface
public interface GameEventListener {
  /**
   * Handle an event
   *
   * @param event The event to handle
   */
  void onEvent(GameEvent event);
}
