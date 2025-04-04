package com.engine.scene;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;

import org.openjdk.tools.javac.util.Pair;

import com.engine.core.GameEngine;
import com.engine.entity.EntityFactory;
import com.engine.ui.UISystem;

/**
 * Manages scenes in the game
 */
public class SceneManager {
  private static final Logger LOGGER = Logger.getLogger(SceneManager.class.getName());
  private Pair<String, Scene> currentScene;
  private final Map<String, Scene> scenes = new HashMap<>();
  private final GameEngine engine;
  private final Stack<Pair<String, Scene>> sceneStack = new Stack<>();
  private final EntityFactory entityFactory;
  private final UISystem uiSystem;

  public SceneManager(GameEngine engine, EntityFactory entityFactory, UISystem uiSystem) {
    this.engine = engine;
    this.entityFactory = entityFactory;
    this.uiSystem = uiSystem;
  }

  /**
   * Register a scene with the manager
   *
   * @param name  Scene identifier
   * @param scene The scene instance
   */
  public void registerScene(String name, Scene scene) {
    scene.setEngine(engine);
    scenes.put(name, scene);
  }

  /**
   * Set the current active scene
   *
   * @param sceneName Name of the scene to activate
   */
  public void setActiveScene(String sceneName) {
    if (!scenes.containsKey(sceneName)) {
      throw new IllegalArgumentException("Scene not found: " + sceneName);
    }

    Scene newScene = scenes.get(sceneName);

    // Don't transition if it's the same scene
    if (currentScene != null && currentScene.snd == newScene) {
      LOGGER.info("Scene " + sceneName + " is already active");
      return;
    }

    // Prevent scene transitions from happening too quickly
    synchronized (this) {
      // First clear registrars to prevent new entities being created
      entityFactory.setCurrentRegistrar(null);
      uiSystem.setCurrentRegistrar(null);

      // Deactivate current scene if there is one
      if (currentScene != null) {
        try {
          LOGGER.info("Deactivating current scene");
          currentScene.snd.onDeactivate();

          // Give the ECS time to process any cleanup
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            LOGGER.warning("Scene transition sleep interrupted: " + e.getMessage());
          }

          // Encourage garbage collection
          System.gc();

          // Give GC time to work
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
            LOGGER.warning("GC wait interrupted: " + e.getMessage());
          }
        } catch (Exception e) {
          LOGGER.severe("Error deactivating current scene: " + e.getMessage());
        }
      }

      // Set the new scene
      this.currentScene = new Pair<String, Scene>(sceneName, newScene);

      // Set the new scene as the entity registrar for both entity factory and UI
      // system
      try {
        entityFactory.setCurrentRegistrar(newScene);
        uiSystem.setCurrentRegistrar(newScene);

        LOGGER.info("Activating new scene: " + sceneName);
        newScene.onActivate();
      } catch (Exception e) {
        LOGGER.severe("Error activating new scene: " + e.getMessage());
      }
    }
  }

  /**
   * Push a scene onto the stack and activate it
   *
   * @param sceneName Name of the scene to push and activate
   */
  public void pushScene(String sceneName) {
    if (!scenes.containsKey(sceneName)) {
      throw new IllegalArgumentException("Scene not found: " + sceneName);
    }

    synchronized (this) {
      if (currentScene != null) {
        // Clear registrars first
        entityFactory.setCurrentRegistrar(null);
        uiSystem.setCurrentRegistrar(null);

        currentScene.snd.onDeactivate();
        sceneStack.push(currentScene);

        // Give the ECS a chance to process
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          LOGGER.warning("Scene push interrupted: " + e.getMessage());
        }
      }

      currentScene = new Pair<String, Scene>(sceneName, scenes.get(sceneName));

      // Set the new scene as registrar for both systems
      entityFactory.setCurrentRegistrar(currentScene.snd);
      uiSystem.setCurrentRegistrar(currentScene.snd);

      currentScene.snd.onActivate();
    }
  }

  /**
   * Pop the current scene from the stack and activate the previous one
   */
  public void popScene() {
    if (!sceneStack.isEmpty()) {
      synchronized (this) {
        // Clear registrars first
        entityFactory.setCurrentRegistrar(null);
        uiSystem.setCurrentRegistrar(null);

        if (currentScene != null) {
          currentScene.snd.onDeactivate();
        }

        // Give the ECS a chance to process
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          LOGGER.warning("Scene pop interrupted: " + e.getMessage());
        }

        currentScene = sceneStack.pop();

        // Set the previous scene as registrar for both systems
        entityFactory.setCurrentRegistrar(currentScene.snd);
        uiSystem.setCurrentRegistrar(currentScene.snd);

        currentScene.snd.onActivate();
      }
    }
  }

  /**
   * Update the current scene
   */
  public void update(double deltaTime) {
    if (currentScene != null) {
      currentScene.snd.update(deltaTime);
    }
  }

  /**
   * Get the current active scene
   */
  public Scene getCurrentScene() {
    return currentScene != null ? currentScene.snd : null;
  }

  public String getCurrentSceneName() {
    return currentScene != null ? currentScene.fst : null;
  }

  public String[] getSceneNames() {
    return scenes.keySet().toArray(new String[0]);
  }

  /**
   * Force cleanup of all scenes
   * This is useful for complete resets or when switching between vastly different
   * game modes
   */
  public void cleanupAllScenes() {
    synchronized (this) {
      LOGGER.info("Cleaning up all scenes");

      // First set all registrars to null to prevent new entity creation
      entityFactory.setCurrentRegistrar(null);
      uiSystem.setCurrentRegistrar(null);

      // Then clean up each scene
      for (Scene scene : scenes.values()) {
        try {
          scene.forceCleanup();
        } catch (Exception e) {
          LOGGER.severe("Error cleaning up scene: " + e.getMessage());
        }
      }

      // Give time for cleanup
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        LOGGER.warning("Cleanup sleep interrupted: " + e.getMessage());
      }

      // Run garbage collection
      System.gc();

      LOGGER.info("All scenes cleaned up");
    }
  }
}
