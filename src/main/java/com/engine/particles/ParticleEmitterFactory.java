package com.engine.particles;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jbox2d.dynamics.World;

import com.engine.assets.AssetManager;
import com.engine.particles.emitters.ColorParticleEmitter;
import com.engine.particles.emitters.PhysicalParticleEmitter;
import com.engine.particles.emitters.SpriteParticleEmitter;
import com.engine.physics.PhysicsSystem;

/**
 * Factory for creating particle emitters of different types.
 */
@Singleton
public class ParticleEmitterFactory {

  private final PhysicsSystem physicsSystem;
  private final AssetManager assetManager;
  private final World physicsWorld;

  private final Map<String, Supplier<ParticleEmitter>> emitterFactories = new HashMap<>();

  @Inject
  public ParticleEmitterFactory(PhysicsSystem physicsSystem, AssetManager assetManager, World physicsWorld) {
    this.physicsSystem = physicsSystem;
    this.assetManager = assetManager;
    this.physicsWorld = physicsWorld;

    // Register built-in emitter types
    registerDefaultEmitters();
  }

  /**
   * Create a particle emitter of the specified type
   *
   * @param type   The type of emitter to create
   * @param x      X position
   * @param y      Y position
   * @param params Configuration parameters
   * @return The created emitter
   */
  public ParticleEmitter createEmitter(String type, float x, float y, Map<String, Object> params) {
    // Default to color emitter if type not found
    Supplier<ParticleEmitter> factory = emitterFactories.getOrDefault(type, () -> {
      return new ColorParticleEmitter(x, y);
    });

    ParticleEmitter emitter = factory.get();
    emitter.setPosition(x, y);

    // Apply configuration if provided
    if (params != null) {
      emitter.configure(params);
    }

    return emitter;
  }

  /**
   * Create a color particle emitter
   *
   * @param x X position
   * @param y Y position
   * @return The created emitter
   */
  public ColorParticleEmitter createColorEmitter(float x, float y) {
    return new ColorParticleEmitter(x, y);
  }

  /**
   * Create a sprite particle emitter
   *
   * @param x       X position
   * @param y       Y position
   * @param sprites Array of sprite images to use
   * @return The created emitter
   */
  public SpriteParticleEmitter createSpriteEmitter(float x, float y, BufferedImage[] sprites) {
    return new SpriteParticleEmitter(x, y, sprites);
  }

  /**
   * Create a sprite emitter with sprites loaded from paths
   *
   * @param x           X position
   * @param y           Y position
   * @param spritePaths Array of sprite paths to load from asset manager
   * @return The created emitter
   */
  public SpriteParticleEmitter createSpriteEmitter(float x, float y, String[] spritePaths) {
    BufferedImage[] sprites = new BufferedImage[spritePaths.length];
    for (int i = 0; i < spritePaths.length; i++) {
      sprites[i] = assetManager.loadImage("partical_" + i, spritePaths[i]);
    }
    return createSpriteEmitter(x, y, sprites);
  }

  /**
   * Create a physics-based particle emitter
   *
   * @param x X position
   * @param y Y position
   * @return The created emitter
   */
  public PhysicalParticleEmitter createPhysicalEmitter(float x, float y) {
    return new PhysicalParticleEmitter(x, y, physicsSystem, physicsWorld);
  }

  /**
   * Register a custom emitter type
   *
   * @param typeName Type identifier for the emitter
   * @param factory  Supplier that creates an instance of the emitter
   */
  public void registerEmitterType(String typeName, Supplier<ParticleEmitter> factory) {
    emitterFactories.put(typeName, factory);
  }

  /**
   * Register the built-in emitter types
   */
  private void registerDefaultEmitters() {
    // Register color particle emitter
    registerEmitterType("color", () -> new ColorParticleEmitter(0, 0));

    // Register sprite particle emitter (requires initializing with sprites later)
    registerEmitterType("sprite", () -> {
      BufferedImage[] defaultSprites = new BufferedImage[1];
      defaultSprites[0] = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
      return new SpriteParticleEmitter(0, 0, defaultSprites);
    });

    // Register physical particle emitter
    registerEmitterType("physical", () -> new PhysicalParticleEmitter(0, 0, physicsSystem, physicsWorld));
  }

  /**
   * Create a fire effect emitter
   *
   * @param x X position
   * @param y Y position
   * @return Configured fire particle emitter
   */
  public ParticleEmitter createFireEmitter(float x, float y) {
    ColorParticleEmitter emitter = createColorEmitter(x, y);

    Map<String, Object> config = new HashMap<>();
    config.put("startColor", new Color(255, 100, 0, 200));
    config.put("endColor", new Color(255, 0, 0, 0));
    config.put("minSize", 4.0f);
    config.put("maxSize", 25.0f);
    config.put("minSpeed", 20.0f);
    config.put("maxSpeed", 60.0f);
    config.put("minLifetime", 0f);
    config.put("maxLifetime", 1.5f);
    config.put("spreadAngle", Math.PI / 4);
    config.put("baseAngle", Math.PI / 2); // Up
    config.put("emissionRate", 20.0f);
    config.put("gravity", 5.0f); // Particles rise

    emitter.configure(config);
    return emitter;
  }

  /**
   * Create a smoke effect emitter
   *
   * @param x X position
   * @param y Y position
   * @return Configured smoke particle emitter
   */
  public ParticleEmitter createSmokeEmitter(float x, float y) {
    ColorParticleEmitter emitter = createColorEmitter(x, y);

    Map<String, Object> config = new HashMap<>();
    config.put("startColor", new Color(100, 100, 100, 100));
    config.put("endColor", new Color(200, 200, 200, 0));
    config.put("minSize", 15.0f);
    config.put("maxSize", 30.0f);
    config.put("minSpeed", 10.0f);
    config.put("maxSpeed", 30.0f);
    config.put("minLifetime", 0.0f);
    config.put("maxLifetime", 4.0f);
    config.put("spreadAngle", Math.PI / 3);
    config.put("baseAngle", Math.PI / 2); // Up
    config.put("emissionRate", 5.0f);
    config.put("gravity", 10.0f); // Particles rise slowly

    emitter.configure(config);
    return emitter;
  }

  /**
   * Create an explosion effect emitter (burst)
   *
   * @param x X position
   * @param y Y position
   * @return Configured explosion particle emitter
   */
  public ParticleEmitter createExplosionEmitter(float x, float y) {
    ColorParticleEmitter emitter = createColorEmitter(x, y);

    Map<String, Object> config = new HashMap<>();
    config.put("startColor", new Color(255, 200, 0, 255));
    config.put("endColor", new Color(255, 0, 0, 0));
    config.put("minSize", 5.0f);
    config.put("maxSize", 10.0f);
    config.put("minSpeed", 50.0f);
    config.put("maxSpeed", 100.0f);
    config.put("minLifetime", 0.5f);
    config.put("maxLifetime", 1.0f);
    config.put("spreadAngle", Math.PI * 2); // 360 degrees
    config.put("emissionRate", 0.0f); // No continuous emission

    emitter.configure(config);
    // Single burst
    emitter.burst(50);

    return emitter;
  }
}
