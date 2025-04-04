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
}
