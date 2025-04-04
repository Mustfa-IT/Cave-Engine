package com.engine.graph;

import java.awt.Graphics2D;

/**
 * Interface for components that render overlays on top of the main game view.
 * Used to break dependency cycles between rendering system and game engine.
 */
public interface OverlayRenderer {
  /**
   * Render overlays like console, debug info, etc.
   * 
   * @param g Graphics context to render with
   */
  void renderOverlays(Graphics2D g);
}
