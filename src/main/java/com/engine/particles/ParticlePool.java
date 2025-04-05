package com.engine.particles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * Pool of reusable particle objects to reduce garbage collection overhead.
 */
public class ParticlePool {
  private static final Logger LOGGER = Logger.getLogger(ParticlePool.class.getName());

  private final Queue<Particle> availableParticles;
  private final int initialCapacity;
  private int peakUsage = 0;
  private int totalCreated = 0;

  /**
   * Create a new particle pool with the specified initial capacity
   *
   * @param initialCapacity Number of particles to pre-allocate
   */
  public ParticlePool(int initialCapacity) {
    this.initialCapacity = initialCapacity;
    this.availableParticles = new ConcurrentLinkedQueue<>();

    // Pre-allocate particles
    for (int i = 0; i < initialCapacity; i++) {
      availableParticles.add(new Particle(0, 0, 1, Color.WHITE, 1));
      totalCreated++;
    }

    LOGGER.info("Particle pool created with " + initialCapacity + " particles");
  }

  /**
   * Get a particle from the pool or create a new one if needed
   *
   * @param x        X position
   * @param y        Y position
   * @param size     Size of the particle
   * @param color    Color of the particle
   * @param lifetime Lifetime in seconds
   * @return A particle ready to use
   */
  public Particle obtainParticle(float x, float y, float size, Color color, float lifetime) {
    Particle p = availableParticles.poll();

    if (p == null) {
      // Create a new particle if none available
      p = new Particle(x, y, size, color, lifetime);
      totalCreated++;
    } else {
      // Reset and reuse existing particle
      p.reset(x, y, size, color, lifetime);
    }

    // Track usage statistics
    int currentUsage = totalCreated - availableParticles.size();
    if (currentUsage > peakUsage) {
      peakUsage = currentUsage;
    }

    return p;
  }

  /**
   * Return a particle to the pool for reuse
   *
   * @param particle The particle to recycle
   */
  public void recycleParticle(Particle particle) {
    // Clear any references the particle might be holding
    particle.prepareForPool();

    // Add back to the available queue
    availableParticles.offer(particle);
  }

  /**
   * Get the number of available particles in the pool
   *
   * @return Count of available particles
   */
  public int getAvailableCount() {
    return availableParticles.size();
  }

  /**
   * Clean up any particles that have been marked for recycling
   */
  public void cleanup() {
    // This would be called periodically to return inactive particles to the pool
    // The implementation will depend on how particles are tracked in the system
  }

  /**
   * Get peak usage statistics
   *
   * @return The maximum number of particles used simultaneously
   */
  public int getPeakUsage() {
    return peakUsage;
  }

  /**
   * Get the total number of particles created by this pool
   *
   * @return Total particle count
   */
  public int getTotalCreated() {
    return totalCreated;
  }

  /**
   * Recycle a list of particles back to the pool
   *
   * @param particles List of particles to recycle
   */
  public void recycleParticles(List<Particle> particles) {
    for (Particle p : particles) {
      if (p != null) {
        recycleParticle(p);
      }
    }
  }
}
