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
      return;
    }

    // Calculate life progress (0 to 1)
    float lifeProgress = 1.0f - (remainingLifetime / maxLifetime);

    // Update position based on velocity
    x += vx * deltaTime;
    y += vy * deltaTime;

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
}
