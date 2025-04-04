package com.engine.graph;

/**
 * Interface for rendering system - allows for different implementations
 * and easier testing through dependency injection
 */
public interface RenderingSystem {
  /**
   * Render all visible entities
   */
  void render();
}
