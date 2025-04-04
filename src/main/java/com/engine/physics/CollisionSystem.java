package com.engine.physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.contacts.Contact;

import com.engine.components.GameObjectComponent;
import com.engine.gameobject.GameObject;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * System for detecting and managing collisions between physics bodies
 */
@Singleton
public class CollisionSystem implements ContactListener {
  private static final Logger LOGGER = Logger.getLogger(CollisionSystem.class.getName());

  private final Dominion ecs;
  private final Map<String, List<Collision>> activeCollisions = new HashMap<>();

  @Inject
  public CollisionSystem(Dominion ecs) {
    this.ecs = ecs;
    LOGGER.info("Collision system initialized");
  }

  @Override
  public void beginContact(Contact contact) {
    Entity entityA = getEntityFromBody(contact.getFixtureA().getBody());
    Entity entityB = getEntityFromBody(contact.getFixtureB().getBody());

    if (entityA != null && entityB != null) {
      Collision collision = new Collision(entityA, entityB, contact);

      // Store this as an active collision
      storeCollision(collision);

      // Notify collision listeners
      notifyCollisionEnter(entityA, collision);
      notifyCollisionEnter(entityB, collision);
    }
  }

  @Override
  public void endContact(Contact contact) {
    Entity entityA = getEntityFromBody(contact.getFixtureA().getBody());
    Entity entityB = getEntityFromBody(contact.getFixtureB().getBody());

    if (entityA != null && entityB != null) {
      Collision collision = new Collision(entityA, entityB, contact);

      // Remove from active collisions
      removeCollision(collision);

      // Notify collision listeners
      notifyCollisionExit(entityA, collision);
      notifyCollisionExit(entityB, collision);
    }
  }

  @Override
  public void preSolve(Contact contact, Manifold oldManifold) {
    // Implementation for pre-solve collision handling if needed
  }

  @Override
  public void postSolve(Contact contact, ContactImpulse impulse) {
    // Implementation for post-solve collision handling if needed
  }

  private Entity getEntityFromBody(Body body) {
    if (body.getUserData() instanceof Entity) {
      return (Entity) body.getUserData();
    }
    return null;
  }

  private void storeCollision(Collision collision) {
    String entityAId = collision.getEntityA().toString();
    String entityBId = collision.getEntityB().toString();

    // Store collision for entity A
    activeCollisions.computeIfAbsent(entityAId, k -> new ArrayList<>()).add(collision);

    // Store collision for entity B
    activeCollisions.computeIfAbsent(entityBId, k -> new ArrayList<>()).add(collision);
  }

  private void removeCollision(Collision collision) {
    String entityAId = collision.getEntityA().toString();
    String entityBId = collision.getEntityB().toString();

    // Remove collision for entity A
    if (activeCollisions.containsKey(entityAId)) {
      activeCollisions.get(entityAId).removeIf(c -> c.getEntityA().equals(collision.getEntityB()) ||
          c.getEntityB().equals(collision.getEntityB()));
    }

    // Remove collision for entity B
    if (activeCollisions.containsKey(entityBId)) {
      activeCollisions.get(entityBId).removeIf(c -> c.getEntityA().equals(collision.getEntityA()) ||
          c.getEntityB().equals(collision.getEntityA()));
    }
  }

  private void notifyCollisionEnter(Entity entity, Collision collision) {
    GameObjectComponent component = entity.get(GameObjectComponent.class);
    if (component != null) {
      GameObject gameObject = component.getGameObject();
      if (gameObject != null) {
        gameObject.onCollisionEnter(collision);
      }
    }
  }

  private void notifyCollisionExit(Entity entity, Collision collision) {
    GameObjectComponent component = entity.get(GameObjectComponent.class);
    if (component != null) {
      GameObject gameObject = component.getGameObject();
      if (gameObject != null) {
        gameObject.onCollisionExit(collision);
      }
    }
  }

  /**
   * Get all active collisions for an entity
   *
   * @param entity The entity to check
   * @return List of active collisions
   */
  public List<Collision> getCollisionsForEntity(Entity entity) {
    return activeCollisions.getOrDefault(entity.toString(), new ArrayList<>());
  }

  /**
   * Check if an entity is colliding with any other entities
   *
   * @param entity The entity to check
   * @return True if the entity is involved in any active collisions
   */
  public boolean hasCollisions(Entity entity) {
    return !getCollisionsForEntity(entity).isEmpty();
  }

  /**
   * Get all entities colliding with the specified entity
   *
   * @param entity The entity to check
   * @return List of entities in collision
   */
  public List<Entity> getCollidingEntities(Entity entity) {
    List<Entity> collidingEntities = new ArrayList<>();

    List<Collision> collisions = getCollisionsForEntity(entity);
    for (Collision collision : collisions) {
      collidingEntities.add(collision.getOther(entity));
    }

    return collidingEntities;
  }

  /**
   * Check if an entity is colliding with another specific entity
   *
   * @param entity The entity to check
   * @param other  The other entity to check against
   * @return True if the entities are colliding
   */
  public boolean isCollidingWith(Entity entity, Entity other) {
    List<Collision> collisions = getCollisionsForEntity(entity);

    for (Collision collision : collisions) {
      if (collision.involves(other)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Find all entities of a specific type that are colliding with the given entity
   *
   * @param <T>           The component type to filter by
   * @param entity        The entity to check collisions for
   * @param componentType The component type to filter colliding entities by
   * @return List of entities with the specified component type that are colliding
   */
  public <T> List<Entity> findCollidingEntitiesWith(Entity entity, Class<T> componentType) {
    List<Entity> result = new ArrayList<>();
    List<Collision> collisions = getCollisionsForEntity(entity);

    for (Collision collision : collisions) {
      Entity other = collision.getOther(entity);
      if (other != null && other.has(componentType)) {
        result.add(other);
      }
    }

    return result;
  }
}
