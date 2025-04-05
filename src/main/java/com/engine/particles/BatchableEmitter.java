package com.engine.particles;

/**
 * Interface for particle emitters that support batch rendering.
 */
public interface BatchableEmitter {

  /**
   * Get the batch renderer type this emitter is compatible with
   *
   * @return The batch renderer type identifier
   */
  String getBatchType();

  /**
   * Add this emitter's particles to a batch renderer
   *
   * @param renderer The batch renderer to add particles to
   */
  void addToBatch(BatchRenderer renderer);
}
