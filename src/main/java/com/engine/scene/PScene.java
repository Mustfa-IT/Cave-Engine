package com.engine.scene;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import com.engine.components.Transform;
import com.engine.core.GameEngine;
import com.engine.entity.EntityFactory;
import com.engine.input.InputManager;
import com.engine.particles.ParticleEmitter;
import com.engine.particles.ParticleSystem;

import dev.dominion.ecs.api.Entity;

/**
 * Demo showcasing the Particle System capabilities
 */
public class PScene extends Scene {
  private float mouseX, mouseY;
  private ParticleSystem particleSystem;

  public PScene(EntityFactory entityFactory) {
    super(entityFactory);
  }

  private Entity player;
  private ParticleEmitter engineEmitter;

  @Override
  public void initialize() {
    // Set up camera
    engine.createCamera(0, 0, 2.0f);
    particleSystem = engine.getParticleSystem();
    // Create player entity
    player = engine.getEcs().createEntity();
    player.add(new Transform(0, 0, 0, 1, 1));

    // Create a continuous particle emitter attached to player
    engineEmitter = particleSystem.createFire(0, -20);
    particleSystem.attachEmitterToEntity(engineEmitter, player);

    // Add input handling for demo
    setupInput();

    // Create debug overlay
    engine.createDebugOverlay();

    // Create a static explosion at start for visual effect
    particleSystem.createExplosion(0, 0, 100);
  }

  private void setupInput() {
    InputManager input = engine.getInputManager();

    // Track mouse position for particle creation
    input.onMouseMove(p -> {
      float[] worldPos = engine.getCameraSystem().screenToWorld((float) p.getX(), (float) p.getY());
      mouseX = worldPos[0];
      mouseY = worldPos[1];
    });

    // Create different particle effects with mouse clicks
    input.addMouseListener(e -> {
      if (e.getButton() == 1) { // Left click
        particleSystem.createExplosion(mouseX, mouseY, 50);
      } else if (e.getButton() == 3) { // Right click
        ParticleEmitter smokeEmitter = particleSystem.createSmoke(mouseX, mouseY);
        // Auto-remove after 3 seconds
        new Thread(() -> {
          // try {
          //   Thread.sleep(3000);
          //   particleSystem.removeEmitter(smokeEmitter);
          // } catch (InterruptedException ex) {
          //   Thread.currentThread().interrupt();
          // }
        }).start();
      }
      return false;
    });

    // Move player with arrow keys
    input.addKeyListener(e -> {
      Transform transform = player.get(Transform.class);
      float speed = 500.0f * (float) engine.getDeltaTime();

      if (e.getKeyCode() == KeyEvent.VK_UP) {
        transform.setY(transform.getY() + speed);
      } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
        transform.setY(transform.getY() - speed);
      } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
        transform.setX(transform.getX() - speed);
      } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
        transform.setX(transform.getX() + speed);
      } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
        createPhysicalParticles();
      }

      return false;
    });

    // Different particle type hotkeys
    input.addKeyListener(e -> {
      if (e.getKeyCode() == KeyEvent.VK_1) {
        createColorParticles();
      } else if (e.getKeyCode() == KeyEvent.VK_2) {
        createSpriteParticles();
      } else if (e.getKeyCode() == KeyEvent.VK_3) {
        createPhysicalParticles();
      } else if (e.getKeyCode() == KeyEvent.VK_F1) {
        engine.toggleDebugOverlay();
      }
      return false;
    });
  }

  private void createColorParticles() {
    Map<String, Object> params = new HashMap<>();
    params.put("startColor", new Color(0, 150, 255, 200));
    params.put("minSize", 8.0f);
    params.put("maxSize", 20.0f);
    params.put("emissionRate", 30.0f);
    params.put("spreadAngle", Math.PI / 8);
    params.put("baseAngle", -Math.PI / 2); // Up

    ParticleEmitter emitter = particleSystem.createEmitter("color", mouseX, mouseY, params);
    emitter.start();
  }

  private void createSpriteParticles() {
    // Load sprite images for particles
    String[] spritePaths = {
        "sprites/particle_star.png",
        "sprites/particle_circle.png"
    };

    // Create sprite emitter with these images
    ParticleEmitter emitter = engine.getParticleSystem().getFactory()
        .createSpriteEmitter(mouseX, mouseY, spritePaths);

    // Configure and start
    Map<String, Object> params = new HashMap<>();
    params.put("minSize", 15.0f);
    params.put("maxSize", 40.0f);
    params.put("emissionRate", 10.0f);
    params.put("spreadAngle", Math.PI * 2); // 360 degrees
    emitter.configure(params);

    particleSystem.addEmitter(emitter);
    emitter.start();
  }

  private void createPhysicalParticles() {
    // Create physical particles that interact with the world
    ParticleEmitter emitter = particleSystem.createEmitter("physical", mouseX, mouseY, null);

    // Configure physical properties
    Map<String, Object> params = new HashMap<>();
    params.put("density", 0.8f);
    params.put("restitution", 0.7f); // Bouncy
    params.put("minRadius", 3.0f);
    params.put("maxRadius", 10.0f);
    params.put("minLifetime", 5.0f);
    params.put("maxLifetime", 10.0f);
    emitter.configure(params);

    // Emit a burst of physical particles
    emitter.burst(20);
  }

  @Override
  public void update(double deltaTime) {
    // Scene update logic
  }

}
