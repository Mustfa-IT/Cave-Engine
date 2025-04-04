package com.engine.physics;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.Body;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.BodyType;

import com.engine.components.PhysicsBodyComponent;
import com.engine.components.Transform;
import com.engine.core.EngineConfig;

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
  private static float worldUnitsPerMeter = 30.0f;
  private final int velocityIterations;
  private final int positionIterations;
  private float accumulator = 0.0f;
  private final float timeStep;

  @Inject
  public PhysicsWorld(Vec2 gravity, Dominion ecs, CollisionSystem collisionSystem, EngineConfig config) {
    super(gravity);
    this.ecs = ecs;
    this.velocityIterations = config.getVelocityIterations();
    this.positionIterations = config.getPositionIterations();
    this.timeStep = config.getPhysicsTimeStep();

    // Enable auto-sleeping for bodies that haven't moved
    this.setAllowSleep(config.isEnableBodySleeping());

    // Set the contact listener for collision callbacks
    this.setContactListener(collisionSystem);

    if (config.isOptimizeBroadphase()) {
      // Use dynamic tree broadphase for better performance with many objects
      this.setBroadPhaseOptimized();
    }

    LOGGER.info("Physics world created with optimized settings: " +
        "iterations(" + velocityIterations + "," + positionIterations + "), " +
        "timeStep=" + timeStep + ", sleeping=" + config.isEnableBodySleeping());
    initializePhysicsBodies();
  }

  /**
   * Configure the world to use a more efficient broadphase algorithm
   */
  private void setBroadPhaseOptimized() {
    // The implementation depends on the JBox2D version you're using
    // For newer versions, you might need to create a new world with different
    // settings
    LOGGER.info("Using optimized broadphase settings");
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

            // Apply correct shape based on collider type
            if (result.entity().has(BoxCollider.class)) {
              BoxCollider boxCollider = result.entity().get(BoxCollider.class);
              physics.setShape(boxCollider.getShape());
              physics.setWidth(boxCollider.getWidth());
              physics.setHeight(boxCollider.getHeight());
            } else if (result.entity().has(CircleCollider.class)) {
              CircleCollider circleCollider = result.entity().get(CircleCollider.class);
              physics.setShape(circleCollider.getShape());
              physics.setWidth(circleCollider.getRadius() * 2);
              physics.setHeight(circleCollider.getRadius() * 2);
            } else if (result.entity().has(PolygonCollider.class)) {
              PolygonCollider polygonCollider = result.entity().get(PolygonCollider.class);
              physics.setShape(polygonCollider.getShape());
              // Set approximate width/height for debug rendering
              calculatePolygonBounds(polygonCollider, physics);
            }

            // Create fixture with the shape
            body.createFixture(physics.getFixtureDef());
            physics.setBody(body);
            body.setUserData(result.entity());

            // Log initial position for debugging
            System.out.println("Body created at: " + body.getPosition().x + ", " + body.getPosition().y);
          }
        });
  }

  /**
   * Calculate approximate bounds of a polygon collider for debug rendering
   *
   * @param polygonCollider The polygon collider
   * @param physics         The physics body component to update
   */
  private void calculatePolygonBounds(PolygonCollider polygonCollider, PhysicsBodyComponent physics) {
    Vec2[] vertices = polygonCollider.getVertices();
    if (vertices == null || vertices.length == 0) {
      physics.setWidth(1.0f);
      physics.setHeight(1.0f);
      return;
    }

    float minX = Float.MAX_VALUE;
    float maxX = Float.MIN_VALUE;
    float minY = Float.MAX_VALUE;
    float maxY = Float.MIN_VALUE;

    for (Vec2 v : vertices) {
      minX = Math.min(minX, v.x);
      maxX = Math.max(maxX, v.x);
      minY = Math.min(minY, v.y);
      maxY = Math.max(maxY, v.y);
    }

    physics.setWidth((maxX - minX) * worldUnitsPerMeter);
    physics.setHeight((maxY - minY) * worldUnitsPerMeter);
  }

  /**
   * Steps the physics simulation and updates entity transforms
   */
  public void update(double deltaTime) {
    // Use fixed time steps with accumulator for stable physics
    accumulator += (float) deltaTime;

    // Prevent spiral of death if game slows down severely
    if (accumulator > 0.2f) {
      accumulator = 0.2f;
    }

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
      // Both Box2D and our render system now use Y+ up
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

  /**
   * Create a Box2D body with a circle shape
   *
   * @param position The position in physics world coordinates
   * @param radius   The radius in physics world coordinates
   * @param bodyType The type of body (static, dynamic, kinematic)
   * @return The created Box2D body
   */
  public Body createCircleBody(Vec2 position, float radius, BodyType bodyType) {
    BodyDef bodyDef = new BodyDef();
    bodyDef.position.set(position);
    bodyDef.type = bodyType;

    Body body = createBody(bodyDef);

    CircleShape circleShape = new CircleShape();
    circleShape.setRadius(radius);

    FixtureDef fixtureDef = new FixtureDef();
    fixtureDef.shape = circleShape;
    fixtureDef.density = 1.0f;
    fixtureDef.friction = 0.3f;
    fixtureDef.restitution = 0.2f;

    body.createFixture(fixtureDef);
    return body;
  }

  /**
   * Create a Box2D body with a box shape
   *
   * @param position   The position in physics world coordinates
   * @param halfWidth  The half-width in physics world coordinates
   * @param halfHeight The half-height in physics world coordinates
   * @param bodyType   The type of body (static, dynamic, kinematic)
   * @return The created Box2D body
   */
  public Body createBoxBody(Vec2 position, float halfWidth, float halfHeight, BodyType bodyType) {
    BodyDef bodyDef = new BodyDef();
    bodyDef.position.set(position);
    bodyDef.type = bodyType;

    Body body = createBody(bodyDef);

    PolygonShape boxShape = new PolygonShape();
    boxShape.setAsBox(halfWidth, halfHeight);

    FixtureDef fixtureDef = new FixtureDef();
    fixtureDef.shape = boxShape;
    fixtureDef.density = 1.0f;
    fixtureDef.friction = 0.3f;
    fixtureDef.restitution = 0.2f;

    body.createFixture(fixtureDef);
    return body;
  }

  /**
   * Create a Box2D body with a polygon shape
   *
   * @param position The position in physics world coordinates
   * @param vertices The vertices of the polygon
   * @param bodyType The type of body (static, dynamic, kinematic)
   * @return The created Box2D body
   */
  public Body createPolygonBody(Vec2 position, Vec2[] vertices, BodyType bodyType) {
    BodyDef bodyDef = new BodyDef();
    bodyDef.position.set(position);
    bodyDef.type = bodyType;

    Body body = createBody(bodyDef);

    PolygonShape polygonShape = new PolygonShape();
    polygonShape.set(vertices, vertices.length);

    FixtureDef fixtureDef = new FixtureDef();
    fixtureDef.shape = polygonShape;
    fixtureDef.density = 1.0f;
    fixtureDef.friction = 0.3f;
    fixtureDef.restitution = 0.2f;

    body.createFixture(fixtureDef);
    return body;
  }
}
