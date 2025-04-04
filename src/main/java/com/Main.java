package com;

import java.awt.event.KeyEvent;
import java.awt.Graphics2D;
import java.awt.Color;

import com.engine.core.GameEngine;
import com.engine.core.AbstractGameObject;
import com.engine.di.DaggerEngineComponent;
import com.engine.di.EngineComponent;
import com.engine.di.EngineModule;
import com.engine.gameobject.GameObject;
import com.engine.physics.Collision;
import com.engine.entity.EntityFactory;
import com.engine.entity.EntityFactory.PhysicsParameters;
import com.engine.scene.TestScene;
import com.engine.scene.TestScene2;

import org.jbox2d.dynamics.BodyType;

import dev.dominion.ecs.api.Entity;

public class Main {
  public static void main(String[] args) {
    // Use Dagger to create the engine with simplified initialization
    GameEngine game = createEngine();

    // Configure the engine with enhanced fluent API
    game.configure(config -> config
        .targetFps(60)
        .showPerformanceStats(true)
        .debugMode(false, true, true)
        .gravity(0, -9.8f)
        .windowTitle("Enhanced Physics Engine"))
        .createCamera(0, 0, 1.0f)
        .createScene("test", () -> new TestScene(game.getEntityFactory()))
        .createScene("test2", () -> new TestScene2(game.getEntityFactory()))
        .setActiveScene("test2")
        .start();

    // Set up input handlers for creating custom GameObjects
    setupInputHandlers(game);
  }

  /**
   * Sets up input handlers for various game controls
   *
   * @param game The game engine instance
   */
  private static void setupInputHandlers(GameEngine game) {
    // Get input manager from the engine
    var inputManager = game.getInputManager();
    var entityFactory = game.getEntityFactory();

    // Setup pause toggle with P key
    inputManager.onKeyPress(KeyEvent.VK_P, e -> game.togglePause());

    // Debug visualization toggle with D key
    inputManager.onKeyPress(KeyEvent.VK_D, e -> {
      boolean currentDebugState = game.isDebugColliders();
      game.setDebugDisplay(!currentDebugState, !currentDebugState, game.isDebugGrid());
    });

    // Scene switching
    inputManager.onKeyPress(KeyEvent.VK_1, e -> game.setActiveScene("test"));
    inputManager.onKeyPress(KeyEvent.VK_2, e -> game.setActiveScene("test2"));

    // Screenshot with F12
    inputManager.onKeyPress(KeyEvent.VK_F12,
        e -> game.takeScreenshot("screenshot_" + System.currentTimeMillis() + ".png"));

    // Create custom GameObject on click at world position
    inputManager.onMousePress(1, e -> {
      float[] worldPos = inputManager.getMouseWorldPosition();
      System.out.println("Mouse clicked at world position: " + worldPos[0] + ", " + worldPos[1]);

      // Create a custom GameObject at click position
      entityFactory.createGameObject(worldPos[0], worldPos[1], new RotatingSquare(50, Color.YELLOW));
    });

    // Create physics GameObject with right click (using new component-based
    // approach)
    inputManager.onMousePress(3, e -> {
      float[] worldPos = inputManager.getMouseWorldPosition();

      // Create a physics-enabled GameObject with the new approach
      PhysicsParameters circlePhysics = PhysicsParameters.circle(
          30, BodyType.DYNAMIC, 1.0f, 0.3f, 0.5f);

      entityFactory.createGameObject(
          worldPos[0], worldPos[1],
          new BouncingObject(),
          circlePhysics);
    });

    // Create box physics GameObject with middle click
    inputManager.onMousePress(2, e -> {
      float[] worldPos = inputManager.getMouseWorldPosition();

      // Create non-physics GameObject first
      Entity entity = entityFactory.createGameObject(worldPos[0], worldPos[1], new ColorChangingBox());

      // Then add physics to it
      entityFactory.addBoxPhysics(entity, 40, 40, BodyType.DYNAMIC, 0.5f, 0.3f, 0.8f);
    });
  }

  /**
   * Example custom GameObject that rotates and draws a square
   */
  static class RotatingSquare implements GameObject {
    private final float size;
    private final Color color;
    private float rotation = 0;

    public RotatingSquare(float size, Color color) {
      this.size = size;
      this.color = color;
    }

    @Override
    public void onStart(Entity entity) {
      System.out.println("RotatingSquare started!");
    }

    @Override
    public void update(double deltaTime) {
      // Rotate the square slowly
      rotation += deltaTime * 1.5f;
    }

    @Override
    public void render(Graphics2D g) {
      g.rotate(rotation);
      g.setColor(color);
      g.fillRect((int) (-size / 2), (int) (-size / 2), (int) size, (int) size);
    }

    @Override
    public void onDestroy() {
      System.out.println("RotatingSquare destroyed!");
    }
  }

  /**
   * Example physics GameObject that changes color when active
   */
  static class BouncingObject extends AbstractGameObject {
    private Color color = Color.CYAN;
    private float time = 0;

    @Override
    public void onStart(Entity entity) {
      super.onStart(entity);
      System.out.println("BouncingObject started!");
    }

    @Override
    public void update(double deltaTime) {
      time += deltaTime;

      // Cycle through colors based on time
      float hue = (time * 0.5f) % 1.0f;
      color = Color.getHSBColor(hue, 0.8f, 1.0f);
    }

    @Override
    public void render(Graphics2D g) {
      g.setColor(color);
      g.fillOval(-15, -15, 30, 30);

      // Draw a simple face
      g.setColor(Color.BLACK);
      g.fillOval(-7, -7, 4, 4); // left eye
      g.fillOval(3, -7, 4, 4); // right eye
      g.drawArc(-8, 0, 16, 8, 0, -180); // smile
    }

    @Override
    public void onDestroy() {
      System.out.println("BouncingObject destroyed!");
    }

  }

  /**
   * Example of a GameObject that changes color and will have physics added after
   * creation
   */
  static class ColorChangingBox extends AbstractGameObject {
    private Color color = Color.GREEN;
    private float time = 0;

    @Override
    public void update(double deltaTime) {
      time += deltaTime;
      // Change color based on time
      color = Color.getHSBColor((time * 0.3f) % 1.0f, 0.7f, 0.9f);
    }

    @Override
    public void render(Graphics2D g) {
      g.setColor(color);
      g.fillRect(-20, -20, 40, 40);
    }

    @Override
    public void onDestroy() {
      System.out.println("ColorChangingBox destroyed!");
    }

  }

  /**
   * Creates and initializes the game engine
   *
   * @return Configured GameEngine instance
   */
  private static GameEngine createEngine() {
    EngineComponent engineComponent = DaggerEngineComponent.builder()
        .concreteModule(new EngineModule.ConcreteModule())
        .build();
    return engineComponent.engine();
  }
}
