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

    // In screen space (Y+ is down), so positive Y values are below center
    // Create ground near the bottom of the screen
    entityFactory.createGround(0, 300, 800, 20, Color.GRAY);

    // Create balls - negative Y values are above the center
    // Create a central red ball above the ground
    entityFactory.createBall(0, 100, 25, Color.RED, 1.0f, 0.3f, 0.5f);

    // Create a few more balls at different positions
    entityFactory.createBall(-200, 0, 20, Color.BLUE, 1.0f, 0.3f, 0.7f);
    entityFactory.createBall(200, 0, 20, Color.GREEN, 1.0f, 0.3f, 0.7f);

    // Stationary platform in the middle
    entityFactory.createGround(-200, 200, 200, 20, Color.LIGHT_GRAY);

    System.out.println("TestScene 2 initialized");
    engine.getUiSystem().createLabel("Hello",100f,100f);
  }

  @Override
  public void update(double deltaTime) {
    if (((System.currentTimeMillis() - startTime) / 1000) > 5) {
      super.engine.getSceneManager().pushScene("test");
    }
  }

  @Override
  public void onActivate() {
    System.out.println("TestScene 2 activated");
    super.onActivate();
  }

  @Override
  public void onDeactivate() {
    System.out.println("TestScene 2 deactivated");
    super.onDeactivate();
  }
}
