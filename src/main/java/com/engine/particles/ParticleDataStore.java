package com.engine.particles;

import java.awt.Color;

/**
 * Memory-optimized storage for particle data using arrays instead of objects.
 * This improves cache locality and reduces GC pressure.
 */
public class ParticleDataStore {

  // Core particle properties in arrays for better memory locality
  private final float[] posX;
  private final float[] posY;
  private final float[] velX;
  private final float[] velY;
  private final float[] accX;
  private final float[] accY;
  private final float[] sizes;
  private final float[] rotations;
  private final float[] rotationSpeeds;
  private final float[] lifetimes;
  private final float[] remainingLifetimes;
  private final boolean[] active;

  // Color components
  private final int[] colors;
  private final float[] alphas;

  private final int capacity;
  private int count = 0;

  /**
   * Create a new particle data store with the specified capacity
   *
   * @param capacity Maximum number of particles to store
   */
  public ParticleDataStore(int capacity) {
    this.capacity = capacity;

    // Allocate arrays
    posX = new float[capacity];
    posY = new float[capacity];
    velX = new float[capacity];
    velY = new float[capacity];
    accX = new float[capacity];
    accY = new float[capacity];
    sizes = new float[capacity];
    rotations = new float[capacity];
    rotationSpeeds = new float[capacity];
    lifetimes = new float[capacity];
    remainingLifetimes = new float[capacity];
    active = new boolean[capacity];

    colors = new int[capacity];
    alphas = new float[capacity];
  }

  /**
   * Create a new particle
   *
   * @param x        X position
   * @param y        Y position
   * @param size     Particle size
   * @param color    Particle color
   * @param lifetime Particle lifetime
   * @return Index of the new particle or -1 if full
   */
  public int createParticle(float x, float y, float size, Color color, float lifetime) {
    if (count >= capacity) {
      return -1;
    }

    int index = count++;
    posX[index] = x;
    posY[index] = y;
    velX[index] = 0;
    velY[index] = 0;
    accX[index] = 0;
    accY[index] = 0;
    sizes[index] = size;
    rotations[index] = 0;
    rotationSpeeds[index] = 0;
    lifetimes[index] = lifetime;
    remainingLifetimes[index] = lifetime;
    active[index] = true;

    colors[index] = color.getRGB();
    alphas[index] = 1.0f;

    return index;
  }

  /**
   * Update all particles
   *
   * @param deltaTime Time since last update
   */
  public void updateAll(float deltaTime) {
    // Update all particles in one pass for better cache utilization
    for (int i = 0; i < count; i++) {
      if (!active[i])
        continue;

      // Update lifetime
      remainingLifetimes[i] -= deltaTime;
      if (remainingLifetimes[i] <= 0) {
        active[i] = false;
        continue;
      }

      // Update position and velocity with Verlet integration
      float halfDt2 = 0.5f * deltaTime * deltaTime;
      posX[i] += velX[i] * deltaTime + accX[i] * halfDt2;
      posY[i] += velY[i] * deltaTime + accY[i] * halfDt2;

      velX[i] += accX[i] * deltaTime;
      velY[i] += accY[i] * deltaTime;

      // Update rotation
      rotations[i] += rotationSpeeds[i] * deltaTime;

      // Update alpha based on lifetime
      float progress = 1.0f - (remainingLifetimes[i] / lifetimes[i]);
      alphas[i] = 1.0f - progress; // Linear fade out
    }

    // Compact arrays by removing inactive particles
    compactArrays();
  }

  /**
   * Remove inactive particles and compact arrays
   */
  private void compactArrays() {
    int writeIndex = 0;

    for (int readIndex = 0; readIndex < count; readIndex++) {
      if (active[readIndex]) {
        if (writeIndex != readIndex) {
          // Copy data from read index to write index
          posX[writeIndex] = posX[readIndex];
          posY[writeIndex] = posY[readIndex];
          velX[writeIndex] = velX[readIndex];
          velY[writeIndex] = velY[readIndex];
          accX[writeIndex] = accX[readIndex];
          accY[writeIndex] = accY[readIndex];
          sizes[writeIndex] = sizes[readIndex];
          rotations[writeIndex] = rotations[readIndex];
          rotationSpeeds[writeIndex] = rotationSpeeds[readIndex];
          lifetimes[writeIndex] = lifetimes[readIndex];
          remainingLifetimes[writeIndex] = remainingLifetimes[readIndex];
          active[writeIndex] = active[readIndex];
          colors[writeIndex] = colors[readIndex];
          alphas[writeIndex] = alphas[readIndex];
        }
        writeIndex++;
      }
    }

    count = writeIndex;
  }

  /**
   * Get the number of active particles
   *
   * @return Number of active particles
   */
  public int getCount() {
    return count;
  }

  /**
   * Get capacity of the store
   *
   * @return Maximum number of particles
   */
  public int getCapacity() {
    return capacity;
  }

  /**
   * Get X position of a particle
   */
  public float getX(int index) {
    return posX[index];
  }

  /**
   * Get Y position of a particle
   */
  public float getY(int index) {
    return posY[index];
  }

  /**
   * Get size of a particle
   */
  public float getSize(int index) {
    return sizes[index];
  }

  /**
   * Get rotation of a particle
   */
  public float getRotation(int index) {
    return rotations[index];
  }

  /**
   * Get color of a particle
   */
  public Color getColor(int index) {
    return new Color(colors[index], true);
  }

  /**
   * Get alpha value of a particle
   */
  public float getAlpha(int index) {
    return alphas[index];
  }

  /**
   * Check if a particle is active
   */
  public boolean isActive(int index) {
    return active[index];
  }
}
