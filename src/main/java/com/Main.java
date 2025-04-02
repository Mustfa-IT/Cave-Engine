package com;

import java.awt.Color;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;

import com.engine.components.RenderableComponent;
import com.engine.components.Transform;
import com.engine.core.GameEngine;
import com.engine.graph.Circle;
import com.engine.graph.Rect;
import com.engine.pyhsics.PhysicsBodyComponent;

public class Main {
  public static void main(String[] args) {
    GameEngine game = new GameEngine();
    var ecs = game.ecs;
    var physics = game.getPhysicsWorld();

    // Create a ground platform with matching visual and physics dimensions
    Transform groundTransform = new Transform(400, 700, 0, 1, 1);

    // Visual representation - make sure the width and height match physics shape
    float groundWidth = 800; // Make this wider
    float groundHeight = 20; // Make this thicker for better visibility
    Rect groundRect = new Rect(groundTransform, Color.GRAY, groundWidth, groundHeight);

    // Create physics body definition for ground
    BodyDef groundBodyDef = new BodyDef();
    groundBodyDef.type = BodyType.STATIC;
    groundBodyDef.position = physics.toPhysicsWorld((float) groundTransform.getX(), (float) groundTransform.getY());
    groundBodyDef.fixedRotation = true;

    // Create ground shape with matching dimensions to the visual rectangle
    PolygonShape groundShape = new PolygonShape();
    // setAsBox takes half-width and half-height
    groundShape.setAsBox(
        physics.toPhysicsWorld(groundWidth / 2),
        physics.toPhysicsWorld(groundHeight / 2));

    // Create ground entity
    ecs.createEntity(
        "ground",
        groundTransform,
        new RenderableComponent(groundRect),
        new PhysicsBodyComponent(groundBodyDef, groundShape, 0, 0.3f, 0.2f));

    // Create a falling ball - MATCHING VISUAL AND PHYSICS DIMENSIONS
    float ballRadius = 25; // Same radius for both visual and physics
    Transform ballTransform = new Transform(400, 100, 0, 1, 1);
    Circle ballCircle = new Circle(ballTransform, Color.RED, ballRadius * 2); // Diameter is 2*radius

    // Create physics body definition for ball
    BodyDef ballBodyDef = new BodyDef();
    ballBodyDef.type = BodyType.DYNAMIC;
    ballBodyDef.position = physics.toPhysicsWorld((float) ballTransform.getX(), (float) ballTransform.getY());
    // Prevent excessive rotation
    ballBodyDef.angularDamping = 0.8f;
    // Add some linear damping to simulate air resistance
    ballBodyDef.linearDamping = 0.1f;

    // Create ball shape with MATCHING radius
    CircleShape ballShape = new CircleShape();
    ballShape.setRadius(physics.toPhysicsWorld(ballRadius));

    // Create ball entity with slightly higher density to make it fall more
    // naturally
    ecs.createEntity(
        "ball",
        ballTransform,
        new RenderableComponent(ballCircle),
        new PhysicsBodyComponent(ballBodyDef, ballShape, 1.5f, 0.3f, 0.5f)); // Adjusted density and restitution

    // Add a second ball to test multiple physics objects
    float ball2Radius = 15;
    Transform ball2Transform = new Transform(300, 50, 0, 1, 1);
    Circle ball2Circle = new Circle(ball2Transform, Color.BLUE, ball2Radius * 2);

    BodyDef ball2BodyDef = new BodyDef();
    ball2BodyDef.type = BodyType.DYNAMIC;
    ball2BodyDef.position = physics.toPhysicsWorld((float) ball2Transform.getX(), (float) ball2Transform.getY());

    CircleShape ball2Shape = new CircleShape();
    ball2Shape.setRadius(physics.toPhysicsWorld(ball2Radius));

    ecs.createEntity(
        "ball2",
        ball2Transform,
        new RenderableComponent(ball2Circle),
        new PhysicsBodyComponent(ball2BodyDef, ball2Shape, 1.0f, 0.3f, 0.7f));

    // Print debug info
    System.out.println("Ground position: " + groundTransform.getX() + ", " + groundTransform.getY());
    System.out.println("Ground dimensions: " + groundWidth + "x" + groundHeight);
    System.out.println("Ball radius (visual): " + ballRadius * 2 + ", (physics): " + ballRadius);

    game.start();
  }
}
