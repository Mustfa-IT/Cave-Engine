package com.engine.physics;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.Body;
import org.jbox2d.collision.shapes.PolygonShape;

import com.engine.components.PhysicsBodyComponent;
import com.engine.components.Transform;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Results.With2;

public class PhysicsWorld extends World {
  private final Dominion ecs;
  private float worldUnitsPerMeter = 30.0f;
  private final int velocityIterations = 10;
  private final int positionIterations = 8;
  private float accumulator = 0.0f;
  private final float timeStep = 1.0f / 60.0f; // Fixed time step for physics

  // Debug flag to monitor position changes
  private boolean debugPositions = true;

  public PhysicsWorld(Vec2 gravity, Dominion ecs) {
    super(gravity);
    this.ecs = ecs;
    initializePhysicsBodies();
  }

  /**
   * Updates physics bodies for all entities that have physics components
   */
  private void initializePhysicsBodies() {
    // Find all entities with PhysicsBodyComponent but no Body yet
    ecs.findEntitiesWith(PhysicsBodyComponent.class).stream()
        .forEach(result -> {
          PhysicsBodyComponent physics = result.comp();
          if (physics.getBody() == null) {
            // No try/catch for potential errors
            Body body = createBody(physics.getBodyDef());
            body.createFixture(physics.getFixtureDef());
            physics.setBody(body);
            body.setUserData(result.entity());
          }
        });
  }

  /**
   * Steps the physics simulation and updates entity transforms
   */
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
      float screenX = position.x * worldUnitsPerMeter;
      float screenY = position.y * worldUnitsPerMeter;

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

  public float toPhysicsWorld(float value) {
    return value / worldUnitsPerMeter;
  }

  public Vec2 toPhysicsWorld(float x, float y) {
    return new Vec2(x / worldUnitsPerMeter, y / worldUnitsPerMeter);
}

public float fromPhysicsWorld(float v) {
    return v * worldUnitsPerMeter;
}

  // Helper method to correctly set a box shape in the physics world
  public void setBoxShape(PolygonShape shape, float halfWidth, float halfHeight) {
    shape.setAsBox(toPhysicsWorld(halfWidth), toPhysicsWorld(halfHeight));
  }

  /**
   * Removes a body from the physics world
   *
   * @param body The body to remove
   */
  public void removeBody(Body body) {
    if (body != null) {
      destroyBody(body);
    }
  }
}
