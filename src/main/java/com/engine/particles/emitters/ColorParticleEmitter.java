package com.engine.particles.emitters;

import java.awt.Color;
import java.util.Map;
import java.util.Random;

import com.engine.particles.AbstractParticleEmitter;
import com.engine.particles.Particle;

/**
 * Emitter that produces simple colored particles.
 */
public class ColorParticleEmitter extends AbstractParticleEmitter {

  private final Random random = new Random();

  // Particle properties
  private Color startColor = Color.WHITE;
  private Color endColor = new Color(255, 255, 255, 0); // Fade to transparent
  private float minSize = 5.0f;
  private float maxSize = 10.0f;
  private float minSpeed = 50.0f;
  private float maxSpeed = 100.0f;
  private float minLifetime = 1.0f;
  private float maxLifetime = 2.0f;
  private float spreadAngle = (float) Math.PI * 2; // 360 degrees
  private float baseAngle = 0; // Initial emission direction
  private float gravity = 0;

  public ColorParticleEmitter(float x, float y) {
    setPosition(x, y);
  }

  @Override
  protected Particle createParticle() {
    // Randomize lifetime
    float lifetime = minLifetime + random.nextFloat() * (maxLifetime - minLifetime);

    // Randomize size
    float size = minSize + random.nextFloat() * (maxSize - minSize);

    // Create base particle
    Particle p = new Particle(x, y, size, startColor, lifetime);

    // Randomize velocity based on angle and speed
    float angle = baseAngle + (random.nextFloat() - 0.5f) * spreadAngle;
    float speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed);
    p.setVelocityX((float) Math.cos(angle) * speed);
    p.setVelocityY((float) Math.sin(angle) * speed);

    // Set acceleration (gravity)
    p.setAccelerationY(gravity);

    // Randomize rotation
    p.setRotation((float) (random.nextFloat() * Math.PI * 2));
    p.setRotationSpeed((float) ((random.nextFloat() - 0.5f) * Math.PI));

    // Set size transition
    p.setInitialSize(size);
    p.setFinalSize(size * 0.5f); // Shrink to half size

    return p;
  }

  @Override
  public void configure(Map<String, Object> params) {
    super.configure(params);

    if (params.containsKey("startColor"))
      this.startColor = (Color) params.get("startColor");
    if (params.containsKey("endColor"))
      this.endColor = (Color) params.get("endColor");
    if (params.containsKey("minSize"))
      this.minSize = ((Number) params.get("minSize")).floatValue();
    if (params.containsKey("maxSize"))
      this.maxSize = ((Number) params.get("maxSize")).floatValue();
    if (params.containsKey("minSpeed"))
      this.minSpeed = ((Number) params.get("minSpeed")).floatValue();
    if (params.containsKey("maxSpeed"))
      this.maxSpeed = ((Number) params.get("maxSpeed")).floatValue();
    if (params.containsKey("minLifetime"))
      this.minLifetime = ((Number) params.get("minLifetime")).floatValue();
    if (params.containsKey("maxLifetime"))
      this.maxLifetime = ((Number) params.get("maxLifetime")).floatValue();
    if (params.containsKey("spreadAngle"))
      this.spreadAngle = ((Number) params.get("spreadAngle")).floatValue();
    if (params.containsKey("baseAngle"))
      this.baseAngle = ((Number) params.get("baseAngle")).floatValue();
    if (params.containsKey("gravity"))
      this.gravity = ((Number) params.get("gravity")).floatValue();
  }
}
