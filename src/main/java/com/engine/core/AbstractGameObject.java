package com.engine.core;

import java.awt.Graphics2D;
import com.engine.gameobject.GameObject;
import dev.dominion.ecs.api.Entity;

/**
 * Abstract base implementation of GameObject that provides common functionality
 * and default implementations of lifecycle methods.
 */
public abstract class AbstractGameObject implements GameObject {
  protected Entity entity;

  @Override
  public void onStart(Entity entity) {
    this.entity = entity;
  }

  @Override
  public void update(double deltaTime) {
    // Default implementation does nothing
  }

  @Override
  public void render(Graphics2D g) {
    // Default implementation does nothing
  }

  @Override
  public void onDestroy() {
    // Default implementation does nothing
  }

  /**
   * Get the entity this GameObject is attached to.
   * Available after onStart() has been called.
   *
   * @return The attached entity or null if not yet attached
   */
  public Entity getEntity() {
    return entity;
  }

  /**
   * Add a component to this GameObject's entity
   *
   * @param <T>       Component type
   * @param component The component to add
   * @return This GameObject for method chaining
   */
  protected <T> AbstractGameObject addComponent(T component) {
    if (entity != null) {
      entity.add(component);
    }
    return this;
  }

  /**
   * Get a component from this GameObject's entity
   *
   * @param <T>            Component type
   * @param componentClass Class of the component to get
   * @return The component or null if not found
   */
  protected <T> T getComponent(Class<T> componentClass) {
    if (entity != null) {
      return entity.get(componentClass);
    }
    return null;
  }
}
