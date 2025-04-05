package com.engine.particles;

import java.awt.Graphics2D;

/**
 * Interface for renderers that can batch-render multiple particles efficiently.
 */
public interface BatchRenderer {

  /**
   * Add a particle to the batch for rendering
   *
   * @param particle The particle to add
   */
  void addParticle(Particle particle);

  /**
   * Render all batched particles at once
   *
   * @param g Graphics context to render with
   */
  void render(Graphics2D g);

  /**
   * Reset the batch renderer for a new frame
   */
  void reset();

  /**
   * Get the renderer's unique type identifier
   *
   * @return String identifier for this renderer type
   */
  String getType();
}
