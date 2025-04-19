package com.engine.entity;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;

import com.engine.components.PhysicsBodyComponent;
import com.engine.components.RenderableComponent;
import com.engine.components.RigidBody;
import com.engine.components.Transform;
import com.engine.components.GameObjectComponent;
import com.engine.components.SpriteComponent;
import com.engine.components.SpriteAnimationComponent;
import com.engine.graph.Circle;
import com.engine.graph.Rect;
import com.engine.gameobject.GameObject;
import com.engine.physics.PhysicsWorld;
import com.engine.physics.BoxCollider;
import com.engine.physics.CircleCollider;
import com.engine.physics.Collider;
import com.engine.physics.PolygonCollider;
import com.engine.animation.Animation;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * Factory for creating game entities in world coordinates
 * where (0,0) is the center of the viewport
 */
@Singleton
public class EntityFactory {
  /** Logger for entity factory messages */
  private static final Logger LOGGER = Logger.getLogger(EntityFactory.class.getName());

  /** Entity Component System for managing entities */
  private final Dominion ecs;

  /** Physics world for physics calculations and conversions */
  private final PhysicsWorld physicsWorld;

  /** Current entity registrar for registering created entities */
  private EntityRegistrar currentRegistrar;

  /**
   * Creates a new EntityFactory
   *
   * @param ecs          The Entity Component System
   * @param physicsWorld The physics world
   */
  @Inject
  public EntityFactory(Dominion ecs, PhysicsWorld physicsWorld) {
    this.ecs = ecs;
    this.physicsWorld = physicsWorld;
  }

  /**
   * Set the current entity registrar
   *
   * @param registrar The entity registrar to use
   */
  public void setCurrentRegistrar(EntityRegistrar registrar) {
    this.currentRegistrar = registrar;
  }

  /**
   * Register created entity with current registrar
   *
   * @param entity The entity to register
   * @return The registered entity
   */
  private Entity registerWithRegistrar(Entity entity) {
    if (currentRegistrar != null && entity != null) {
      try {
        return currentRegistrar.registerEntity(entity);
      } catch (Exception e) {
        LOGGER.warning("Error registering entity: " + e.getMessage());
      }
    }
    // Fixed infinite recursion bug - just return the entity if no registrar or
    // error
    return entity;
  }

