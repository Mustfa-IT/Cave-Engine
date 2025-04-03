package com.engine.entity;

import dev.dominion.ecs.api.Entity;

/**
 * Interface for registering entities with a container (like a Scene)
 */
public interface EntityRegistrar {
  /**
   * Register an entity with this registrar
   *
   * @param entity The entity to register
   * @return The registered entity
   */
  Entity registerEntity(Entity entity);
}
