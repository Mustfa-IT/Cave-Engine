package com.engine.physics;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;

/**
 * A polygon-shaped collider
 */
public class PolygonCollider implements Collider {
  private final PolygonShape shape;
  private Vec2[] vertices;

  /**
   * Creates a polygon collider with the specified vertices
   *
   * @param vertices The vertices of the polygon (up to 8 vertices)
   */
  public PolygonCollider(Vec2[] vertices) {
    this.vertices = vertices;
    this.shape = new PolygonShape();
    updateShape();
  }

  /**
   * Set the vertices of this polygon collider
   *
   * @param vertices The vertices
   */
  public void setVertices(Vec2[] vertices) {
    this.vertices = vertices;
    updateShape();
  }

  /**
   * Get the vertices of this polygon collider
   *
   * @return The vertices
   */
  public Vec2[] getVertices() {
    return vertices;
  }

  @Override
  public Shape getShape() {
    return shape;
  }

  @Override
  public void updateShape() {
    // JBox2D polygon shapes can have at most 8 vertices
    if (vertices != null && vertices.length > 0 && vertices.length <= 8) {
      shape.set(vertices, vertices.length);
    }
  }
}
