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

  public PhysicsBodyComponent(BodyDef bodyDef, Shape shape, float density, float friction, float restitution) {
    this.bodyDef = bodyDef;
    this.fixtureDef = new FixtureDef();
    this.fixtureDef.shape = shape;
    this.fixtureDef.density = density;
    this.fixtureDef.friction = friction;
    this.fixtureDef.restitution = restitution;
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
}
