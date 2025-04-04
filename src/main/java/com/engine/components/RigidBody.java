package com.engine.components;

import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;

import com.engine.physics.Collider;

/**
 * Component representing a physical body in the Box2D world.
 */
public class RigidBody {
  /**
   * The type of rigid body
   */
  public enum Type {
    /**
     * Affected by forces and collisions
     */
    DYNAMIC,

    /**
     * Fixed in place, cannot move (e.g., ground, walls)
     */
    STATIC,

    /**
     * Can be moved programmatically but not affected by physics forces
     */
    KINEMATIC
  }

  private Body body;
  private final BodyDef bodyDef;
  private final FixtureDef fixtureDef;
  private Shape shape;
  private float width;
  private float height;
  private boolean isTrigger;
  private String collisionLayer;
  // Added fields for collision filtering
  private short collisionCategory = 0x0001; // Default category
  private short collisionMask = -1; // Collide with everything by default

  /**
   * Creates a rigid body with the specified type and mass
   *
   * @param type The type of rigid body
   * @param mass The mass of the body (ignored for STATIC bodies)
   */
  public RigidBody(Type type, float mass) {
    this.bodyDef = new BodyDef();
    this.bodyDef.type = toBox2DBodyType(type);
    this.fixtureDef = new FixtureDef();
    this.fixtureDef.density = mass;
    this.fixtureDef.friction = 0.3f;
    this.fixtureDef.restitution = 0.1f;
    this.width = 0;
    this.height = 0;
    this.isTrigger = false;
    this.collisionLayer = "default";

    // Set default collision filtering
    this.fixtureDef.filter.categoryBits = collisionCategory;
    this.fixtureDef.filter.maskBits = collisionMask;
  }

  /**
   * Creates a rigid body with the specified body definition and shape
   *
   * @param bodyDef     The Box2D body definition
   * @param shape       The collision shape
   * @param density     The density of the body
   * @param friction    The friction coefficient
   * @param restitution The restitution (bounciness)
   */
  public RigidBody(BodyDef bodyDef, Shape shape, float density, float friction, float restitution) {
    this.bodyDef = bodyDef;
    this.fixtureDef = new FixtureDef();
    this.fixtureDef.shape = shape;
    this.fixtureDef.density = density;
    this.fixtureDef.friction = friction;
    this.fixtureDef.restitution = restitution;
    this.shape = shape;
    this.width = 0;
    this.height = 0;
    this.isTrigger = false;
    this.collisionLayer = "default";

    // Set default collision filtering
    this.fixtureDef.filter.categoryBits = collisionCategory;
    this.fixtureDef.filter.maskBits = collisionMask;
  }

  /**
   * Convert from our Type enum to Box2D's BodyType
   */
  public static BodyType toBox2DBodyType(Type type) {
    switch (type) {
      case DYNAMIC:
        return BodyType.DYNAMIC;
      case STATIC:
        return BodyType.STATIC;
      case KINEMATIC:
        return BodyType.KINEMATIC;
      default:
        return BodyType.STATIC;
    }
  }

  /**
   * Convert from Box2D's BodyType to our Type enum
   */
  public static Type fromBox2DBodyType(BodyType bodyType) {
    if (bodyType == BodyType.DYNAMIC) {
      return Type.DYNAMIC;
    } else if (bodyType == BodyType.STATIC) {
      return Type.STATIC;
    } else if (bodyType == BodyType.KINEMATIC) {
      return Type.KINEMATIC;
    }
    return Type.STATIC;
  }

  /**
   * Apply a force to the center of the body
   *
   * @param forceX The X component of the force
   * @param forceY The Y component of the force
   */
  public void applyForce(float forceX, float forceY) {
    if (body != null) {
      body.applyForceToCenter(new Vec2(forceX, forceY));
    }
  }

