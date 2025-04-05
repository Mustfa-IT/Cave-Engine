package com.engine.scene;

import java.awt.Color;
import java.util.Random;

import javax.inject.Inject;

import com.engine.entity.EntityFactory;

/**
 * A simple scene that creates a box with many dynamic balls inside
 */
public class SimpleScene2 extends Scene {
  private static final int BOX_WIDTH = 800;
  private static final int BOX_HEIGHT = 600;
  private static final int BALL_COUNT = 50;
  private static final float WALL_THICKNESS = 20;

  private final Random random = new Random();

  @Inject
  public SimpleScene2(EntityFactory entityFactory) {
    super(entityFactory);
  }

  @Override
  public void initialize() {

    // Create the box walls (static bodies)
    createContainerBox();

    // Create multiple dynamic balls inside the box
    createBalls();
  }

  private void createContainerBox() {
    // Bottom wall
    entityFactory.createGround(0, BOX_HEIGHT / 2, BOX_WIDTH, WALL_THICKNESS, Color.DARK_GRAY);

    // Top wall
    entityFactory.createGround(0, -BOX_HEIGHT / 2, BOX_WIDTH, WALL_THICKNESS, Color.DARK_GRAY);

    // Left wall
    entityFactory.createGround(-BOX_WIDTH / 2, 0, WALL_THICKNESS, BOX_HEIGHT, Color.DARK_GRAY);

    // Right wall
    entityFactory.createGround(BOX_WIDTH / 2, 0, WALL_THICKNESS, BOX_HEIGHT, Color.DARK_GRAY);
  }

  private void createBalls() {
    for (int i = 0; i < BALL_COUNT; i++) {
      // Random position within the box (accounting for the walls)
      float x = random.nextFloat() * (BOX_WIDTH - 100) - (BOX_WIDTH / 2 - 50);
      float y = random.nextFloat() * (BOX_HEIGHT - 100) - (BOX_HEIGHT / 2 - 50);

      // Random radius between 10 and 30
      float radius = 10 + random.nextFloat() * 20;

      // Random color
      Color color = new Color(
          random.nextFloat(),
          random.nextFloat(),
          random.nextFloat());

      // Random physics properties
      float density = 0.5f + random.nextFloat() * 2.0f;
      float friction = 0.2f + random.nextFloat() * 0.6f;
      float restitution = 0.3f + random.nextFloat() * 0.6f;

      // Create the ball
      entityFactory.createBall(x, y, radius, color, density, friction, restitution);
    }
  }
}
