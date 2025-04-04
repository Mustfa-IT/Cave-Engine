package com.engine.scene;

import java.awt.Color;

import com.engine.entity.EntityFactory;

/**
 * A test scene with basic physics objects
 */
public class SimpleScene extends Scene {
  long startTime;

  public SimpleScene(EntityFactory entityFactory) {
    super(entityFactory);
  }

  @Override
  public void update(double deltaTime) {

  }

  @Override
  public void initialize() {
    System.out.println("TestScene 1 activated");
    startTime = System.currentTimeMillis();

    // In our new coordinate system (Y+ is up), create ground near the bottom
    entityFactory.createGround(0, -300, 1800, 20, Color.GRAY);
    for (int i = 0; i < 30; i++) {
      // Create objects with negative Y values (below center)
      // Create red balls
      entityFactory.createBall(i * 30 - 400, -100, 25, Color.RED, 1.5f, 0.3f, 0.5f);

      // Create blue balls
      entityFactory.createBall(i * 30 - 400, -150, 15, Color.BLUE, 1.0f, 0.3f, 0.7f);

      // Create green rectangles
      entityFactory.createRect(i * 30 - 400, -200, 10f, 20f, Color.GREEN, 1.0f, 0.3f, 0.3f);
    }
    engine.getUiSystem().createLabel("Hello", 100f, 100f);
  }
}
