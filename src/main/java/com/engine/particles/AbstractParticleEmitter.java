package com.engine.particles;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.dominion.ecs.api.Entity;

/**
 * Abstract base class for particle emitters providing common functionality.
 */
public abstract class AbstractParticleEmitter implements ParticleEmitter {

  // Common properties
  protected float x, y; // Position
  protected float emissionRate = 10; // Particles per second
  protected float emissionAccumulator = 0; // Time accumulator for emission
  protected int maxParticles = 1000; // Maximum number of particles
  protected boolean active = false; // Whether the emitter is active
  protected boolean continuous = false; // Whether to emit continuously
  protected Entity attachedEntity = null; // Entity this emitter is attached to

  // Particle pool
  protected final List<Particle> particles = new ArrayList<>();

  @Override
  public void update(float deltaTime) {
    // Update emission (if continuous)
    if (continuous && active) {
      emissionAccumulator += deltaTime;
      float emissionInterval = 1.0f / emissionRate;

      while (emissionAccumulator >= emissionInterval) {
        emit(1);
        emissionAccumulator -= emissionInterval;
      }
    }

    // Update position if attached to an entity
    updateAttachedPosition();

    // Update all particles
    for (int i = particles.size() - 1; i >= 0; i--) {
      Particle p = particles.get(i);
      p.update(deltaTime);

      // Remove inactive particles
      if (!p.isActive()) {
        // Fast remove by swapping with last element
        int lastIndex = particles.size() - 1;
        if (i < lastIndex) {
          particles.set(i, particles.get(lastIndex));
        }
        particles.remove(lastIndex);
      }
    }
  }

  @Override
  public void render(Graphics2D g) {
    for (Particle p : particles) {
      p.render(g);
    }
  }

  @Override
  public void emit(int count) {
    for (int i = 0; i < count && particles.size() < maxParticles; i++) {
      particles.add(createParticle());
    }
  }

  @Override
  public void burst(int count) {
    boolean prevContinuous = continuous;
    continuous = false;
    emit(count);
    continuous = prevContinuous;
  }

  @Override
  public void start() {
    active = true;
    continuous = true;
  }

  @Override
  public void stop() {
    active = false;
    continuous = false;
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public float getX() {
    return x;
  }

  @Override
  public float getY() {
    return y;
  }

  @Override
  public List<Particle> getParticles() {
    return particles;
  }

  @Override
  public void setAttachedEntity(Entity entity) {
    this.attachedEntity = entity;
  }

  @Override
  public Entity getAttachedEntity() {
    return attachedEntity;
  }

  @Override
  public void configure(Map<String, Object> params) {
    if (params.containsKey("x"))
      this.x = ((Number) params.get("x")).floatValue();
    if (params.containsKey("y"))
      this.y = ((Number) params.get("y")).floatValue();
    if (params.containsKey("emissionRate"))
      this.emissionRate = ((Number) params.get("emissionRate")).floatValue();
    if (params.containsKey("maxParticles"))
      this.maxParticles = ((Number) params.get("maxParticles")).intValue();
  }

  /**
   * Create a new particle with emitter-specific properties
   *
   * @return A newly created particle
   */
  protected abstract Particle createParticle();

  /**
   * Update position if attached to an entity
   */
  protected void updateAttachedPosition() {
    if (attachedEntity != null) {
      // Try to get Transform component from attached entity
      var transform = attachedEntity.get(com.engine.components.Transform.class);
      if (transform != null) {
        this.x = (float) transform.getX();
        this.y = (float) transform.getY();
      }
    }
  }
}
