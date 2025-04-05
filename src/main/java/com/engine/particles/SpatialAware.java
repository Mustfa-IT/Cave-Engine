package com.engine.particles;

import com.engine.util.ParticleQuadTree;
import com.engine.util.SpatialHashGrid;

/**
 * Interface for emitters that can efficiently register themselves in a spatial
 * data structure.
 */
public interface SpatialAware {

  /**
   * Register all active particles from this emitter in a spatial hash grid
   *
   * @param grid The spatial grid to register with
   */
  void registerInSpatialStructure(SpatialHashGrid grid);

  /**
   * Register all active particles from this emitter in a quad tree
   *
   * @param quadTree The quad tree to register with
   */
  void registerInQuadTree(ParticleQuadTree quadTree);
}
