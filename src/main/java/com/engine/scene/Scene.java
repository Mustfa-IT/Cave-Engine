package com.engine.scene;

import java.util.ArrayList;
import java.util.List;

import com.engine.components.PhysicsBodyComponent;
import com.engine.core.GameEngine;
import com.engine.entity.EntityFactory;
import com.engine.entity.EntityRegistrar;

import dev.dominion.ecs.api.Entity;

/**
 * Represents a game scene with entities
 */
public abstract class Scene implements EntityRegistrar {
  protected final EntityFactory entityFactory;
  protected GameEngine engine;
  protected final List<Entity> sceneEntities = new ArrayList<>();

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
   * Register an entity with this scene
   *
   * @param entity The entity to register
   */
  @Override
  public Entity registerEntity(Entity entity) {
    if (entity != null) {
      sceneEntities.add(entity);
    }
    return entity;
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
    this.initialize();
    // Default implementation does nothing, can be overridden by scenes
  }

  /**
   * Called when this scene becomes inactive
   *
   */
  public void onDeactivate() {
    // Clean up all entities created in this scene
    if (engine != null) {
      cleanupEntities();
    }
  }

  /**
   * Clean up all entities created by this scene
   */
  protected void cleanupEntities() {
    System.out.println("Scene Size " + sceneEntities.size());
    for (Entity entity : sceneEntities) {
      // Then delete the entity
      var p = entity.get(PhysicsBodyComponent.class);
      if (p != null)
        engine.getPhysicsWorld().removeBody(p.getBody());

      engine.getEcs().deleteEntity(entity);
    }
    sceneEntities.clear();
    System.out.println("Scene entities cleaned up");
  }
}
