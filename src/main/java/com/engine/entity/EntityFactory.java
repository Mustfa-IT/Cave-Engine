package com.engine.entity;

import java.awt.Color;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;

import com.engine.components.RenderableComponent;
import com.engine.components.Transform;
import com.engine.graph.Circle;
import com.engine.graph.Rect;
import com.engine.pyhsics.PhysicsBodyComponent;
import com.engine.pyhsics.PhysicsWorld;

import dev.dominion.ecs.api.Dominion;

/**
 * Factory for creating game entities
 */
public class EntityFactory {
  private final Dominion ecs;
  private final PhysicsWorld physicsWorld;

  public EntityFactory(Dominion ecs, PhysicsWorld physicsWorld) {
    this.ecs = ecs;
    this.physicsWorld = physicsWorld;
  }

  /**
   * Creates a static ground platform
   */
  public String createGround(float x, float y, float width, float height, Color color) {
    // Create transform
    Transform transform = new Transform(x, y, 0, 1, 1);

    // Visual representation
    Rect groundRect = new Rect(transform, color, width, height);

    // Physics body definition
    BodyDef groundBodyDef = new BodyDef();
    groundBodyDef.type = BodyType.STATIC;
    groundBodyDef.position = physicsWorld.toPhysicsWorld((float) x, (float) y);
    groundBodyDef.fixedRotation = true;

    // Physics shape
    PolygonShape groundShape = new PolygonShape();
    groundShape.setAsBox(
        physicsWorld.toPhysicsWorld(width / 2),
        physicsWorld.toPhysicsWorld(height / 2));

    // Create entity
    return ecs.createEntity(
        "ground",
        transform,
        new RenderableComponent(groundRect),
        new PhysicsBodyComponent(groundBodyDef, groundShape, 0, 0.3f, 0.2f)).toString();
  }

  /**
   * Creates a dynamic ball
   */
  public String createBall(float x, float y, float radius, Color color,
      float density, float friction, float restitution) {
    // Create transform
    Transform transform = new Transform(x, y, 0, 1, 1);

    // Visual representation
    Circle ballCircle = new Circle(transform, color, radius * 2); // Diameter

    // Physics body definition
    BodyDef ballBodyDef = new BodyDef();
    ballBodyDef.type = BodyType.DYNAMIC;
    ballBodyDef.position = physicsWorld.toPhysicsWorld(x, y);
    ballBodyDef.angularDamping = 0.8f;
    ballBodyDef.linearDamping = 0.1f;

    // Physics shape
    CircleShape ballShape = new CircleShape();
    ballShape.setRadius(physicsWorld.toPhysicsWorld(radius));

    // Create entity with random ID to allow multiple instances
    String entityId = "ball-" + Math.round(Math.random() * 10000);

    return ecs.createEntity(
        entityId,
        transform,
        new RenderableComponent(ballCircle),
        new PhysicsBodyComponent(ballBodyDef, ballShape, density, friction, restitution)).toString();
  }

  public String createRect(float x, float y, float width, float height, Color color,
      float density, float friction, float restitution) {
    Transform transform = new Transform(x, y, 0, 1, 1);
    Rect rectangle = new Rect(transform, color, width, height);
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = BodyType.DYNAMIC;
    bodyDef.position = physicsWorld.toPhysicsWorld(x, y);
    PolygonShape shape = new PolygonShape();
    shape.setAsBox(
        physicsWorld.toPhysicsWorld(width / 2),
        physicsWorld.toPhysicsWorld(height / 2));
    String entityId = "rect-" + Math.round(Math.random() * 10000);
    return ecs.createEntity(
        entityId,
        transform,
        new RenderableComponent(rectangle),
        new PhysicsBodyComponent(bodyDef, shape, density, friction, restitution))
        .toString();
  }
}
