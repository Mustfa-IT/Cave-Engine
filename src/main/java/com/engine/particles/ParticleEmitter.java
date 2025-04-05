package com.engine.particles;

import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;

import dev.dominion.ecs.api.Entity;

/**
 * Interface defining the behavior of particle emitters.
 * All concrete emitter types must implement this interface.
 */
public interface ParticleEmitter {

  /**
   * Update all particles in this emitter
   * 
   * @param deltaTime Time since last update in seconds
   */
  void update(float deltaTime);

  /**
   * Render all active particles
   * 
   * @param g Graphics context
   */
  void render(Graphics2D g);

  /**
   * Emit a specific number of particles
   * 
   * @param count Number of particles to emit
   */
  void emit(int count);

  /**
   * Emit a burst of particles and then stop
   * 
   * @param count Number of particles in the burst
   */
  void burst(int count);

  /**
   * Start continuous emission at the configured rate
   */
  void start();

  /**
   * Stop continuous emission
   */
  void stop();

  /**
   * Check if the emitter is currently active
   * 
   * @return true if active, false otherwise
   */
  boolean isActive();

  /**
   * Set the emitter position
   * 
   * @param x X coordinate
   * @param y Y coordinate
   */
  void setPosition(float x, float y);

  /**
   * Get the emitter position X coordinate
   * 
   * @return X position
   */
  float getX();

  /**
   * Get the emitter position Y coordinate
   * 
   * @return Y position
   */
  float getY();

  /**
   * Configure the emitter with a set of parameters
   * 
   * @param params Map of parameter name to value
   */
  void configure(Map<String, Object> params);

  /**
   * Get all active particles
   * 
   * @return List of active particles
   */
  List<Particle> getParticles();

  /**
   * Set the entity this emitter is attached to (if any)
   * 
   * @param entity The entity to attach to
   */
  void setAttachedEntity(Entity entity);

  /**
   * Get the entity this emitter is attached to (if any)
   * 
   * @return The attached entity or null
   */
  Entity getAttachedEntity();
}