  /**
   * Apply an impulse to the center of the body
   *
   * @param impulseX The X component of the impulse
   * @param impulseY The Y component of the impulse
   */
  public void applyImpulse(float impulseX, float impulseY) {
    if (body != null) {
      body.applyLinearImpulse(new Vec2(impulseX, impulseY), body.getWorldCenter(), true);
    }
  }

  /**
   * Set the velocity of the body
   *
   * @param velocityX The X component of the velocity
   * @param velocityY The Y component of the velocity
   */
  public void setVelocity(float velocityX, float velocityY) {
    if (body != null) {
      body.setLinearVelocity(new Vec2(velocityX, velocityY));
    }
  }

  /**
   * Enable or disable continuous collision detection for this body
   *
   * @param enable True to enable CCD, false to disable
   */
  public void setContinuousCollisionDetection(boolean enable) {
    if (body != null) {
      body.setBullet(enable);
    }
  }

  /**
   * Set the collider for this rigid body
   *
   * @param collider The collider to use
   */
  public void setCollider(Collider collider) {
    if (collider != null) {
      this.shape = collider.getShape();
      if (fixtureDef != null) {
        fixtureDef.shape = shape;
      }
    }
  }

  // Existing getters and setters
  public Body getBody() {
    return body;
  }

  public void setBody(Body body) {
    this.body = body;
  }

  public BodyDef getBodyDef() {
    return bodyDef;
  }

  public FixtureDef getFixtureDef() {
    return fixtureDef;
  }

  public Shape getShape() {
    return shape;
  }

  public void setShape(Shape shape) {
    this.shape = shape;
    if (fixtureDef != null) {
      fixtureDef.shape = shape;
    }
  }

  public float getWidth() {
    return width;
  }

  public float getHeight() {
    return height;
  }

  public boolean isTrigger() {
    return isTrigger;
  }

  public void setTrigger(boolean trigger) {
    isTrigger = trigger;
  }

  public String getCollisionLayer() {
    return collisionLayer;
  }

  public void setCollisionLayer(String collisionLayer) {
    this.collisionLayer = collisionLayer;
  }

  public void setSensor(boolean isSensor) {
    this.isTrigger = isSensor;
    if (fixtureDef != null) {
      fixtureDef.isSensor = isSensor;
    }
    if (body != null && body.getFixtureList() != null) {
      body.getFixtureList().setSensor(isSensor);
    }
  }

  public void setCollisionCategory(short category) {
    this.collisionCategory = category;
    if (fixtureDef != null) {
      fixtureDef.filter.categoryBits = category;
    }
    if (body != null && body.getFixtureList() != null) {
      org.jbox2d.dynamics.Filter filter = body.getFixtureList().getFilterData();
      filter.categoryBits = category;
      body.getFixtureList().setFilterData(filter);
    }
  }

  public void setCollisionMask(short mask) {
    this.collisionMask = mask;
    if (fixtureDef != null) {
      fixtureDef.filter.maskBits = mask;
    }
    if (body != null && body.getFixtureList() != null) {
      org.jbox2d.dynamics.Filter filter = body.getFixtureList().getFilterData();
      filter.maskBits = mask;
      body.getFixtureList().setFilterData(filter);
    }
  }

  public short getCollisionCategory() {
    return collisionCategory;
  }

  public short getCollisionMask() {
    return collisionMask;
  }

  /**
   * Set the friction coefficient
   *
   * @param friction Friction coefficient (0 = no friction, 1 = max friction)
   */
  public void setFriction(float friction) {
    if (fixtureDef != null) {
      fixtureDef.friction = friction;
    }
    if (body != null && body.getFixtureList() != null) {
      body.getFixtureList().setFriction(friction);
    }
  }

  /**
   * Set the restitution (bounciness)
   *
   * @param restitution Restitution value (0 = no bounce, 1 = perfect bounce)
   */
  public void setRestitution(float restitution) {
    if (fixtureDef != null) {
      fixtureDef.restitution = restitution;
    }
    if (body != null && body.getFixtureList() != null) {
      body.getFixtureList().setRestitution(restitution);
    }
  }
}
