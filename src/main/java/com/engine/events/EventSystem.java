package com.engine.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;

/**
 * A central event system that allows components to communicate
 * without direct dependencies.
 */
@Singleton
public class EventSystem {
  private static final Logger LOGGER = Logger.getLogger(EventSystem.class.getName());

  private final Map<String, List<GameEventListener>> listeners = new ConcurrentHashMap<>();
  private final List<GameEvent> eventQueue = new ArrayList<>();
  private final Map<String, Object> globalState = new HashMap<>();

  /**
   * Register a listener for a specific event type
   *
   * @param eventType The event type to listen for
   * @param listener  The listener to be called when event is fired
   */
  public void addEventListener(String eventType, GameEventListener listener) {
    listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    LOGGER.fine("Added listener for event type: " + eventType);
  }

  /**
   * Remove a listener for a specific event type
   *
   * @param eventType The event type
   * @param listener  The listener to remove
   */
  public void removeEventListener(String eventType, GameEventListener listener) {
    if (listeners.containsKey(eventType)) {
      listeners.get(eventType).remove(listener);
      LOGGER.fine("Removed listener for event type: " + eventType);
    }
  }

  /**
   * Fire an event immediately
   *
   * @param event The event to fire
   */
  public void fireEvent(GameEvent event) {
    if (listeners.containsKey(event.getType())) {
      for (GameEventListener listener : listeners.get(event.getType())) {
        try {
          listener.onEvent(event);
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Error in event listener", e);
        }
      }
    }
  }

  /**
   * Queue an event to be processed on the next update
   *
   * @param event The event to queue
   */
  public void queueEvent(GameEvent event) {
    synchronized (eventQueue) {
      eventQueue.add(event);
    }
  }

  /**
   * Process all queued events
   */
  public void processEvents() {
    List<GameEvent> currentEvents;

    synchronized (eventQueue) {
      if (eventQueue.isEmpty())
        return;

      currentEvents = new ArrayList<>(eventQueue);
      eventQueue.clear();
    }

    for (GameEvent event : currentEvents) {
      fireEvent(event);
    }
  }

  /**
   * Store a value in the global state
   *
   * @param key   The key
   * @param value The value to store
   */
  public void setState(String key, Object value) {
    globalState.put(key, value);
  }

  /**
   * Get a value from the global state
   *
   * @param key The key
   * @return The stored value or null if not present
   */
  public Object getState(String key) {
    return globalState.get(key);
  }

  /**
   * Get a typed value from the global state
   *
   * @param <T>          The type to cast to
   * @param key          The key
   * @param defaultValue The default value if key is not found
   * @return The stored value cast to the specified type or the default value
   */
  @SuppressWarnings("unchecked")
  public <T> T getState(String key, T defaultValue) {
    if (!globalState.containsKey(key))
      return defaultValue;

    try {
      return (T) globalState.get(key);
    } catch (ClassCastException e) {
      LOGGER.warning("Failed to cast state value for key: " + key);
      return defaultValue;
    }
  }
}
