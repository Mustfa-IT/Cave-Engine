package com.engine.core;

import org.jbox2d.common.Vec2;

/**
 * Configuration class for fluent engine setup
 */
public class EngineConfig {
  private int velocityIterations = 10; // Default value
  private int positionIterations = 8; // Default value
  private float physicsTimeStep = 1.0f / 60.0f; // Default value
  private boolean enableBodySleeping = true; // Default value
  private boolean optimizeBroadphase = false; // Default value
  private int targetFps = 60;
  private boolean showPerformanceStats = false;
  private boolean debugPhysics = false;
  private boolean debugColliders = false;
  private boolean debugEvents = false;
  private boolean showGrid = false;
  private Vec2 gravity = new Vec2(0f, -9.8f);
  private String windowTitle = "Cave Engine";

  /**
   * Default constructor
   */
  public EngineConfig() {
  }

  /**
   * Wither or not to show logs for the Event System
   *
   * @param debugEvents  True to enable logging for the Events, false to use default
   * @return This config instance for method chaining
   *
   */
  public void setDebugEvents(boolean debugEvents) {
    this.debugEvents = debugEvents;
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

  public EngineConfig targetFps(int fps) {
    this.targetFps = fps;
    return this;
  }

  public EngineConfig showPerformanceStats(boolean show) {
    this.showPerformanceStats = show;
    return this;
  }

  public EngineConfig debugMode(boolean debugPhysics, boolean debugColliders, boolean showGrid) {
    this.debugPhysics = debugPhysics;
    this.debugColliders = debugColliders;
    this.showGrid = showGrid;
    return this;
  }

  public EngineConfig gravity(float x, float y) {
    this.gravity = new Vec2(x, y);
    return this;
  }

  public EngineConfig windowTitle(String title) {
    this.windowTitle = title;
    return this;
  }

  public int getTargetFps() {
    return targetFps;
  }

  public boolean isShowPerformanceStats() {
    return showPerformanceStats;
  }

  public boolean isDebugPhysics() {
    return debugPhysics;
  }

  public boolean isDebugColliders() {
    return debugColliders;
  }

  public boolean isShowGrid() {
    return showGrid;
  }

  public Vec2 getGravity() {
    return gravity;
  }

  public String getWindowTitle() {
    return windowTitle;
  }

  public boolean debugEvents() {
    return debugEvents;
  }
}
