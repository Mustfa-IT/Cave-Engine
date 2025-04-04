package com.engine.gameobject;

import java.awt.Graphics2D;
import dev.dominion.ecs.api.Entity;
import com.engine.physics.Collision;

/**
 * Interface for custom game objects that receive lifecycle notifications
 */
public interface GameObject {
  /**
   * Called once when the GameObject is created
   *
   * @param entity The entity this GameObject is attached to
   */
  void onStart(Entity entity);

  /**
   * Called every frame to update game logic
   *
   * @param deltaTime Time since last update in seconds
   */
  void update(double deltaTime);

  /**
   * Called every frame to render the GameObject
   *
   * @param g Graphics context to render to
   */
  void render(Graphics2D g);

  /**
   * Called when the GameObject is being destroyed
   */
  void onDestroy();

  /**
   * Called when this GameObject begins colliding with another entity
   * Default implementation does nothing
   *
   * @param collision The collision data
   */
  default void onCollisionEnter(Collision collision) {
    // Default implementation does nothing
  }

  /**
   * Called when this GameObject stops colliding with another entity
   * Default implementation does nothing
   *
   * @param collision The collision data
   */
  default void onCollisionExit(Collision collision) {
    // Default implementation does nothing
  }
}