  /**
   * Validates that a dimensional parameter is positive
   *
   * @param value The value to check
   * @param name  The parameter name for error reporting
   * @throws IllegalArgumentException if value is not positive
   */
  private void validatePositive(float value, String name) {
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive: " + value);
    }
  }

  /**
   * Creates a physics body definition
   *
   * @param x        X position
   * @param y        Y position
   * @param bodyType Type of physics body
   * @return The body definition
   */
  private BodyDef createBodyDef(float x, float y, BodyType bodyType) {
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = bodyType;
    bodyDef.position = physicsWorld.toPhysicsWorld(x, y);
    return bodyDef;
  }

  /**
   * Creates a static ground platform in world coordinates
   *
   * @param x      X position
   * @param y      Y position
   * @param width  Width of the ground
   * @param height Height of the ground
   * @param color  Color of the ground
   * @return The created entity
   * @throws IllegalArgumentException if width or height is not positive
   */
  public Entity createGround(float x, float y, float width, float height, Color color) {
    validatePositive(width, "Width");
    validatePositive(height, "Height");

    // Create physics body definition
    BodyDef groundBodyDef = createBodyDef(x, y, BodyType.STATIC);
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

    LOGGER.fine("Created ground at: " + x + "," + y + " with size: " + width + "x" + height);
    return registerWithRegistrar(entity);
  }

  /**
   * Creates a dynamic ball in world coordinates
   *
   * @param x           X position
   * @param y           Y position
   * @param radius      Radius of the ball
   * @param color       Color of the ball
   * @param density     Physics density
   * @param friction    Physics friction
   * @param restitution Physics restitution (bounciness)
   * @return The created entity
   * @throws IllegalArgumentException if radius is not positive
   */
  public Entity createBall(float x, float y, float radius, Color color,
      float density, float friction, float restitution) {
    validatePositive(radius, "Radius");
    validatePositive(density, "Density");

    // Physics body definition
    BodyDef ballBodyDef = createBodyDef(x, y, BodyType.DYNAMIC);
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

    LOGGER.fine("Created ball at: " + x + "," + y + " with radius: " + radius);
    return registerWithRegistrar(entity);
  }

  /**
   * Creates a rectangular entity with physics
   *
   * @param x           X position
   * @param y           Y position
   * @param width       Width of the rectangle
   * @param height      Height of the rectangle
   * @param color       Color of the rectangle
   * @param density     Physics density
   * @param friction    Physics friction
   * @param restitution Physics restitution (bounciness)
   * @return The created entity
   * @throws IllegalArgumentException if width or height is not positive
   */
  public Entity createRect(float x, float y, float width, float height, Color color,
      float density, float friction, float restitution) {
    validatePositive(width, "Width");
    validatePositive(height, "Height");
    validatePositive(density, "Density");

    // Physics body definition
    BodyDef bodyDef = createBodyDef(x, y, BodyType.DYNAMIC);

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

    LOGGER.fine("Created rectangle at: " + x + "," + y + " with size: " + width + "x" + height);
    return registerWithRegistrar(entity);
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
   * @return The created entity
   * @throws IllegalArgumentException if width or height is not positive
   */
  public Entity createPhysicsGameObject(float x, float y, float width, float height,
      GameObject gameObject, BodyType bodyType, float density, float friction, float restitution) {
    validatePositive(width, "Width");
    validatePositive(height, "Height");
    if (gameObject == null) {
      throw new IllegalArgumentException("GameObject cannot be null");
    }

    // Physics body definition
    BodyDef bodyDef = createBodyDef(x, y, bodyType);

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

    LOGGER.fine("Created physics GameObject at: " + x + "," + y);
    return registerWithRegistrar(entity);
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
   * Creates a sprite entity
   *
   * @param x     X coordinate in world space
   * @param y     Y coordinate in world space
   * @param image Image to render
   * @return Created entity
   */
  public Entity createSprite(float x, float y, BufferedImage image) {
    // Create transform with world coordinates
    Transform transform = new Transform(x, y, 0, 1, 1);

    // Create entity with random ID
    String entityId = "sprite-" + Math.round(Math.random() * 10000);

    // Create sprite component
    SpriteComponent spriteComponent = new SpriteComponent(image);

    Entity entity = ecs.createEntity(
        entityId,
        transform,
        spriteComponent);

    System.out.println("Created sprite at: " + x + "," + y);
    return registerWithRegistrar(entity);
  }

  /**
   * Creates an animated sprite entity
   *
   * @param x            X coordinate in world space
   * @param y            Y coordinate in world space
   * @param defaultImage Default image to show
   * @param animations   Array of animations to add
   * @return Created entity
   */
  public Entity createAnimatedSprite(float x, float y, BufferedImage defaultImage, Animation... animations) {
    // Create transform with world coordinates
    Transform transform = new Transform(x, y, 0, 1, 1);

    // Create entity with random ID
    String entityId = "animated-sprite-" + Math.round(Math.random() * 10000);

    // Create sprite component
    SpriteComponent spriteComponent = new SpriteComponent(defaultImage);

    // Create animation component
    SpriteAnimationComponent animComponent = new SpriteAnimationComponent();

    // Add animations
    if (animations != null) {
      for (Animation anim : animations) {
        if (anim != null) {
          animComponent.addAnimation(anim);
        }
      }
    }

    Entity entity = ecs.createEntity(
        entityId,
        transform,
        spriteComponent,
        animComponent);

    System.out.println("Created animated sprite at: " + x + "," + y);
    return registerWithRegistrar(entity);
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

  /**
   * Creates a static box entity
   *
   * @param x      X position
   * @param y      Y position
   * @param width  Width of the box
   * @param height Height of the box
   * @param color  Color of the box
   * @return The created entity
   */
  public Entity createStaticBox(float x, float y, float width, float height, Color color) {
    return createBox(x, y, width, height, RigidBody.Type.STATIC, color);
  }

  /**
   * Creates a dynamic box entity
   *
   * @param x      X position
   * @param y      Y position
   * @param width  Width of the box
   * @param height Height of the box
   * @param color  Color of the box
   * @return The created entity
   */
  public Entity createDynamicBox(float x, float y, float width, float height, Color color) {
    return createBox(x, y, width, height, RigidBody.Type.DYNAMIC, color);
  }

  /**
   * Creates a box entity with the specified rigid body type
   *
   * @param x        X position
   * @param y        Y position
   * @param width    Width of the box
   * @param height   Height of the box
   * @param bodyType Type of physics body
   * @param color    Color of the box
   * @return The created entity
   * @throws IllegalArgumentException if width or height is not positive
   */
  public Entity createBox(float x, float y, float width, float height, RigidBody.Type bodyType, Color color) {
    validatePositive(width, "Width");
    validatePositive(height, "Height");
    if (color == null) {
      throw new IllegalArgumentException("Color cannot be null");
    }

    // Create box collider
    BoxCollider boxCollider = new BoxCollider(width, height);

    // Create body definition
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = RigidBody.toBox2DBodyType(bodyType);
    bodyDef.position.set(physicsWorld.toPhysicsWorld(x), physicsWorld.toPhysicsWorld(y));

    // Create entity
    Entity entity = ecs.createEntity(
        "box_" + System.currentTimeMillis(),
        new Transform(x, y, 0, 1, 1),
        new RenderableComponent(new Rect(color, width, height)),
        boxCollider,
        new PhysicsBodyComponent(
            bodyDef,
            boxCollider.getShape(),
            bodyType == RigidBody.Type.STATIC ? 0.0f : 1.0f,
            0.3f,
            0.2f));

    return registerWithRegistrar(entity);
  }

  /**
   * Creates a static circle entity
   */
  public Entity createStaticCircle(float x, float y, float radius, Color color) {
    return createCircle(x, y, radius, RigidBody.Type.STATIC, color);
  }

  /**
   * Creates a dynamic circle entity
   */
  public Entity createDynamicCircle(float x, float y, float radius, Color color) {
    return createCircle(x, y, radius, RigidBody.Type.DYNAMIC, color);
  }

  /**
   * Creates a circle entity with the specified rigid body type
   *
   * @param x        X position
   * @param y        Y position
   * @param radius   Radius of the circle
   * @param bodyType Type of physics body
   * @param color    Color of the circle
   * @return The created entity
   * @throws IllegalArgumentException if radius is not positive
   */
  public Entity createCircle(float x, float y, float radius, RigidBody.Type bodyType, Color color) {
    validatePositive(radius, "Radius");
    if (color == null) {
      throw new IllegalArgumentException("Color cannot be null");
    }

    // Create circle collider
    CircleCollider circleCollider = new CircleCollider(radius);

    // Create body definition
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = RigidBody.toBox2DBodyType(bodyType);
    bodyDef.position.set(physicsWorld.toPhysicsWorld(x), physicsWorld.toPhysicsWorld(y));

    // Create entity
    Entity entity = ecs.createEntity(
        "circle_" + System.currentTimeMillis(),
        new Transform(x, y, 0, 1, 1),
        new RenderableComponent(new Circle(color, radius)),
        circleCollider,
        new PhysicsBodyComponent(
            bodyDef,
            circleCollider.getShape(),
            bodyType == RigidBody.Type.STATIC ? 0.0f : 1.0f,
            0.3f,
            0.2f));

    return registerWithRegistrar(entity);
  }

  /**
   * Creates a polygon entity with the specified vertices and rigid body type
   *
   * @param x        X position
   * @param y        Y position
   * @param vertices Array of vertices defining the polygon shape
   * @param bodyType Type of physics body
   * @return The created entity
   * @throws IllegalArgumentException if vertices array is null or has less than 3
   *                                  points
   */
  public Entity createPolygon(float x, float y, Vec2[] vertices, RigidBody.Type bodyType) {
    if (vertices == null || vertices.length < 3) {
      throw new IllegalArgumentException("Polygon must have at least 3 vertices");
    }

    // Convert vertices to physics world coordinates
    Vec2[] scaledVertices = new Vec2[vertices.length];
    for (int i = 0; i < vertices.length; i++) {
      scaledVertices[i] = physicsWorld.toPhysicsWorld(vertices[i].x, vertices[i].y);
    }

    // Create polygon collider
    PolygonCollider polygonCollider = new PolygonCollider(scaledVertices);

    // Create body definition
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = RigidBody.toBox2DBodyType(bodyType);
    bodyDef.position.set(physicsWorld.toPhysicsWorld(x), physicsWorld.toPhysicsWorld(y));

    // Create entity with a basic renderable component (you might want to create a
    // polygon renderer)
    Entity entity = ecs.createEntity(
        "polygon_" + System.currentTimeMillis(),
        new Transform(x, y, 0, 1, 1),
        polygonCollider,
        new PhysicsBodyComponent(
            bodyDef,
            polygonCollider.getShape(),
            bodyType == RigidBody.Type.STATIC ? 0.0f : 1.0f,
            0.3f,
            0.2f));

    return registerWithRegistrar(entity);
  }

  /**
   * Creates a sensor entity that triggers collisions but has no physical response
   */
  public Entity createSensor(float x, float y, Collider collider) {
    // Create body definition for a static body
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = BodyType.STATIC;
    bodyDef.position.set(physicsWorld.toPhysicsWorld(x), physicsWorld.toPhysicsWorld(y));

    // Create physics body component
    PhysicsBodyComponent physicsBody = new PhysicsBodyComponent(
        bodyDef,
        collider.getShape(),
        0.0f, // Zero density for sensor
        0.0f, // Zero friction for sensor
        0.0f // Zero restitution for sensor
    );

    // Set as sensor
    physicsBody.setSensor(true);

    // Create entity
    Entity entity = ecs.createEntity(
        "sensor_" + System.currentTimeMillis(),
        new Transform(x, y, 0, 1, 1),
        physicsBody);

    // Add the specific collider component
    if (collider instanceof BoxCollider) {
      entity.add((BoxCollider) collider);
    } else if (collider instanceof CircleCollider) {
      entity.add((CircleCollider) collider);
    } else if (collider instanceof PolygonCollider) {
      entity.add((PolygonCollider) collider);
    }

    return entity;
  }

  /**
   * Set collision filtering for an entity
   */
  public void setCollisionFiltering(Entity entity, short category, short mask) {
    if (entity.has(PhysicsBodyComponent.class)) {
      PhysicsBodyComponent physics = entity.get(PhysicsBodyComponent.class);
      physics.setCollisionCategory(category);
      physics.setCollisionMask(mask);
    }
  }

  /**
   * Creates a basic entity with only a transform component
   *
   * @param x X coordinate in world space
   * @param y Y coordinate in world space
   * @return The created entity
   */
  public Entity createBasicEntity(float x, float y) {
    return createBasicEntity(x, y, 0, 1, 1);
  }

  /**
   * Creates a basic entity with only a transform component and specified rotation
   * and scale
   *
   * @param x        X coordinate in world space
   * @param y        Y coordinate in world space
   * @param rotation Rotation in radians
   * @param scaleX   X scale factor
   * @param scaleY   Y scale factor
   * @return The created entity
   */
  public Entity createBasicEntity(float x, float y, float rotation, float scaleX, float scaleY) {
    // Create transform with world coordinates
    Transform transform = new Transform(x, y, rotation, scaleX, scaleY);

    // Create entity with random ID
    String entityId = "entity-" + Math.round(Math.random() * 10000);

    Entity entity = ecs.createEntity(entityId, transform);

    LOGGER.fine("Created basic entity at: " + x + "," + y);
    return registerWithRegistrar(entity);
  }
}
