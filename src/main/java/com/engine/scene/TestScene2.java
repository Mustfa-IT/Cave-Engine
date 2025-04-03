package com.engine.scene;

import java.awt.Color;

import com.engine.entity.EntityFactory;

/**
 * A test scene with basic physics objects
 */
public class TestScene2 extends Scene {
  long startTime;

  public TestScene2(EntityFactory entityFactory) {
    super(entityFactory);
  }

  @Override
  public void initialize() {
    startTime = System.currentTimeMillis();
    // Create ground
    entityFactory.createGround(400, 700, 800, 20, Color.GRAY);

    // Create red ball
    entityFactory.createBall(400, 100, 25, Color.RED, 1.5f, 0.3f, 0.5f);

    System.out.println("TestScene 2 initialized");
  }

  @Override
  public void update(double deltaTime) {
    if (((System.currentTimeMillis() - startTime) / 1000) > 3) {
      super.engine.getSceneManager().setActiveScene("test");
    }
  }

  @Override
  public void onActivate() {
    System.out.println("TestScene 2 activated");
    super.onActivate(); // Fixed: Call onActivate instead of onDeactivate
  }

  @Override
  public void onDeactivate() {
    System.out.println("TestScene 2 deactivated");
    super.onDeactivate(); // This is correct, but was potentially being called multiple times
  }
}
