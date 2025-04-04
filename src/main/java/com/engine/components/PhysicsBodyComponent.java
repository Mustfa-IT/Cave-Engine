package com.engine.components;

import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;

/**
 * Component representing a physical body in the Box2D world.
 */
public class PhysicsBodyComponent {
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

  public PhysicsBodyComponent(BodyDef bodyDef, Shape shape, float density, float friction, float restitution) {
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

  /**
   * Set whether this body is a sensor (triggers collisions but has no physical
   * response)
   * 
   * @param isSensor True if this body should be a sensor
   */
  public void setSensor(boolean isSensor) {
    this.isTrigger = isSensor;
    if (fixtureDef != null) {
      fixtureDef.isSensor = isSensor;
    }
    if (body != null && body.getFixtureList() != null) {
      body.getFixtureList().setSensor(isSensor);
    }
  }

  /**
   * Set the collision category bits (which collision group this body belongs to)
   * 
   * @param category The category bits
   */
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

  /**
   * Set the collision mask bits (which collision groups this body should collide
   * with)
   * 
   * @param mask The mask bits
   */
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

  /**
   * Get the collision category bits
   * 
   * @return The category bits
   */
  public short getCollisionCategory() {
    return collisionCategory;
  }

  /**
   * Get the collision mask bits
   * 
   * @return The mask bits
   */
  public short getCollisionMask() {
    return collisionMask;
  }
}
