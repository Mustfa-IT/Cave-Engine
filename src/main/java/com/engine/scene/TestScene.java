package com.engine.scene;

import java.awt.Color;

import com.engine.entity.EntityFactory;

/**
 * A test scene with basic physics objects
 */
public class TestScene extends Scene {
  long startTime;

  public TestScene(EntityFactory entityFactory) {
    super(entityFactory);
  }

  @Override
  public void update(double deltaTime) {
    if (((System.currentTimeMillis() - startTime) / 1000) > 3) {
      startTime = System.currentTimeMillis();
      super.engine.getSceneManager().popScene();
      ;
    }
  }

  @Override
  public void initialize() {
    System.out.println("TestScene 1 activated");
    startTime = System.currentTimeMillis();
    // Create ground
    entityFactory.createGround(1000, 700, 1800, 20, Color.GRAY);
    for (int i = 0; i < 30; i++) {
      // Create red ball
      entityFactory.createBall(400 + (i * 10), 100, 25, Color.RED, 1.5f, 0.3f, 0.5f);

      // Create blue ball
      entityFactory.createBall(300 + (i * 10), 50, 15, Color.BLUE, 1.0f, 0.3f, 0.7f);

      // Create green rect
      entityFactory.createRect(310f + (i * 10), 100f, 10f, 20f, Color.GREEN, 1.0f, 0.3f, 0.3f);
    }
  }

}
