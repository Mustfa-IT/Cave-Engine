package com.engine.physics;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.Shape;

/**
 * A circle-shaped collider
 */
public class CircleCollider implements Collider {
  private final CircleShape shape;
  private float radius;

  /**
   * Creates a circle collider with the specified radius
   *
   * @param radius The radius of the circle
   */
  public CircleCollider(float radius) {
    this.radius = radius;
    this.shape = new CircleShape();
    updateShape();
  }

  /**
   * Set the radius of this circle collider
   *
   * @param radius The radius
   */
  public void setRadius(float radius) {
    this.radius = radius;
    updateShape();
  }

  /**
   * Get the radius of this circle collider
   *
   * @return The radius
   */
  public float getRadius() {
    return radius;
  }

  @Override
  public Shape getShape() {
    return shape;
  }

  @Override
  public void updateShape() {
    shape.setRadius(radius);
  }
}
