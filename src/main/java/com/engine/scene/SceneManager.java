package com.engine.scene;

import java.util.HashMap;
import java.util.Map;

import com.engine.core.GameEngine;

/**
 * Manages scenes in the game
 */
public class SceneManager {
  private Scene currentScene;
  private final Map<String, Scene> scenes = new HashMap<>();
  private final GameEngine engine;

  public SceneManager(GameEngine engine) {
    this.engine = engine;
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

    // Deactivate current scene if there is one
    if (currentScene != null) {
      currentScene.onDeactivate();
    }

    // Set and initialize new scene
    Scene newScene = scenes.get(sceneName);
    this.currentScene = newScene;

    // Initialize if needed and activate
    newScene.onActivate();
  }

  /**
   * Initializes a specific scene
   */
  public void initializeScene(String sceneName) {
    if (!scenes.containsKey(sceneName)) {
      throw new IllegalArgumentException("Scene not found: " + sceneName);
    }
    scenes.get(sceneName).initialize();
  }

  /**
   * Initialize all registered scenes
   */
  public void initializeAllScenes() {
    scenes.values().forEach(Scene::initialize);
  }

  /**
   * Update the current scene
   */
  public void update(double deltaTime) {
    if (currentScene != null) {
      currentScene.update(deltaTime);
    }
  }

  /**
   * Get the current active scene
   */
  public Scene getCurrentScene() {
    return currentScene;
  }
}
