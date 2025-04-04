package com;

import java.awt.event.KeyEvent;

import com.engine.core.GameEngine;
import com.engine.di.DaggerEngineComponent;
import com.engine.di.EngineComponent;
import com.engine.di.EngineModule;
import com.engine.scene.TestScene;
import com.engine.scene.TestScene2;

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

    // Set up input handling with the InputManager
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

    // Log world position of mouse clicks
    inputManager.onMousePress(1, e -> {
      float[] worldPos = inputManager.getMouseWorldPosition();
      System.out.println("Mouse clicked at world position: " + worldPos[0] + ", " + worldPos[1]);
    });
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
