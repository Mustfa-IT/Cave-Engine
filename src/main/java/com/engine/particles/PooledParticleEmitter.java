package com.engine.particles;

/**
 * Interface for particle emitters that can use a shared particle pool.
 */
public interface PooledParticleEmitter {

  /**
   * Set the particle pool this emitter should use
   *
   * @param pool The particle pool
   */
  void setParticlePool(ParticlePool pool);
}
