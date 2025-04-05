package com.engine.graph;

import java.awt.Graphics2D;

/**
 * Interface for components that need to render custom graphics directly via the
 * RenderSystem.
 * This interface is implemented by systems that need to add additional
 * rendering
 * capabilities that aren't tied to specific entities.
 */
public interface CustomRenderer {

  /**
   * Render this component
   *
   * @param g The graphics context to render with
   */
  void render(Graphics2D g);

  /**
   * Get the rendering priority of this renderer.
   * Higher values mean the renderer is drawn later (on top).
   *
   * @return The priority value
   */
  int getPriority();
}
