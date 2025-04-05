package com.engine.particles;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Represents a single particle in the particle system.
 * Contains common properties that all particle types share.
 */
public class Particle {
  // Position
  private float x, y;
  // Velocity
  private float vx, vy;
  // Acceleration
  private float ax, ay;
  // Size
  private float size;
  private float initialSize;
  private float finalSize;
  // Color and opacity
  private Color color;
  private float alpha = 1.0f;
  // Rotation
  private float rotation;
  private float rotationSpeed;
  // Lifetime
  private float maxLifetime;
  private float remainingLifetime;
  // Active state
  private boolean active = true;

  // For pooling - track if this particle is marked for recycling
  private boolean recyclable = false;

  // For optimization - last frame rendered (to avoid redundant rendering)
  private long lastFrameRendered = -1;

  // For batch rendering - data needed by batch renderer
  private int batchId = -1;

  /**
   * Create a new particle with default properties
   */
  public Particle(float x, float y, float size, Color color, float maxLifetime) {
    this.x = x;
    this.y = y;
    this.size = size;
    this.initialSize = size;
    this.finalSize = size;
    this.color = color;
    this.maxLifetime = maxLifetime;
    this.remainingLifetime = maxLifetime;
  }

  /**
   * Reset a particle for reuse from pool
   */
  public void reset(float x, float y, float size, Color color, float maxLifetime) {
    this.x = x;
    this.y = y;
    this.vx = 0;
    this.vy = 0;
    this.ax = 0;
    this.ay = 0;
    this.size = size;
    this.initialSize = size;
    this.finalSize = size;
    this.color = color;
    this.alpha = 1.0f;
    this.rotation = 0;
    this.rotationSpeed = 0;
    this.maxLifetime = maxLifetime;
    this.remainingLifetime = maxLifetime;
    this.active = true;
    this.recyclable = false;
    this.batchId = -1;
    this.lastFrameRendered = -1;
  }

  /**
   * Prepare the particle for returning to pool (clear references)
   */
  public void prepareForPool() {
    this.color = null;
    this.active = false;
    this.recyclable = true;
  }

  /**
   * Update particle state based on delta time
   *
   * @param deltaTime Time since last update in seconds
   */
  public void update(float deltaTime) {
    if (!active)
      return;

    // Update remaining lifetime
    remainingLifetime -= deltaTime;
    if (remainingLifetime <= 0) {
      active = false;
      recyclable = true;
      return;
    }

    // Calculate life progress (0 to 1)
    float lifeProgress = 1.0f - (remainingLifetime / maxLifetime);

    // Update position based on velocity (optimize by integrating acceleration)
    float halfDeltaTimeSquared = 0.5f * deltaTime * deltaTime;
    x += vx * deltaTime + ax * halfDeltaTimeSquared;
    y += vy * deltaTime + ay * halfDeltaTimeSquared;

    // Update velocity based on acceleration
    vx += ax * deltaTime;
    vy += ay * deltaTime;

    // Update rotation
    rotation += rotationSpeed * deltaTime;

    // Update size based on initial and final size
    size = initialSize + (finalSize - initialSize) * lifeProgress;
  }

  /**
   * Render the particle
   *
   * @param g Graphics context
   */
  public void render(Graphics2D g) {
    if (!active)
      return;

    // Save the original transform and composite
    var originalTransform = g.getTransform();
    var originalComposite = g.getComposite();

    // Apply transparency if needed
    if (alpha < 1.0f) {
      g.setComposite(java.awt.AlphaComposite.getInstance(
          java.awt.AlphaComposite.SRC_OVER, alpha));
    }

    // Apply transform for rotation
    g.translate(x, y);
    g.rotate(rotation);

    // Draw particle (default is a square)
    float halfSize = size / 2;
    g.setColor(color);
    g.fillRect((int) (-halfSize), (int) (-halfSize), (int) size, (int) size);

    // Restore original transform and composite
    g.setTransform(originalTransform);
    g.setComposite(originalComposite);
  }

  // Getters and setters
  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public float getX() {
    return x;
  }

  public void setX(float x) {
    this.x = x;
  }

  public float getY() {
    return y;
  }

  public void setY(float y) {
    this.y = y;
  }

  public float getVelocityX() {
    return vx;
  }

  public void setVelocityX(float vx) {
    this.vx = vx;
  }

  public float getVelocityY() {
    return vy;
  }

  public void setVelocityY(float vy) {
    this.vy = vy;
  }

  public float getAccelerationX() {
    return ax;
  }

  public void setAccelerationX(float ax) {
    this.ax = ax;
  }

  public float getAccelerationY() {
    return ay;
  }

  public void setAccelerationY(float ay) {
    this.ay = ay;
  }

  public float getSize() {
    return size;
  }

  public void setSize(float size) {
    this.size = size;
  }

  public void setInitialSize(float size) {
    this.initialSize = size;
  }

  public void setFinalSize(float size) {
    this.finalSize = size;
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public float getAlpha() {
    return alpha;
  }

  public void setAlpha(float alpha) {
    this.alpha = alpha;
  }

  public float getRotation() {
    return rotation;
  }

  public void setRotation(float rotation) {
    this.rotation = rotation;
  }

  public float getRotationSpeed() {
    return rotationSpeed;
  }

  public void setRotationSpeed(float rotationSpeed) {
    this.rotationSpeed = rotationSpeed;
  }

  public float getMaxLifetime() {
    return maxLifetime;
  }

  public float getRemainingLifetime() {
    return remainingLifetime;
  }

  /**
   * Marks this particle as recyclable (ready to return to pool)
   */
  public void markAsRecyclable() {
    this.recyclable = true;
    this.active = false;
  }

  /**
   * Checks if this particle is ready for recycling
   */
  public boolean isRecyclable() {
    return recyclable;
  }

  /**
   * Set the batch ID for batch rendering
   */
  public void setBatchId(int batchId) {
    this.batchId = batchId;
  }

  /**
   * Get the batch ID
   */
  public int getBatchId() {
    return batchId;
  }

  /**
   * Set the last frame this particle was rendered
   */
  public void setLastFrameRendered(long frameNumber) {
    this.lastFrameRendered = frameNumber;
  }

  /**
   * Check if the particle was already rendered in the current frame
   */
  public boolean wasRenderedInFrame(long frameNumber) {
    return lastFrameRendered == frameNumber;
  }
}
