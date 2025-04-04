package com.engine.scene;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.engine.components.PhysicsBodyComponent;
import com.engine.components.RenderableComponent;
import com.engine.components.UIComponent;
import com.engine.core.GameEngine;
import com.engine.entity.EntityFactory;
import com.engine.entity.EntityRegistrar;

import dev.dominion.ecs.api.Entity;

/**
 * Represents a game scene with entities
 */
public abstract class Scene implements EntityRegistrar {
  private static final Logger LOGGER = Logger.getLogger(Scene.class.getName());
  protected final EntityFactory entityFactory;
  protected GameEngine engine;
  protected final Set<Entity> sceneEntities = new HashSet<>();

  // Track whether this scene is active
  private boolean isActive = false;

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
      LOGGER.fine("Entity registered with scene: " + entity);
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
    LOGGER.info("Scene activated: " + this.getClass().getSimpleName());
    isActive = true;
    this.initialize();
  }

  /**
   * Called when this scene becomes inactive
   */
  public void onDeactivate() {
    LOGGER.info("Scene deactivated: " + this.getClass().getSimpleName());
    isActive = false;
    // Clean up all entities created in this scene
    if (engine != null) {
      cleanupEntities();
    }
  }

  /**
   * Check if this scene is currently active
   */
  public boolean isActive() {
    return isActive;
  }

  /**
   * Clean up all entities created by this scene
   */
  protected void cleanupEntities() {
    LOGGER.info("Cleaning up entities. Count: " + sceneEntities.size());

    // Make a copy to avoid concurrent modification
    Set<Entity> entities = new HashSet<>(sceneEntities);
    sceneEntities.clear();

    // Clean up component resources but don't try to delete entities
    for (Entity entity : entities) {
      try {
        if (entity == null)
          continue;

        // Clean up just the components
        cleanupEntityComponents(entity);
      } catch (Exception e) {
        LOGGER.warning("Error cleaning up entity components: " + e.getMessage());
      }
    }

    // Signal to the garbage collector
    entities = null;
    System.gc();

    LOGGER.info("Scene entities cleaned up");
  }

  /**
   * Clean up just the components of an entity, without trying to delete the
   * entity itself
   */
  private void cleanupEntityComponents(Entity entity) {
    // Clean up physics component
    try {
      PhysicsBodyComponent physicsComponent = entity.get(PhysicsBodyComponent.class);
      if (physicsComponent != null && physicsComponent.getBody() != null) {
        engine.getPhysicsWorld().removeBody(physicsComponent.getBody());
        LOGGER.fine("Removed physics component from entity");
      }
    } catch (Exception e) {
      // Just log and continue
      LOGGER.fine("Could not clean up physics component: " + e.getMessage());
    }

    // Clean up UI component
    try {
      UIComponent uiComponent = entity.get(UIComponent.class);
      if (uiComponent != null) {
        // Set it to invisible first to avoid render issues
        uiComponent.setVisible(false);
        LOGGER.fine("Disabled UI component");
      }
    } catch (Exception e) {
      // Just log and continue
      LOGGER.fine("Could not clean up UI component: " + e.getMessage());
    }

    // Clean up or mark renderable component as inactive
    try {
      RenderableComponent renderableComponent = entity.get(RenderableComponent.class);
      if (renderableComponent != null) {
        renderableComponent.setVisible(false);
        LOGGER.fine("Disabled renderable component");
      }
    } catch (Exception e) {
      // Just log and continue
      LOGGER.fine("Could not clean up renderable component: " + e.getMessage());
    }

    // We deliberately skip entity deletion to avoid the null chunk issue
    // Let the ECS garbage collector handle entity cleanup
  }

  /**
   * Force cleanup all scene resources
   * This can be called manually if needed for thorough cleanup
   */
  public void forceCleanup() {
    LOGGER.info("Force cleaning all scene resources");
    cleanupEntities();

    // Explicitly request garbage collection
    System.gc();

    // Reset any scene-specific state here
    sceneEntities.clear();
  }
}
