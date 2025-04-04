package com.engine.physics;

import org.jbox2d.dynamics.contacts.Contact;
import dev.dominion.ecs.api.Entity;

/**
 * Represents collision data between two entities
 */
public class Collision {
  private final Entity entityA;
  private final Entity entityB;
  private final Contact contact;

  public Collision(Entity entityA, Entity entityB, Contact contact) {
    this.entityA = entityA;
    this.entityB = entityB;
    this.contact = contact;
  }

  /**
   * Get the first entity involved in the collision
   * 
   * @return The first entity
   */
  public Entity getEntityA() {
    return entityA;
  }

  /**
   * Get the second entity involved in the collision
   * 
   * @return The second entity
   */
  public Entity getEntityB() {
    return entityB;
  }

  /**
   * Get the physics contact information
   * 
   * @return The Box2D contact
   */
  public Contact getContact() {
    return contact;
  }

  /**
   * Check if a specific entity is involved in this collision
   * 
   * @param entity The entity to check
   * @return True if the entity is involved in this collision
   */
  public boolean involves(Entity entity) {
    return entity.equals(entityA) || entity.equals(entityB);
  }

  /**
   * Get the other entity involved in the collision
   * 
   * @param entity One of the entities in the collision
   * @return The other entity, or null if the provided entity is not part of this
   *         collision
   */
  public Entity getOther(Entity entity) {
    if (entity.equals(entityA)) {
      return entityB;
    } else if (entity.equals(entityB)) {
      return entityA;
    }
    return null;
  }
}
