package com.engine.particles;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.engine.particles.emitters.SpriteParticleEmitter.SpriteParticle;

/**
 * Batch renderer for sprite particles that groups rendering by sprite image.
 */
public class SpriteParticleBatchRenderer implements BatchRenderer {

  // Group particles by sprite and alpha for efficient rendering
  private final Map<BufferedImage, Map<Float, List<SpriteParticle>>> particlesByImageAndAlpha = new HashMap<>();
  private long currentFrameNumber = 0;

  @Override
  public void addParticle(Particle particle) {
    if (!particle.isActive() || !(particle instanceof SpriteParticle)) {
      return;
    }

    SpriteParticle spriteParticle = (SpriteParticle) particle;
    BufferedImage sprite = spriteParticle.getSprite();
    if (sprite == null) {
      return;
    }

    // Round alpha to nearest 0.05 to reduce state changes
    float roundedAlpha = Math.round(spriteParticle.getAlpha() * 20) / 20.0f;

    // Add to batch organized by image and alpha
    Map<Float, List<SpriteParticle>> alphaGroups = particlesByImageAndAlpha.computeIfAbsent(sprite,
        k -> new HashMap<>());
    alphaGroups.computeIfAbsent(roundedAlpha, k -> new ArrayList<>()).add(spriteParticle);

    // Mark as processed
    spriteParticle.setLastFrameRendered(currentFrameNumber);
  }

  @Override
  public void render(Graphics2D g) {
    // Increment frame counter
    currentFrameNumber++;

    // Save original transform and composite
    AffineTransform originalTransform = g.getTransform();
    Composite originalComposite = g.getComposite();

    // First render by sprite (to minimize texture binds)
    for (Map.Entry<BufferedImage, Map<Float, List<SpriteParticle>>> spriteEntry : particlesByImageAndAlpha.entrySet()) {

      BufferedImage sprite = spriteEntry.getKey();
      Map<Float, List<SpriteParticle>> alphaGroups = spriteEntry.getValue();

      // Then render by alpha (to minimize composite changes)
      for (Map.Entry<Float, List<SpriteParticle>> alphaEntry : alphaGroups.entrySet()) {
        float alpha = alphaEntry.getKey();
        List<SpriteParticle> particles = alphaEntry.getValue();

        // Set alpha composite once for the whole group
        if (alpha < 1.0f) {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        // Render all particles with this sprite and alpha
        for (SpriteParticle p : particles) {
          float size = p.getSize();

          // Apply transform for this particle
          g.translate(p.getX(), p.getY());
          g.rotate(p.getRotation());

          // Draw sprite
          int drawX = (int) (-size / 2);
          int drawY = (int) (-size / 2);
          int drawWidth = (int) size;
          int drawHeight = (int) size;

          g.drawImage(sprite, drawX, drawY, drawWidth, drawHeight, null);

          // Reset transform for next particle
          g.setTransform(originalTransform);
        }
      }
    }

    // Restore original composite
    g.setComposite(originalComposite);
  }

  @Override
  public void reset() {
    particlesByImageAndAlpha.clear();
  }

  @Override
  public String getType() {
    return "sprite";
  }
}
