package com.engine.scene;

import java.awt.Color;

import com.engine.entity.EntityFactory;

/**
 * A test scene with basic physics objects
 */
public class TestScene extends Scene {

  public TestScene(EntityFactory entityFactory) {
    super(entityFactory);
  }

  @Override
  public void initialize() {
    // Create ground
    entityFactory.createGround(400, 700, 800, 20, Color.GRAY);

    // Create red ball
    entityFactory.createBall(400, 100, 25, Color.RED, 1.5f, 0.3f, 0.5f);

    // Create blue ball
    entityFactory.createBall(300, 50, 15, Color.BLUE, 1.0f, 0.3f, 0.7f);

    // Create green rect
    entityFactory.createRect(310f, 100f, 10f, 20f, Color.GREEN, 1.0f, 0.3f, 0.3f);

    System.out.println("TestScene initialized");
  }

  @Override
  public void update(double deltaTime) {
    // Example of scene-specific logic that could be implemented here
    // For instance, spawning new entities based on time or events
  }

  @Override
  public void onActivate() {
    System.out.println("TestScene activated");
  }

  @Override
  public void onDeactivate() {
    System.out.println("TestScene deactivated");
  }
}
