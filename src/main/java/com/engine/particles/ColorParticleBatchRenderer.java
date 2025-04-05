package com.engine.particles;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch renderer for color particles that reduces draw calls.
 */
public class ColorParticleBatchRenderer implements BatchRenderer {

  // Group particles by alpha for efficient rendering
  private final Map<Float, List<Particle>> particlesByAlpha = new HashMap<>();
  private long currentFrameNumber = 0;

  @Override
  public void addParticle(Particle particle) {
    if (!particle.isActive()) {
      return;
    }

    // Round alpha to nearest 0.05 to reduce state changes
    float roundedAlpha = Math.round(particle.getAlpha() * 20) / 20.0f;

    particlesByAlpha.computeIfAbsent(roundedAlpha, k -> new ArrayList<>()).add(particle);

    // Mark the particle as processed for this frame
    particle.setLastFrameRendered(currentFrameNumber);
  }

  @Override
  public void render(Graphics2D g) {
    // Increment frame counter for tracking rendered particles
    currentFrameNumber++;

    // Save original transform and composite
    AffineTransform originalTransform = g.getTransform();
    Composite originalComposite = g.getComposite();

    // Render each alpha group
    for (Map.Entry<Float, List<Particle>> entry : particlesByAlpha.entrySet()) {
      float alpha = entry.getKey();
      List<Particle> particles = entry.getValue();

      // Set appropriate alpha composite once for the whole batch
      if (alpha < 1.0f) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
      }

      // Render all particles with this alpha
      for (Particle p : particles) {
        Color color = p.getColor();
        float size = p.getSize();
        float halfSize = size / 2;

        g.setColor(color);

        // Apply transform for this particle
        g.translate(p.getX(), p.getY());
        g.rotate(p.getRotation());

        // Draw the particle
        g.fillRect((int) (-halfSize), (int) (-halfSize), (int) size, (int) size);

        // Reset transform for next particle
        g.setTransform(originalTransform);
      }
    }

    // Restore original composite
    g.setComposite(originalComposite);
  }

  @Override
  public void reset() {
    particlesByAlpha.clear();
  }

  @Override
  public String getType() {
    return "color";
  }
}
