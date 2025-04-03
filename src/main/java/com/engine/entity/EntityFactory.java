package com.engine.entity;

import java.awt.Color;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;

import com.engine.components.PhysicsBodyComponent;
import com.engine.components.RenderableComponent;
import com.engine.components.Transform;
import com.engine.graph.Circle;
import com.engine.graph.Rect;
import com.engine.physics.PhysicsWorld;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * Factory for creating game entities in world coordinates
 * where (0,0) is the center of the viewport
 */
public class EntityFactory {
  private final Dominion ecs;
  private final PhysicsWorld physicsWorld;
  private EntityRegistrar currentRegistrar;

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
      return currentRegistrar.registerEntity(entity);
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
}
