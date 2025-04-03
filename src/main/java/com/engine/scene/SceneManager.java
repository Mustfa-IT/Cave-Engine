package com.engine.scene;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.engine.core.GameEngine;

/**
 * Manages scenes in the game
 */
public class SceneManager {
  private Scene currentScene;
  private final Map<String, Scene> scenes = new HashMap<>();
  private final GameEngine engine;
  private final Stack<Scene> sceneStack = new Stack<>();

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

    // Set the new scene in the entity factory
    engine.getEntityFactory().setCurrentScene(newScene);

    // Activate the new scene
    newScene.onActivate();
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
    if (currentScene != null) {
      currentScene.onDeactivate();
      sceneStack.push(currentScene);
    }
    currentScene = scenes.get(sceneName);
    engine.getEntityFactory().setCurrentScene(currentScene);
    currentScene.onActivate();
  }

  /**
   * Pop the current scene from the stack and activate the previous one
   */
  public void popScene() {
    if (!sceneStack.isEmpty()) {
      currentScene.onDeactivate();
      currentScene = sceneStack.pop();
      engine.getEntityFactory().setCurrentScene(currentScene);
      currentScene.onActivate();
    }
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
