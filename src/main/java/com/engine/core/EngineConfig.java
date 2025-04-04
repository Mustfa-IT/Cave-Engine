package com.engine.core;

public class EngineConfig {
  private int velocityIterations = 10; // Default value
  private int positionIterations = 8; // Default value
  private float physicsTimeStep = 1.0f / 60.0f; // Default value
  private boolean enableBodySleeping = true; // Default value
  private boolean optimizeBroadphase = false; // Default value

  /**
   * Default constructor
   */
  public EngineConfig() {
    // Initialize with default values (already set in field declarations)
  }

  /**
   * Set physics solver iterations for performance tuning
   * Lower values = better performance but less accurate physics
   *
   * @param velocityIterations Velocity solver iterations
   * @param positionIterations Position solver iterations
   * @return This config instance for method chaining
   */
  public EngineConfig physicsIterations(int velocityIterations, int positionIterations) {
    this.velocityIterations = velocityIterations;
    this.positionIterations = positionIterations;
    return this;
  }

  /**
   * Set physics simulation time step
   * Larger values = better performance but less smooth/accurate physics
   *
   * @param timeStep The fixed time step for physics simulation
   * @return This config instance for method chaining
   */
  public EngineConfig physicsTimeStep(float timeStep) {
    this.physicsTimeStep = timeStep;
    return this;
  }

  /**
   * Enable or disable automatic sleeping of inactive bodies
   * Sleeping bodies are temporarily removed from simulation calculations
   *
   * @param enable True to enable sleeping, false to disable
   * @return This config instance for method chaining
   */
  public EngineConfig enableBodySleeping(boolean enable) {
    this.enableBodySleeping = enable;
    return this;
  }

  /**
   * Enable broadphase optimization to reduce collision checks
   *
   * @param enable True to enable optimized broadphase, false to use default
   * @return This config instance for method chaining
   */
  public EngineConfig broadphaseOptimization(boolean enable) {
    this.optimizeBroadphase = enable;
    return this;
  }

  /**
   * Get the configured velocity iterations for physics solving
   *
   * @return Number of velocity iterations
   */
  public int getVelocityIterations() {
    return velocityIterations;
  }

  /**
   * Get the configured position iterations for physics solving
   *
   * @return Number of position iterations
   */
  public int getPositionIterations() {
    return positionIterations;
  }

  /**
   * Get the configured physics time step
   *
   * @return The physics time step in seconds
   */
  public float getPhysicsTimeStep() {
    return physicsTimeStep;
  }

  /**
   * Check if automatic body sleeping is enabled
   *
   * @return True if body sleeping is enabled
   */
  public boolean isEnableBodySleeping() {
    return enableBodySleeping;
  }

  /**
   * Check if broadphase optimization is enabled
   *
   * @return True if broadphase optimization is enabled
   */
  public boolean isOptimizeBroadphase() {
    return optimizeBroadphase;
  }

  // ...existing code...
}
