package com.engine.physics;

import org.jbox2d.collision.shapes.Shape;

/**
 * Base interface for all collider components
 */
public interface Collider {
  /**
   * Gets the JBox2D shape for this collider
   *
   * @return The Box2D shape
   */
  Shape getShape();

  /**
   * Updates the shape's properties if needed
   */
  void updateShape();
}
