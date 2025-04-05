package com.engine.particles.emitters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Random;

import com.engine.particles.AbstractParticleEmitter;
import com.engine.particles.Particle;

/**
 * Emitter that produces particles using sprite images.
 */
public class SpriteParticleEmitter extends AbstractParticleEmitter {

  public static class SpriteParticle extends Particle {
    private BufferedImage sprite;

    public SpriteParticle(float x, float y, float size, Color color, float lifetime, BufferedImage sprite) {
      super(x, y, size, color, lifetime);
      this.sprite = sprite;
    }

    @Override
    public void render(Graphics2D g) {
      if (!isActive())
        return;

      // Save the original transform and composite
      var originalTransform = g.getTransform();
      var originalComposite = g.getComposite();

      // Apply transparency if needed
      if (getAlpha() < 1.0f) {
        g.setComposite(java.awt.AlphaComposite.getInstance(
            java.awt.AlphaComposite.SRC_OVER, getAlpha()));
      }

      // Apply transform for rotation and position
      g.translate(getX(), getY());
      g.rotate(getRotation());

      // Calculate size and position
      float halfSize = getSize() / 2;
      int drawX = (int) (-halfSize);
      int drawY = (int) (-halfSize);
      int drawWidth = (int) (getSize());
      int drawHeight = (int) (getSize());

      // Draw sprite image
      g.drawImage(sprite, drawX, drawY, drawWidth, drawHeight, null);

      // Restore original transform and composite
      g.setTransform(originalTransform);
      g.setComposite(originalComposite);
    }
  }

  private final Random random = new Random();
  private BufferedImage[] sprites;

  // Particle properties
  private float minSize = 10.0f;
  private float maxSize = 30.0f;
  private float minSpeed = 50.0f;
  private float maxSpeed = 100.0f;
  private float minLifetime = 1.0f;
  private float maxLifetime = 3.0f;
  private float spreadAngle = (float) Math.PI * 2; // 360 degrees
  private float baseAngle = 0; // Initial emission direction
  private float gravity = 0;
  private float fadeRate = 0.95f; // Alpha reduction per second

  public SpriteParticleEmitter(float x, float y, BufferedImage[] sprites) {
    setPosition(x, y);
    this.sprites = sprites;
    if (this.sprites == null || this.sprites.length == 0) {
      throw new IllegalArgumentException("Sprite array must not be empty");
    }
  }

  @Override
  protected Particle createParticle() {
    // Skip if no sprites available
    if (sprites == null || sprites.length == 0)
      return null;

    // Pick a random sprite
    BufferedImage sprite = sprites[random.nextInt(sprites.length)];

    // Randomize lifetime
    float lifetime = minLifetime + random.nextFloat() * (maxLifetime - minLifetime);

    // Randomize size
    float size = minSize + random.nextFloat() * (maxSize - minSize);

    // Create sprite particle
    SpriteParticle p = new SpriteParticle(x, y, size, Color.WHITE, lifetime, sprite);

    // Randomize velocity based on angle and speed
    float angle = baseAngle + (random.nextFloat() - 0.5f) * spreadAngle;
    float speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed);
    p.setVelocityX((float) Math.cos(angle) * speed);
    p.setVelocityY((float) Math.sin(angle) * speed);

    // Set acceleration (gravity)
    p.setAccelerationY(gravity);

    // Randomize rotation
    p.setRotation((float) (random.nextFloat() * Math.PI * 2));
    p.setRotationSpeed((float) ((random.nextFloat() - 0.5f) * Math.PI * 0.5));

    return p;
  }

  @Override
  public void update(float deltaTime) {
    super.update(deltaTime);

    // Apply custom fading to particles
    for (Particle p : particles) {
      float currentAlpha = p.getAlpha();
      float newAlpha = Math.max(0, currentAlpha - fadeRate * deltaTime);
      p.setAlpha(newAlpha);
    }
  }

  @Override
  public void configure(Map<String, Object> params) {
    super.configure(params);

    if (params.containsKey("sprites"))
      this.sprites = (BufferedImage[]) params.get("sprites");
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
    if (params.containsKey("fadeRate"))
      this.fadeRate = ((Number) params.get("fadeRate")).floatValue();
  }
}
