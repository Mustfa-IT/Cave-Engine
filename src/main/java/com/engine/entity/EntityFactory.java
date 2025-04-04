package com.engine.entity;

import java.awt.Color;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;

import com.engine.components.PhysicsBodyComponent;
import com.engine.components.RenderableComponent;
import com.engine.components.Transform;
import com.engine.components.GameObjectComponent;
import com.engine.graph.Circle;
import com.engine.graph.Rect;
import com.engine.gameobject.GameObject;
import com.engine.physics.PhysicsWorld;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * Factory for creating game entities in world coordinates
 * where (0,0) is the center of the viewport
 */
@Singleton
public class EntityFactory {
  private static final Logger LOGGER = Logger.getLogger(EntityFactory.class.getName());
  private final Dominion ecs;
  private final PhysicsWorld physicsWorld;
  private EntityRegistrar currentRegistrar;

  @Inject
  public EntityFactory(Dominion ecs, PhysicsWorld physicsWorld) {
    this.ecs = ecs;
    this.physicsWorld = physicsWorld;
  }

  /**
   * Set the current entity registrar
   */
  public void setCurrentRegistrar(EntityRegistrar registrar) {
    this.currentRegistrar = registrar;
  }

  /**
   * Register created entity with current registrar
   */
  private Entity registerWithRegistrar(Entity entity) {
    if (currentRegistrar != null && entity != null) {
      try {
        return currentRegistrar.registerEntity(entity);
      } catch (Exception e) {
        LOGGER.warning("Error registering entity: " + e.getMessage());
      }
    }
    return entity;
  }

  /**
   * Creates a static ground platform in world coordinates
   */
  public String createGround(float x, float y, float width, float height, Color color) {
    // Create physics body definition
    BodyDef groundBodyDef = new BodyDef();
    groundBodyDef.type = BodyType.STATIC;
    groundBodyDef.position = physicsWorld.toPhysicsWorld(x, y);
    groundBodyDef.fixedRotation = true;

    // Physics shape
    PolygonShape groundShape = new PolygonShape();
    physicsWorld.setBoxShape(groundShape, width / 2, height / 2);

    // Visual representation
    Rect groundRect = new Rect(color, width, height);

    // Create transform with world coordinates
    Transform transform = new Transform(x, y, 0, 1, 1);

    // Create entity
    Entity entity = ecs.createEntity(
        "ground",
        transform,
        new RenderableComponent(groundRect),
        new PhysicsBodyComponent(groundBodyDef, groundShape, 0, 0.3f, 0.2f));

    System.out.println("Created ground at: " + x + "," + y + " with size: " + width + "x" + height);
    return registerWithRegistrar(entity).toString();
  }

  /**
   * Creates a dynamic ball in world coordinates
   */
  public String createBall(float x, float y, float radius, Color color,
      float density, float friction, float restitution) {
    // Physics body definition
    BodyDef ballBodyDef = new BodyDef();
    ballBodyDef.type = BodyType.DYNAMIC;
    ballBodyDef.position = physicsWorld.toPhysicsWorld(x, y);
    ballBodyDef.angularDamping = 0.8f;
    ballBodyDef.linearDamping = 0.1f;

    // Physics shape
    CircleShape ballShape = new CircleShape();
    ballShape.setRadius(physicsWorld.toPhysicsWorld(radius));

    // Visual representation
    Circle ballCircle = new Circle(color, radius * 2);

    // Create transform with world coordinates
    Transform transform = new Transform(x, y, 0, 1, 1);

    // Create entity with random ID
    String entityId = "ball-" + Math.round(Math.random() * 10000);

    Entity entity = ecs.createEntity(
        entityId,
        transform,
        new RenderableComponent(ballCircle),
        new PhysicsBodyComponent(ballBodyDef, ballShape, density, friction, restitution));

    System.out.println("Created ball at: " + x + "," + y + " with radius: " + radius);
    return registerWithRegistrar(entity).toString();
  }

