package com.engine.physics;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.Body;
import org.jbox2d.collision.shapes.PolygonShape;

import com.engine.components.PhysicsBodyComponent;
import com.engine.components.Transform;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Results.With2;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Logger;

@Singleton
public class PhysicsWorld extends World implements PhysicsSystem {
  private static final Logger LOGGER = Logger.getLogger(PhysicsWorld.class.getName());
  private final Dominion ecs;
  // Scale factor - increase for better physics precision
  private float worldUnitsPerMeter = 30.0f;
  private final int velocityIterations = 10;
  private final int positionIterations = 8;
  private float accumulator = 0.0f;
  private final float timeStep = 1.0f / 60.0f;

  private boolean debugPositions = true;

  @Inject
  public PhysicsWorld(Vec2 gravity, Dominion ecs) {
    // In Box2D, positive Y is up. In screen coordinates, positive Y is down.
    // We need to make gravity work correctly in screen coordinates
    super(new Vec2(gravity.x, gravity.y)); // Invert Y gravity for Box2D
    this.ecs = ecs;
    LOGGER.info("Physics world initialized with gravity: " + gravity.x + ", " + gravity.y);
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
            Body body = createBody(physics.getBodyDef());
            body.createFixture(physics.getFixtureDef());
            physics.setBody(body);
            body.setUserData(result.entity());

            // Log initial position for debugging
            System.out.println("Body created at: " + body.getPosition().x + ", " + body.getPosition().y);
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
      // Get position and angle from physics body
      Vec2 position = body.getPosition();
      float angle = body.getAngle();

      // Convert from physics world to render world
      // Note that in Box2D Y+ is up, and in screen space Y+ is down
      float worldX = position.x * worldUnitsPerMeter;
      float worldY = position.y * worldUnitsPerMeter;

      // Apply the transform update
      transform.setX(worldX);
      transform.setY(worldY);
      transform.setRotation(angle);

    }
  }

  // Helper methods for physics body creation

  @Override
  public float toPhysicsWorld(float value) {
    return value / worldUnitsPerMeter;
  }

  @Override
  public Vec2 toPhysicsWorld(float x, float y) {
    return new Vec2(x / worldUnitsPerMeter, y / worldUnitsPerMeter);
  }

  @Override
  public float fromPhysicsWorld(float v) {
    return v * worldUnitsPerMeter;
  }

  @Override
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
      try {
        // Check if the body is in the world's body list
        boolean bodyInWorld = false;
        Body currentBody = getBodyList();
        while (currentBody != null) {
          if (currentBody == body) {
            bodyInWorld = true;
            break;
          }
          currentBody = currentBody.getNext();
        }

        if (bodyInWorld) {
          destroyBody(body);
          LOGGER.fine("Physics body removed successfully");
        } else {
          LOGGER.fine("Body already removed from world");
        }
      } catch (Exception e) {
        LOGGER.warning("Error removing physics body: " + e.getMessage());
      }
    }
  }
}
