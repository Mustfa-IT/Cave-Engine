package com.engine.physics;

import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

/**
 * Interface for the physics system to allow for different implementations
 * and easier testing through dependency injection.
 */
public interface PhysicsSystem {
  /**
   * Update the physics simulation
   * 
   * @param deltaTime Time passed since last update in seconds
   */
  void update(double deltaTime);

  /**
   * Remove a body from the physics world
   * 
   * @param body The body to remove
   */
  void removeBody(Body body);

  /**
   * Convert a value from game units to physics units
   * 
   * @param value Value in game units
   * @return Value in physics units
   */
  float toPhysicsWorld(float value);

  /**
   * Convert coordinates from game units to physics units
   * 
   * @param x X coordinate in game units
   * @param y Y coordinate in game units
   * @return Vector with coordinates in physics units
   */
  Vec2 toPhysicsWorld(float x, float y);

  /**
   * Convert a value from physics units to game units
   * 
   * @param v Value in physics units
   * @return Value in game units
   */
  float fromPhysicsWorld(float v);

  /**
   * Set up a box shape with the specified dimensions
   * 
   * @param shape      The polygon shape to configure
   * @param halfWidth  Half-width of the box in game units
   * @param halfHeight Half-height of the box in game units
   */
  void setBoxShape(PolygonShape shape, float halfWidth, float halfHeight);
}
