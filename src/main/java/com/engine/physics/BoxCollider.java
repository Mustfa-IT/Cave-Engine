package com.engine.physics;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;

/**
 * A box-shaped collider
 */
public class BoxCollider implements Collider {
  private final PolygonShape shape;
  private float width;
  private float height;

  /**
   * Creates a box collider with the specified width and height
   *
   * @param width  The width of the box
   * @param height The height of the box
   */
  public BoxCollider(float width, float height) {
    this.width = width;
    this.height = height;
    this.shape = new PolygonShape();
    updateShape();
  }

  /**
   * Set the dimensions of this box collider
   *
   * @param width  The width
   * @param height The height
   */
  public void setSize(float width, float height) {
    this.width = width;
    this.height = height;
    updateShape();
  }

  /**
   * Get the width of this box collider
   *
   * @return The width
   */
  public float getWidth() {
    return width;
  }

  /**
   * Get the height of this box collider
   *
   * @return The height
   */
  public float getHeight() {
    return height;
  }

  @Override
  public Shape getShape() {
    return shape;
  }

  @Override
  public void updateShape() {
    // We need to set half-width and half-height for Box2D
    shape.setAsBox(width / 2, height / 2);
  }
}
