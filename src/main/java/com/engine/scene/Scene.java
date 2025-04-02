package com.engine.scene;

import java.util.ArrayList;
import java.util.List;

import com.engine.core.GameEngine;
import com.engine.entity.EntityFactory;

/**
 * Represents a game scene with entities
 */
public abstract class Scene {
  protected final EntityFactory entityFactory;
  protected GameEngine engine;

  public Scene(EntityFactory entityFactory) {
    this.entityFactory = entityFactory;
  }

  /**
   * Sets the reference to the game engine
   *
   * @param engine The game engine instance
   */
  public void setEngine(GameEngine engine) {
    this.engine = engine;
  }

  /**
   * Initialize the scene
   */
  public abstract void initialize();

  /**
   * Update the scene logic
   *
   * @param deltaTime Time since last update
   */
  public void update(double deltaTime) {
    // Default implementation does nothing, can be overridden by scenes
  }

  /**
   * Called when this scene becomes active
   */
  public void onActivate() {
    // Default implementation does nothing, can be overridden by scenes
  }

  /**
   * Called when this scene becomes inactive
   */
  public void onDeactivate() {
    // Default implementation does nothing, can be overridden by scenes
  }
}
