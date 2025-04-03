package com.engine.core;

import org.jbox2d.common.Vec2;

import com.engine.entity.EntityFactory;
import com.engine.graph.RenderSystem;
import com.engine.physics.PhysicsWorld;
import com.engine.scene.Scene;
import com.engine.scene.SceneManager;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Scheduler;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameEngine {
  // Logger for better debugging
  private static final Logger LOGGER = Logger.getLogger(GameEngine.class.getName());

  // Engine states
  public enum State {
    INITIALIZED, RUNNING, PAUSED, STOPPED
  }

  private GameWindow window;
  private State engineState = State.INITIALIZED;
  public Dominion ecs;
  private RenderSystem renderer;
  private CameraSystem cameraSystem;
  private PhysicsWorld physicsWorld;
  private Vec2 defaultGravity = new Vec2(0, 9.8f);
  private boolean closedByWindow;
  private Scheduler scheduler;
  private int targetFps = 60;

  // Performance monitoring
  private long frameCount = 0;
  private long lastFpsReportTime = 0;
  private double averageFps = 0;
  private boolean showPerformanceStats = false;

  // Physics debug options
  private boolean physicsDebug = false;

  // Configuration
  private Properties config = new Properties();

  // Integrated components
  private EntityFactory entityFactory;
  private SceneManager sceneManager;

  public GameEngine() {
    loadConfig();
    int w = Integer.parseInt(config.getProperty("window.width", "1200"));
    int h = Integer.parseInt(config.getProperty("window.height", "800"));
    this.window = new GameWindow(config.getProperty("window.title", "Physics Game"), w, h);
    this.window.setOnClose(() -> {
      stop();
      this.closedByWindow = true;
      return null;
    });
    this.addShutdownHook(new Thread(this::stop));
    init();
    LOGGER.info("GameEngine initialized. Dominion world created.");
  }

  /**
   * Loads configuration from config.properties if available
   */
  private void loadConfig() {
    try {
      FileInputStream in = new FileInputStream("config.properties");
      config.load(in);
      in.close();

      // Apply configuration
      targetFps = Integer.parseInt(config.getProperty("engine.targetFps", "60"));
      defaultGravity = new Vec2(
          Float.parseFloat(config.getProperty("physics.gravityX", "0")),
          Float.parseFloat(config.getProperty("physics.gravityY", "9.8")));
      showPerformanceStats = Boolean.parseBoolean(config.getProperty("engine.showPerformanceStats", "false"));

      LOGGER.info("Configuration loaded successfully");
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Could not load config.properties. Using defaults.", e);
    }
  }

  private void init() {
    try {
      // Core systems initialization
      this.ecs = Dominion.create();
      this.physicsWorld = new PhysicsWorld(defaultGravity, ecs);
      this.cameraSystem = new CameraSystem(ecs);
      this.cameraSystem.createCamera(window.getWidth(), window.getHeight(), 50, 510);
      this.renderer = new RenderSystem(window, ecs, cameraSystem);

      // Create entity factory
      this.entityFactory = new EntityFactory(ecs, physicsWorld);

      // Create scene manager with reference to this engine
      this.sceneManager = new SceneManager(this);

      // Setup the main loop scheduler
      this.scheduler = ecs.createScheduler();
      this.scheduler.schedule(this::update);
      this.scheduler.schedule(() -> renderer.render());
      this.scheduler.schedule(() -> cameraSystem.update((float) this.scheduler.deltaTime()));

      // Initialize FPS counter
      lastFpsReportTime = System.currentTimeMillis();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize game engine", e);
      throw new RuntimeException("Engine initialization failed", e);
    }
  }

  /**
   * Main engine update method, calls physics and scene updates
   */
  private void update() {
    try {
      // Skip updates if paused
      if (engineState == State.PAUSED) {
        return;
      }

      double deltaTime = this.scheduler.deltaTime();

      // Update physics
      this.physicsWorld.update(deltaTime);

      // Update current scene
      if (sceneManager.getCurrentScene() != null) {
        sceneManager.update(deltaTime);
      }

      // Performance monitoring
      frameCount++;
      long now = System.currentTimeMillis();
      if (now - lastFpsReportTime >= 1000) {
        averageFps = frameCount * 1000.0 / (now - lastFpsReportTime);
        frameCount = 0;
        lastFpsReportTime = now;

        if (showPerformanceStats) {
          LOGGER.info(String.format("FPS: %.2f", averageFps));
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error in game update loop", e);
    }
  }

  /**
   * Register a scene with the engine
   *
   * @param name  Scene identifier
   * @param scene Scene instance
   * @return this GameEngine instance for method chaining
   * @throws IllegalArgumentException if the scene name is already registered
   */
  public GameEngine registerScene(String name, Scene scene) {
    try {
      sceneManager.registerScene(name, scene);
      return this;
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to register scene: " + name, e);
      throw e;
    }
  }

  /**
   * Set the active scene by name
   *
   * @param sceneName Name of scene to activate
   * @return this GameEngine instance for method chaining
   * @throws IllegalArgumentException if scene doesn't exist
   */
  public GameEngine setActiveScene(String sceneName) {
    try {
      sceneManager.setActiveScene(sceneName);
      return this;
    } catch (IllegalArgumentException e) {
      LOGGER.log(Level.SEVERE, "Failed to set active scene: " + sceneName, e);
      throw e;
    }
  }

  /**
   * Set the target frames per second
   *
   * @param fps Target FPS
   * @return this GameEngine instance for method chaining
   */
  public GameEngine setTargetFps(int fps) {
    if (fps <= 0) {
      LOGGER.warning("Invalid FPS value: " + fps + ". Using default: 60");
      fps = 60;
    }
    this.targetFps = fps;
    LOGGER.info("Target FPS set to " + fps);

    // Update scheduler rate if engine is already running
    if (engineState == State.RUNNING) {
      scheduler.tickAtFixedRate(targetFps);
    }

    return this;
  }

  /**
   * Start the game engine
   *
   * @return this GameEngine instance for method chaining
   */
  public GameEngine start() {
    if (engineState == State.RUNNING)
      return this;
    engineState = State.RUNNING;
    this.window.initialize();
    LOGGER.info("Starting the Game Engine with target FPS: " + targetFps);
    scheduler.tickAtFixedRate(targetFps); // Start scheduler at target FPS
    return this;
  }

  public void stop() {
    if (closedByWindow)
      return;
    engineState = State.STOPPED;
    LOGGER.info("Stopping the Game Engine...");
    scheduler.shutDown(); // Stop the scheduler
  }

  public void addShutdownHook(Thread hook) {
    Runtime.getRuntime().addShutdownHook(hook);
  }

  public GameWindow getWindow() {
    return window;
  }

  public void setWindow(GameWindow window) {
    this.window = window;
  }

  public boolean isRunning() {
    return engineState == State.RUNNING;
  }

  public void setRunning(boolean running) {
    this.engineState = running ? State.RUNNING : State.STOPPED;
  }

  public Dominion getEcs() {
    return ecs;
  }

  public void setEcs(Dominion world) {
    this.ecs = world;
  }

  public RenderSystem getRenderer() {
    return renderer;
  }

  public void setRenderer(RenderSystem renderer) {
    this.renderer = renderer;
  }

  public EntityFactory getEntityFactory() {
    return entityFactory;
  }

  public SceneManager getSceneManager() {
    return sceneManager;
  }

  public PhysicsWorld getPhysicsWorld() {
    return physicsWorld;
  }

  public CameraSystem getCameraSystem() {
    return cameraSystem;
  }
}
