package com.engine.events;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a game event that can be dispatched and listened to
 */
public class GameEvent {
  private final String type;
  private final Map<String, Object> data = new HashMap<>();

  public GameEvent(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public GameEvent addData(String key, Object value) {
    data.put(key, value);
    return this;
  }

  public Object getData(String key) {
    return data.get(key);
  }

  @SuppressWarnings("unchecked")
  public <T> T getData(String key, T defaultValue) {
    if (!data.containsKey(key))
      return defaultValue;

    try {
      return (T) data.get(key);
    } catch (ClassCastException e) {
      return defaultValue;
    }
  }

  public boolean hasData(String key) {
    return data.containsKey(key);
  }

  @Override
  public String toString() {
    return "GameEvent{" +
        "type='" + type + '\'' +
        ", data=" + data +
        '}';
  }
}