  public String createRect(float x, float y, float width, float height, Color color,
      float density, float friction, float restitution) {
    // Physics body definition
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = BodyType.DYNAMIC;
    bodyDef.position = physicsWorld.toPhysicsWorld(x, y);

    // Physics shape
    PolygonShape shape = new PolygonShape();
    physicsWorld.setBoxShape(shape, width / 2, height / 2);

    // Visual representation
    Rect rectangle = new Rect(color, width, height);

    // Create transform with world coordinates
    Transform transform = new Transform(x, y, 0, 1, 1);

    // Create entity with random ID
    String entityId = "rect-" + Math.round(Math.random() * 10000);

    Entity entity = ecs.createEntity(
        entityId,
        transform,
        new RenderableComponent(rectangle),
        new PhysicsBodyComponent(bodyDef, shape, density, friction, restitution));

    System.out.println("Created rectangle at: " + x + "," + y + " with size: " + width + "x" + height);
    return registerWithRegistrar(entity).toString();
  }

  /**
   * Creates a custom game object entity
   *
   * @param x          X coordinate in world space
   * @param y          Y coordinate in world space
   * @param gameObject Custom GameObject implementation
   * @return Entity ID
   */
  public Entity createGameObject(float x, float y, GameObject gameObject) {
    // Create transform with world coordinates
    Transform transform = new Transform(x, y, 0, 1, 1);

    // Create entity with random ID
    String entityId = "gameobject-" + Math.round(Math.random() * 10000);

    Entity entity = ecs.createEntity(
        entityId,
        transform,
        new GameObjectComponent(gameObject));

    System.out.println("Created custom GameObject at: " + x + "," + y);
    return registerWithRegistrar(entity);
  }

  /**
   * Creates a custom physics game object
   *
   * @param x           X coordinate in world space
   * @param y           Y coordinate in world space
   * @param width       Width of the physics body
   * @param height      Height of the physics body
   * @param gameObject  Custom GameObject implementation
   * @param bodyType    Physics body type (DYNAMIC, STATIC, KINEMATIC)
   * @param density     Physics density
   * @param friction    Physics friction
   * @param restitution Physics restitution (bounciness)
   * @return Entity ID
   */
  public String createPhysicsGameObject(float x, float y, float width, float height,
      GameObject gameObject, BodyType bodyType,
      float density, float friction, float restitution) {
    // Physics body definition
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = bodyType;
    bodyDef.position = physicsWorld.toPhysicsWorld(x, y);

    // Physics shape
    PolygonShape shape = new PolygonShape();
    physicsWorld.setBoxShape(shape, width / 2, height / 2);

    // Create transform with world coordinates
    Transform transform = new Transform(x, y, 0, 1, 1);

    // Create entity with random ID
    String entityId = "physics-gameobject-" + Math.round(Math.random() * 10000);

    Entity entity = ecs.createEntity(
        entityId,
        transform,
        new GameObjectComponent(gameObject),
        new PhysicsBodyComponent(bodyDef, shape, density, friction, restitution));

    System.out.println("Created physics GameObject at: " + x + "," + y);
    return registerWithRegistrar(entity).toString();
  }

  /**
   * Add a box physics body to an existing entity
   *
   * @param entity      The entity to add physics to
   * @param width       Width of the physics body
   * @param height      Height of the physics body
   * @param bodyType    Physics body type (DYNAMIC, STATIC, KINEMATIC)
   * @param density     Physics density
   * @param friction    Physics friction
   * @param restitution Physics restitution (bounciness)
   * @return The updated entity
   */
  public Entity addBoxPhysics(Entity entity, float width, float height,
      BodyType bodyType, float density, float friction, float restitution) {
    if (entity == null) {
      LOGGER.warning("Cannot add physics to null entity");
      return null;
    }

    Transform transform = entity.get(Transform.class);
    if (transform == null) {
      LOGGER.warning("Cannot add physics to entity without Transform");
      return entity;
    }

    // Physics body definition
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = bodyType;
    bodyDef.position = physicsWorld.toPhysicsWorld((float) transform.getX(), (float) transform.getY());

    // Physics shape
    PolygonShape shape = new PolygonShape();
    physicsWorld.setBoxShape(shape, width / 2, height / 2);

    // Add physics component to entity
    entity.add(new PhysicsBodyComponent(bodyDef, shape, density, friction, restitution));

    System.out.println("Added box physics to entity at: " + transform.getX() + "," + transform.getY());
    return entity;
  }

