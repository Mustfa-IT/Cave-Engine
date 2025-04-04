package com.engine.components;

import com.engine.gameobject.GameObject;
import dev.dominion.ecs.api.Entity;

/**
 * Component that attaches a custom GameObject to an entity
 */
public class GameObjectComponent {
  private final GameObject gameObject;
  private boolean initialized = false;
  private boolean destroyed = false;

  public GameObjectComponent(GameObject gameObject) {
    this.gameObject = gameObject;
  }

  public GameObject getGameObject() {
    return gameObject;
  }

  /**
   * Initialize the GameObject if not already initialized
   *
   * @param entity The entity this component is attached to
   */
  public void initIfNeeded(Entity entity) {
    if (!initialized && !destroyed) {
      gameObject.onStart(entity);
      initialized = true;
    }
  }

  /**
   * Mark this GameObject as destroyed
   * Ensures we don't try to update or render it anymore
   */
  public void markDestroyed() {
    if (!destroyed) {
      destroyed = true;
      if (gameObject != null) {
        gameObject.onDestroy();
      }
    }
  }

  /**
   * Check if this GameObject has been destroyed
   */
  public boolean isDestroyed() {
    return destroyed;
  }
}
