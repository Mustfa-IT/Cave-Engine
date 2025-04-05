package com.engine.particles;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.engine.events.EventSystem;
import com.engine.events.EventTypes;
import com.engine.graph.CustomRenderer;
import com.engine.graph.RenderSystem;
import com.engine.particles.emitters.PhysicalParticleEmitter;
import com.engine.physics.CollisionSystem;

import dev.dominion.ecs.api.Entity;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.contacts.Contact;

/**
 * Main system for managing particle emitters and integrating with other engine
 * systems.
 */
@Singleton
public class ParticleSystem implements CustomRenderer {

  private static final Logger LOGGER = Logger.getLogger(ParticleSystem.class.getName());

  private final List<ParticleEmitter> emitters = new ArrayList<>();
  private final Map<Entity, List<ParticleEmitter>> entityEmitters = new HashMap<>();
  private final ParticleEmitterFactory emitterFactory;
  private final EventSystem eventSystem;
  private final RenderSystem renderSystem;

  private int totalParticleCount = 0;
  private boolean paused = false;
  private int maxParticlesTotal = 10000; // Global limit across all emitters

  @Inject
  public ParticleSystem(ParticleEmitterFactory emitterFactory,
      EventSystem eventSystem,
      RenderSystem renderSystem,
      CollisionSystem collisionSystem) {
    this.emitterFactory = emitterFactory;
    this.eventSystem = eventSystem;
    this.renderSystem = renderSystem;

    // Register as a custom renderer with the render system
    renderSystem.addCustomRenderer(this);

    // Set up collision handling for physical particles
    setupCollisionHandling(collisionSystem);

    LOGGER.info("Particle system initialized");
  }

  /**
   * Create a particle emitter of the specified type
   *
   * @param type   The type of emitter to create
   * @param x      X position
   * @param y      Y position
   * @param params Configuration parameters
   * @return The created emitter, already registered with the system
   */
  public ParticleEmitter createEmitter(String type, float x, float y, Map<String, Object> params) {
    ParticleEmitter emitter = emitterFactory.createEmitter(type, x, y, params);
    addEmitter(emitter);
    return emitter;
  }

  /**
   * Add an existing emitter to the system
   *
   * @param emitter The emitter to add
   */
  public void addEmitter(ParticleEmitter emitter) {
    synchronized (emitters) {
      emitters.add(emitter);
    }
    LOGGER.fine("Added particle emitter at " + emitter.getX() + ", " + emitter.getY());

    // Fire event for emitter added
    eventSystem.fireEvent(EventTypes.PARTICLE_EMITTER_ADDED,
        "emitter", emitter);
  }

  /**
   * Remove an emitter from the system
   *
   * @param emitter The emitter to remove
   */
  public void removeEmitter(ParticleEmitter emitter) {
    synchronized (emitters) {
      emitters.remove(emitter);
    }

    // Also remove from entity associations
    Entity attachedEntity = emitter.getAttachedEntity();
    if (attachedEntity != null) {
      synchronized (entityEmitters) {
        List<ParticleEmitter> entityEmitterList = entityEmitters.get(attachedEntity);
        if (entityEmitterList != null) {
          entityEmitterList.remove(emitter);
          if (entityEmitterList.isEmpty()) {
            entityEmitters.remove(attachedEntity);
          }
        }
      }
    }

    // Fire event for emitter removed
    eventSystem.fireEvent(EventTypes.PARTICLE_EMITTER_REMOVED,
        "emitter", emitter);
  }

  /**
   * Attach an emitter to an entity
   *
   * @param emitter The emitter to attach
   * @param entity  The entity to attach to
   */
  public void attachEmitterToEntity(ParticleEmitter emitter, Entity entity) {
    // Set the entity in the emitter
    emitter.setAttachedEntity(entity);

    // Add to entity association map
    entityEmitters.computeIfAbsent(entity, k -> new ArrayList<>()).add(emitter);

    // Add emitter to system if not already added
    if (!emitters.contains(emitter)) {
      addEmitter(emitter);
    }
  }

  /**
   * Get all emitters attached to an entity
   *
   * @param entity The entity
   * @return List of attached emitters
   */
  public List<ParticleEmitter> getEntityEmitters(Entity entity) {
    return entityEmitters.getOrDefault(entity, new ArrayList<>());
  }