  /**
   * Add a circle physics body to an existing entity
   *
   * @param entity      The entity to add physics to
   * @param radius      Radius of the physics circle
   * @param bodyType    Physics body type (DYNAMIC, STATIC, KINEMATIC)
   * @param density     Physics density
   * @param friction    Physics friction
   * @param restitution Physics restitution (bounciness)
   * @return The updated entity
   */
  public Entity addCirclePhysics(Entity entity, float radius,
      BodyType bodyType, float density, float friction, float restitution) {
    if (entity == null) {
      LOGGER.warning("Cannot add physics to null entity");
      return null;
    }

    Transform transform = entity.get(Transform.class);
    if (transform == null) {
      LOGGER.warning("Cannot add physics to entity without Transform");
      return entity;
    }

    // Physics body definition
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = bodyType;
    bodyDef.position = physicsWorld.toPhysicsWorld((float) transform.getX(), (float) transform.getY());

    // Physics shape
    CircleShape shape = new CircleShape();
    shape.setRadius(physicsWorld.toPhysicsWorld(radius));

    // Add physics component to entity
    entity.add(new PhysicsBodyComponent(bodyDef, shape, density, friction, restitution));

    System.out.println("Added circle physics to entity at: " + transform.getX() + "," + transform.getY());
    return entity;
  }

  /**
   * Creates a physics-enabled GameObject entity with specified parameters
   *
   * @param x          X coordinate in world space
   * @param y          Y coordinate in world space
   * @param gameObject Custom GameObject implementation
   * @return Created entity
   */
  public Entity createGameObject(float x, float y, GameObject gameObject, PhysicsParameters physicsParams) {
    // First create the basic GameObject entity
    Entity entity = createGameObject(x, y, gameObject);

    // Add physics if parameters are provided
    if (physicsParams != null) {
      if (physicsParams.isCircle) {
        addCirclePhysics(entity, physicsParams.width / 2,
            physicsParams.bodyType, physicsParams.density,
            physicsParams.friction, physicsParams.restitution);
      } else {
        addBoxPhysics(entity, physicsParams.width, physicsParams.height,
            physicsParams.bodyType, physicsParams.density,
            physicsParams.friction, physicsParams.restitution);
      }

      // Apply collision filtering settings if specified
      PhysicsBodyComponent physicsComponent = entity.get(PhysicsBodyComponent.class);
      if (physicsComponent != null) {
        if (physicsParams.isSensor) {
          physicsComponent.setSensor(true);
        }
        if (physicsParams.collisionCategory != 0) {
          physicsComponent.setCollisionCategory(physicsParams.collisionCategory);
        }
        if (physicsParams.collisionMask != 0) {
          physicsComponent.setCollisionMask(physicsParams.collisionMask);
        }
      }
    }

    return entity;
  }

  /**
   * Represents physics parameters for creating physics-enabled GameObjects
   */
  public static class PhysicsParameters {
    private final BodyType bodyType;
    private final float width;
    private final float height;
    private final float density;
    private final float friction;
    private final float restitution;
    private final boolean isCircle;
    // New collision properties
    private boolean isSensor = false;
    private short collisionCategory = 0x0001; // Default category
    private short collisionMask = -1; // Collide with everything by default

    /**
     * Create box physics parameters
     */
    public static PhysicsParameters box(float width, float height, BodyType bodyType,
        float density, float friction, float restitution) {
      return new PhysicsParameters(width, height, bodyType, density, friction, restitution, false);
    }

    /**
     * Create circle physics parameters
     */
    public static PhysicsParameters circle(float diameter, BodyType bodyType,
        float density, float friction, float restitution) {
      return new PhysicsParameters(diameter, diameter, bodyType, density, friction, restitution, true);
    }

    private PhysicsParameters(float width, float height, BodyType bodyType,
        float density, float friction, float restitution, boolean isCircle) {
      this.width = width;
      this.height = height;
      this.bodyType = bodyType;
      this.density = density;
      this.friction = friction;
      this.restitution = restitution;
      this.isCircle = isCircle;
    }

    /**
     * Set this body as a sensor (triggers collisions but has no physical response)
     * 
     * @param isSensor True if this body should be a sensor
     * @return This parameters instance for method chaining
     */
    public PhysicsParameters setSensor(boolean isSensor) {
      this.isSensor = isSensor;
      return this;
    }

    /**
     * Set the collision category bits (which collision group this body belongs to)
     * 
     * @param category The category bits
     * @return This parameters instance for method chaining
     */
    public PhysicsParameters setCollisionCategory(short category) {
      this.collisionCategory = category;
      return this;
    }

    /**
     * Set the collision mask bits (which collision groups this body should collide
     * with)
     * 
     * @param mask The mask bits
     * @return This parameters instance for method chaining
     */
    public PhysicsParameters setCollisionMask(short mask) {
      this.collisionMask = mask;
      return this;
    }
  }
}
