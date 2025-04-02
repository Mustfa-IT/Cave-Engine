package com.engine.pyhsics;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.Body;
import org.jbox2d.collision.shapes.PolygonShape;

import com.engine.components.Transform;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Results.With2;

public class PhysicsWorld extends World {
  private final Dominion ecs;
  private final float pixelsPerMeter = 30.0f; // Conversion factor between pixels and meters
  private final int velocityIterations = 10; // Increased from 8
  private final int positionIterations = 8; // Increased from 3
  private float accumulator = 0.0f;
  private final float timeStep = 1.0f / 60.0f; // Fixed time step for physics

  // Debug flag to monitor position changes
  private boolean debugPositions = true;

  // Whether we're in a Y-up (physics) or Y-down (screen) coordinate system
  private final boolean isYFlipped = true; // Box2D uses Y-up while screen is Y-down

  public PhysicsWorld(Vec2 gravity, Dominion ecs) {
    super(gravity);
    this.ecs = ecs;
    initializePhysicsBodies();
  }

  private void initializePhysicsBodies() {
    // Find all entities with PhysicsBodyComponent but no Body yet
    ecs.findEntitiesWith(PhysicsBodyComponent.class).stream()
        .forEach(result -> {
          PhysicsBodyComponent physics = result.comp();
          if (physics.getBody() == null) {
            // Create the body in the physics world
            Body body = createBody(physics.getBodyDef());
            body.createFixture(physics.getFixtureDef());
            physics.setBody(body);
            // Store the entity ID in the body's user data
            body.setUserData(result.entity());
          }
        });
  }

  public void update(double deltaTime) {
    // Use fixed time steps with accumulator for stable physics
    accumulator += (float) deltaTime;

    // Perform multiple sub-steps if needed
    while (accumulator >= timeStep) {
      // Step the physics simulation with fixed timestep
      this.step(timeStep, velocityIterations, positionIterations);
      accumulator -= timeStep;
    }

    // Update transforms of all entities with physics bodies
    ecs.findEntitiesWith(PhysicsBodyComponent.class, Transform.class)
        .forEach(this::updateTransformFromBody);

    // Check for new physics bodies
    initializePhysicsBodies();
  }

  private void updateTransformFromBody(With2<PhysicsBodyComponent, Transform> result) {
    PhysicsBodyComponent physics = result.comp1();
    Transform transform = result.comp2();
    Body body = physics.getBody();

    if (body != null) {
      // Update transform from physics body
      Vec2 position = body.getPosition();
      float angle = body.getAngle();

      // Convert from physics world coordinates (meters) to screen coordinates
      // (pixels)
      float screenX = position.x * pixelsPerMeter;
      float screenY = position.y * pixelsPerMeter;

      // Apply the transform update
      transform.setX(screenX);
      transform.setY(screenY);
      transform.setRotation(angle);

      // Print debug information for monitoring
      if (debugPositions && body.getUserData() != null) {
        String entityName = body.getUserData().toString();
        if (entityName.equals("ground")) {
          System.out.println("Ground physics pos: " + position.x + "," + position.y +
              " -> screen pos: " + screenX + "," + screenY);
        }
      }
    }
  }

  // Helper methods for physics body creation
  public Vec2 toPhysicsWorld(float x, float y) {
    return new Vec2(x / pixelsPerMeter, y / pixelsPerMeter);
  }

  public float toPhysicsWorld(float value) {
    return value / pixelsPerMeter;
  }

  public float fromPhysicsWorld(float v) {
    return v * pixelsPerMeter;
  }

  // Helper method to correctly set a box shape in the physics world
  public void setBoxShape(PolygonShape shape, float halfWidth, float halfHeight) {
    shape.setAsBox(toPhysicsWorld(halfWidth), toPhysicsWorld(halfHeight));
  }
}
