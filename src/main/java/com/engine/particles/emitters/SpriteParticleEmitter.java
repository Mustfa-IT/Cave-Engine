package com.engine.particles.emitters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.engine.particles.AbstractParticleEmitter;
import com.engine.particles.BatchRenderer;
import com.engine.particles.BatchableEmitter;
import com.engine.particles.Particle;
import com.engine.particles.ParticlePool;
import com.engine.particles.PooledParticleEmitter;
import com.engine.util.SpatialHashGrid;
import com.engine.util.ParticleQuadTree;
import com.engine.particles.SpatialAware;

/**
 * Emitter that produces particles using sprite images.
 */
public class SpriteParticleEmitter extends AbstractParticleEmitter
    implements BatchableEmitter, PooledParticleEmitter, SpatialAware {

  public static class SpriteParticle extends Particle {
    private BufferedImage sprite;

    public SpriteParticle(float x, float y, float size, Color color, float lifetime, BufferedImage sprite) {
      super(x, y, size, color, lifetime);
      this.sprite = sprite;
    }

    @Override
    public void reset(float x, float y, float size, Color color, float lifetime) {
      super.reset(x, y, size, color, lifetime);
      // Sprite reference will be set separately
    }

    @Override
    public void prepareForPool() {
      super.prepareForPool();
      // Remove reference to sprite
      this.sprite = null;
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

    public BufferedImage getSprite() {
      return sprite;
    }

    public void setSprite(BufferedImage sprite) {
      this.sprite = sprite;
    }
  }

  private final Random random = new Random();
  private BufferedImage[] sprites;
  private ParticlePool particlePool;

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

  // Optimization settings
  private boolean useInstancing = true;

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
    SpriteParticle p;

    // Use particle pool if available
    if (particlePool != null) {
      Particle baseParticle = particlePool.obtainParticle(x, y, size, Color.WHITE, lifetime);

      // Convert to sprite particle if it's not already one
      if (baseParticle instanceof SpriteParticle) {
        p = (SpriteParticle) baseParticle;
        p.setSprite(sprite);
      } else {
        // If the pool gave us a base particle, we need to create a sprite particle
        p = new SpriteParticle(x, y, size, Color.WHITE, lifetime, sprite);
      }
    } else {
      // Create new if no pool
      p = new SpriteParticle(x, y, size, Color.WHITE, lifetime, sprite);
    }

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

    // Return inactive particles to pool
    if (particlePool != null) {
      particles.removeIf(p -> {
        if (!p.isActive()) {
          particlePool.recycleParticle(p);
          return true;
        }
        return false;
      });
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
    if (params.containsKey("useInstancing"))
      this.useInstancing = (Boolean) params.get("useInstancing");
  }

  @Override
  public void setParticlePool(ParticlePool pool) {
    this.particlePool = pool;
  }

  @Override
  public String getBatchType() {
    return useInstancing ? "instanced" : "sprite";
  }

  @Override
  public void addToBatch(BatchRenderer renderer) {
    for (Particle p : particles) {
      if (p.isActive() && p instanceof SpriteParticle) {
        renderer.addParticle(p);
      }
    }
  }

  @Override
  public void registerInSpatialStructure(SpatialHashGrid grid) {
    for (Particle p : particles) {
      if (p.isActive()) {
        float size = p.getSize();
        grid.insertObject(p, p.getX() - size / 2, p.getY() - size / 2, size, size);
      }
    }
  }

  @Override
  public void registerInQuadTree(ParticleQuadTree quadTree) {
    // Create a defensive copy to avoid concurrent modification
    List<Particle> particlesCopy;
    try {
      synchronized (particles) {
        particlesCopy = new ArrayList<>(particles);
      }

      // Only pass active particles
      List<Particle> activeParticles = particlesCopy.stream()
          .filter(p -> p != null && p.isActive())
          .collect(Collectors.toList());

      // Use the efficient batch insert for better performance
      quadTree.insertParticles(activeParticles);
    } catch (Exception e) {
      // Catch exceptions to prevent the entire particle system from crashing
      System.err.println("Error in SpriteParticleEmitter.registerInQuadTree: " + e.getMessage());
    }
  }
}