  /**
   * Update all particle emitters
   *
   * @param deltaTime Time since last update in seconds
   */
  public void update(float deltaTime) {
    if (paused) {
      return;
    }

    // Create a safe copy to iterate over
    List<ParticleEmitter> emittersCopy;
    List<ParticleEmitter> emittersToRemove = new ArrayList<>();

    synchronized (emitters) {
      emittersCopy = new ArrayList<>(emitters);
    }

    totalParticleCount = 0;

    // Update emitters and identify inactive ones
    for (ParticleEmitter emitter : emittersCopy) {
      // Skip inactive emitters with no particles
      if (!emitter.isActive() && emitter.getParticles().isEmpty()) {
        emittersToRemove.add(emitter);
        continue;
      }

      // Update the emitter
      emitter.update(deltaTime);

      // Count particles
      totalParticleCount += emitter.getParticles().size();
    }

    // Now safely remove the inactive emitters
    if (!emittersToRemove.isEmpty()) {
      synchronized (emitters) {
        emitters.removeAll(emittersToRemove);
      }
    }

    // Fire particle count event
    eventSystem.fireEvent(EventTypes.PARTICLE_COUNT_UPDATED,
        "count", totalParticleCount);
  }

  /**
   * Render callback - called by the RenderSystem
   */
  @Override
  public void render(Graphics2D g) {
    // Create a safe copy of the emitters list to avoid
    // ConcurrentModificationException
    List<ParticleEmitter> emittersCopy;
    synchronized (emitters) {
      emittersCopy = new ArrayList<>(emitters);
    }

    // Iterate over the copy instead of the original list
    for (ParticleEmitter emitter : emittersCopy) {
      emitter.render(g);
    }
  }

  /**
   * Get render priority (higher means render later)
   */
  @Override
  public int getPriority() {
    return 500; // Render particles on top of most things
  }

  /**
   * Create an explosion effect at the specified position
   *
   * @param x             X position
   * @param y             Y position
   * @param particleCount Number of particles
   * @return The explosion emitter
   */
  public ParticleEmitter createExplosion(float x, float y, int particleCount) {
    ParticleEmitter emitter = emitterFactory.createExplosionEmitter(x, y);
    addEmitter(emitter);
    emitter.burst(particleCount);
    return emitter;
  }

  /**
   * Create a continuous fire effect at the specified position
   *
   * @param x X position
   * @param y Y position
   * @return The fire emitter
   */
  public ParticleEmitter createFire(float x, float y) {
    ParticleEmitter emitter = emitterFactory.createFireEmitter(x, y);
    addEmitter(emitter);
    emitter.start();
    return emitter;
  }

  /**
   * Create a smoke effect at the specified position
   *
   * @param x X position
   * @param y Y position
   * @return The smoke emitter
   */
  public ParticleEmitter createSmoke(float x, float y) {
    ParticleEmitter emitter = emitterFactory.createSmokeEmitter(x, y);
    addEmitter(emitter);
    emitter.start();
    return emitter;
  }

  /**
   * Get the total particle count across all emitters
   *
   * @return Total particle count
   */
  public int getParticleCount() {
    return totalParticleCount;
  }

  /**
   * Get the emitter factory
   *
   * @return The emitter factory
   */
  public ParticleEmitterFactory getFactory() {
    return emitterFactory;
  }

  /**
   * Pause or resume the particle system
   *
   * @param paused Whether the system should be paused
   */
  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  /**
   * Clear all particles and emitters
   */
  public void clearAll() {
    emitters.clear();
    entityEmitters.clear();
    totalParticleCount = 0;
    LOGGER.info("All particle emitters cleared");
  }

  /**
   * Set up collision handling for physical particles
   */
  private void setupCollisionHandling(CollisionSystem collisionSystem) {
    // Listen for collision events
    eventSystem.addEventListener(EventTypes.COLLISION_BEGIN, event -> {
      Contact contact = (Contact) event.getData("collision");
      if (contact != null) {
        Body bodyA = contact.getFixtureA().getBody();
        Body bodyB = contact.getFixtureB().getBody();

        // Check if either body is a particle
        handleParticleCollision(bodyA);
        handleParticleCollision(bodyB);
      }
      return;
    });
  }

  /**
   * Handle a collision involving a particle
   *
   * @param body The physics body that collided
   */
  private void handleParticleCollision(Body body) {
    if (body.getUserData() instanceof PhysicalParticleEmitter.PhysicalParticle) {
      // Find the emitter for this particle
      for (ParticleEmitter emitter : emitters) {
        if (emitter instanceof PhysicalParticleEmitter) {
          ((PhysicalParticleEmitter) emitter).handleCollision(body);
          break;
        }
      }
    }
  }
}
