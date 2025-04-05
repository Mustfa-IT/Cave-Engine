package com.engine.particles;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.engine.particles.emitters.SpriteParticleEmitter.SpriteParticle;

/**
 * Advanced batch renderer for sprite particles that uses instanced rendering.
 * Significantly improves performance when rendering many particles with the
 * same sprite.
 */
public class InstancedSpriteRenderer implements BatchRenderer {

  // Represents a group of particles sharing the same sprite and properties
  private static class ParticleInstance {
    float x, y;
    float size;
    float rotation;
    float alpha;
  }

  // Group particles by sprite, then by approximate size and alpha for maximum
  // batching
  private final Map<BufferedImage, Map<Integer, List<ParticleInstance>>> instanceGroups = new HashMap<>();
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

    // Create instance data
    ParticleInstance instance = new ParticleInstance();
    instance.x = particle.getX();
    instance.y = particle.getY();
    instance.size = particle.getSize();
    instance.rotation = particle.getRotation();
    instance.alpha = particle.getAlpha();

    // Group key combines discretized size and alpha for maximum batching
    int sizeKey = (int) (instance.size * 2); // Round to nearest 0.5
    int alphaKey = (int) (instance.alpha * 10); // Round to nearest 0.1
    int combinedKey = (sizeKey << 8) | alphaKey;

    // Add to appropriate batch
    instanceGroups
        .computeIfAbsent(sprite, k -> new HashMap<>())
        .computeIfAbsent(combinedKey, k -> new ArrayList<>())
        .add(instance);

    // Mark particle as processed
    particle.setLastFrameRendered(currentFrameNumber);
  }

  @Override
  public void render(Graphics2D g) {
    // Increment frame counter
    currentFrameNumber++;

    // Enable bilinear interpolation for better quality
    Object prevInterpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

    // Save original transform and composite
    AffineTransform originalTransform = g.getTransform();
    Composite originalComposite = g.getComposite();

    // For each sprite type
    for (Map.Entry<BufferedImage, Map<Integer, List<ParticleInstance>>> spriteEntry : instanceGroups.entrySet()) {
      BufferedImage sprite = spriteEntry.getKey();
      Map<Integer, List<ParticleInstance>> instanceLists = spriteEntry.getValue();

      // For each batch of similar particles
      for (Map.Entry<Integer, List<ParticleInstance>> batchEntry : instanceLists.entrySet()) {
        List<ParticleInstance> instances = batchEntry.getValue();
        if (instances.isEmpty())
          continue;

        // Sample the first instance for shared properties
        ParticleInstance sample = instances.get(0);

        // Set alpha composite once for the whole batch
        if (sample.alpha < 1.0f) {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, sample.alpha));
        }

        // Calculate draw dimensions based on sample size
        float size = sample.size;
        int drawWidth = (int) size;
        int drawHeight = (int) size;
        int halfWidth = drawWidth / 2;
        int halfHeight = drawHeight / 2;

        // If all have the same rotation, we can optimize even more
        boolean sameRotation = true;
        float rotation = sample.rotation;

        for (int i = 1; i < instances.size(); i++) {
          if (Math.abs(instances.get(i).rotation - rotation) > 0.01f) {
            sameRotation = false;
            break;
          }
        }

        if (sameRotation && Math.abs(rotation) < 0.01f) {
          // No rotation case - fastest path
          for (ParticleInstance inst : instances) {
            int x = (int) (inst.x - halfWidth);
            int y = (int) (inst.y - halfHeight);
            g.drawImage(sprite, x, y, drawWidth, drawHeight, null);
          }
        } else if (sameRotation) {
          // Same rotation for all - pre-rotate once
          g.rotate(rotation);

          for (ParticleInstance inst : instances) {
            // Calculate rotated position
            float cos = (float) Math.cos(-rotation);
            float sin = (float) Math.sin(-rotation);
            float rx = inst.x * cos - inst.y * sin;
            float ry = inst.x * sin + inst.y * cos;

            int x = (int) (rx - halfWidth);
            int y = (int) (ry - halfHeight);
            g.drawImage(sprite, x, y, drawWidth, drawHeight, null);
          }

          // Reset rotation
          g.setTransform(originalTransform);
        } else {
          // Different rotations - slowest path
          for (ParticleInstance inst : instances) {
            g.translate(inst.x, inst.y);
            g.rotate(inst.rotation);
            g.drawImage(sprite, -halfWidth, -halfHeight, drawWidth, drawHeight, null);
            g.setTransform(originalTransform);
          }
        }
      }
    }

    // Restore original settings
    g.setComposite(originalComposite);
    if (prevInterpolation != null) {
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prevInterpolation);
    }
  }

  @Override
  public void reset() {
    instanceGroups.clear();
  }

  @Override
  public String getType() {
    return "instanced";
  }
}
