package com.engine.core;

import com.engine.components.CameraComponent;
import com.engine.components.UIComponent;
import com.engine.entity.EntityFactory;
import com.engine.graph.RenderSystem;
import com.engine.input.InputManager;
import com.engine.physics.PhysicsWorld;
import com.engine.scene.Scene;
import com.engine.scene.SceneManager;
import com.engine.ui.UISystem;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Scheduler;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jbox2d.common.Vec2;

@Singleton
public class GameEngine {
  // Logger for better debugging
  private static final Logger LOGGER = Logger.getLogger(GameEngine.class.getName());

  // Engine states
  public enum State {
    INITIALIZED, RUNNING, PAUSED, STOPPED
  }

  // Debug flags
  private boolean debugPhysics = false;
  private boolean debugColliders = false;
  private boolean debugGrid = true;

  private final GameWindow window;
  private State engineState = State.INITIALIZED;
  private final Dominion ecs;
  private final RenderSystem renderer;
  private final CameraSystem cameraSystem;
  private final PhysicsWorld physicsWorld;
  private boolean closedByWindow;
  private Scheduler scheduler;
  private int targetFps = 60;

  // Performance monitoring
  private long frameCount = 0;
  private long lastFpsReportTime = 0;
  private double averageFps = 0;
  private boolean showPerformanceStats = false;

  // Integrated components
  private final EntityFactory entityFactory;
  private SceneManager sceneManager;
  private final UISystem uiSystem;
  private final InputManager inputManager;
  private final Properties config;

  @Inject
  public GameEngine(GameWindow window, Dominion ecs, RenderSystem renderer,
      CameraSystem cameraSystem, PhysicsWorld physicsWorld,
      EntityFactory entityFactory, UISystem uiSystem,
      InputManager inputManager, Properties config) {
    this.window = window;
    this.ecs = ecs;
    this.renderer = renderer;
    this.cameraSystem = cameraSystem;
    this.physicsWorld = physicsWorld;
    this.entityFactory = entityFactory;
    this.uiSystem = uiSystem;
    this.inputManager = inputManager;
    this.config = config;

    // Apply configuration
    this.targetFps = Integer.parseInt(config.getProperty("engine.targetFps", "60"));
    this.showPerformanceStats = Boolean.parseBoolean(config.getProperty("engine.showPerformanceStats", "false"));

    this.window.setOnClose(() -> {
      stop();
      this.closedByWindow = true;
      return null;
    });

    addShutdownHook(new Thread(this::stop));
    init();
    LOGGER.info("GameEngine initialized. Dominion world created.");
  }

  private void init() {
    try {
      // Set up the window resize handler
      this.window.setOnResize((width, height) -> {
        cameraSystem.updateAllViewports(width, height);
      });

      // Create camera at world origin (0,0)
      this.cameraSystem.createCamera(window.getWidth(), window.getHeight(), 0, 0);
      this.cameraSystem.updateAllViewports(window.getWidth(), window.getHeight());
      // Create scene manager with reference to engine, entity factory, and UI system
      this.sceneManager = new SceneManager(this, entityFactory, uiSystem);

      // Setup the main loop scheduler
      this.scheduler = ecs.createScheduler();
      this.scheduler.schedule(this::update);
      this.scheduler.schedule(() -> renderer.render());
      this.scheduler.schedule(() -> cameraSystem.update((float) this.scheduler.deltaTime()));
      this.scheduler.schedule(() -> uiSystem.update(this.scheduler.deltaTime()));

      // Initialize FPS counter
      lastFpsReportTime = System.currentTimeMillis();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize game engine", e);
      throw new RuntimeException("Engine initialization failed", e);
    }
  }

  /**
   * Updates physics bodies for all entities that have physics components
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
   * Creates and registers a scene with the engine using a supplier
   *
   * @param name          Scene identifier
   * @param sceneSupplier Supplier that creates the scene
   * @return this GameEngine instance for method chaining
   */
  public GameEngine createScene(String name, Supplier<Scene> sceneSupplier) {
    Scene scene = sceneSupplier.get();
    registerScene(name, scene);
    return this;
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

  public boolean isRunning() {
    return engineState == State.RUNNING;
  }

  public void setRunning(boolean running) {
    this.engineState = running ? State.RUNNING : State.STOPPED;
  }

  public Dominion getEcs() {
    return ecs;
  }

  public RenderSystem getRenderer() {
    return renderer;
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

  public UISystem getUiSystem() {
    return uiSystem;
  }

  /**
   * Get the input manager for handling keyboard and mouse input
   * 
   * @return InputManager instance
   */
  public InputManager getInputManager() {
    return inputManager;
  }

  /**
   * Configure the engine with custom settings
   *
   * @param configurator A consumer that can modify engine settings
   * @return this GameEngine instance for method chaining
   */
  public GameEngine configure(Consumer<EngineConfig> configurator) {
    EngineConfig config = new EngineConfig(this);
    configurator.accept(config);
    return this;
  }

  /**
   * Pause the game engine
   *
   * @return this GameEngine instance for method chaining
   */
  public GameEngine pause() {
    if (engineState == State.RUNNING) {
      engineState = State.PAUSED;
      LOGGER.info("Game engine paused");
    }
    return this;
  }

  /**
   * Resume the game engine from a paused state
   *
   * @return this GameEngine instance for method chaining
   */
  public GameEngine resume() {
    if (engineState == State.PAUSED) {
      engineState = State.RUNNING;
      LOGGER.info("Game engine resumed");
    }
    return this;
  }

  /**
   * Toggle the pause state of the engine
   *
   * @return this GameEngine instance for method chaining
   */
  public GameEngine togglePause() {
    return engineState == State.PAUSED ? resume() : pause();
  }

  /**
   * Set debug display options
   *
   * @param showPhysics   Show physics debug info
   * @param showColliders Show collider outlines
   * @param showGrid      Show world grid
   * @return this GameEngine instance for method chaining
   */
  public GameEngine setDebugDisplay(boolean showPhysics, boolean showColliders, boolean showGrid) {
    this.debugPhysics = showPhysics;
    this.debugColliders = showColliders;
    this.debugGrid = showGrid;

    // Update renderer debug settings
    renderer.setDebugOptions(showPhysics, showColliders, showGrid);

    LOGGER.info("Debug display updated - Physics: " + showPhysics +
        ", Colliders: " + showColliders +
        ", Grid: " + showGrid);
    return this;
  }

  /**
   * Take a screenshot of the current game state
   *
   * @param filePath Path where to save the screenshot
   * @return true if successful, false otherwise
   */
  public boolean takeScreenshot(String filePath) {
    try {
      BufferedImage image = window.captureScreen();
      if (image != null) {
        File outputFile = new File(filePath);
        ImageIO.write(image, "png", outputFile);
        LOGGER.info("Screenshot saved to: " + filePath);
        return true;
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to save screenshot", e);
    }
    return false;
  }

  /**
   * Create and set active camera
   *
   * @param x    X position
   * @param y    Y position
   * @param zoom Initial zoom level
   * @return this GameEngine instance for method chaining
   */
  public GameEngine createCamera(float x, float y, float zoom) {
    Entity camera = cameraSystem.createCamera(window.getWidth(), window.getHeight(), x, y);
    CameraComponent camComponent = camera.get(CameraComponent.class);
    if (camComponent != null) {
      camComponent.setZoom(zoom);
    }
    cameraSystem.setActiveCamera(camera);
    return this;
  }

  /**
   * Wait for a specified duration in milliseconds
   * Used for timed events or transitions
   *
   * @param milliseconds Time to wait
   * @return this GameEngine instance for method chaining
   */
  public GameEngine delay(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      LOGGER.warning("Delay interrupted: " + e.getMessage());
      Thread.currentThread().interrupt();
    }
    return this;
  }

  /**
   * Switch to a new scene with a fade transition effect
   *
   * @param sceneName      Name of the scene to activate
   * @param fadeDurationMs Duration of the fade effect in milliseconds
   * @return this GameEngine instance for method chaining
   */
  public GameEngine transitionToScene(String sceneName, int fadeDurationMs) {
    // Create temporary fade overlay UI
    Entity fadeOverlay = uiSystem.createPanel(0, 0, window.getWidth(), window.getHeight());
    UIComponent uiComp = fadeOverlay.get(UIComponent.class);

    // Fade out (if fadeDurationMs > 0)
    if (fadeDurationMs > 0) {
      // Animation logic would go here
      delay(fadeDurationMs / 2);
    }

    // Change scene
    try {
      sceneManager.setActiveScene(sceneName);
    } catch (IllegalArgumentException e) {
      LOGGER.log(Level.SEVERE, "Failed to set active scene: " + sceneName, e);
      throw e;
    }

    // Fade in (if fadeDurationMs > 0)
    if (fadeDurationMs > 0) {
      // Animation logic would go here
      delay(fadeDurationMs / 2);
    }

    // Remove overlay
    uiComp.setVisible(false);

    return this;
  }

  /**
   * Configuration class for fluent engine setup
   */
  public class EngineConfig {
    private final GameEngine engine;

    EngineConfig(GameEngine engine) {
      this.engine = engine;
    }

    public EngineConfig targetFps(int fps) {
      engine.setTargetFps(fps);
      return this;
    }

    public EngineConfig showPerformanceStats(boolean show) {
      engine.showPerformanceStats = show;
      return this;
    }

    public EngineConfig debugMode(boolean physics, boolean colliders, boolean grid) {
      engine.setDebugDisplay(physics, colliders, grid);
      return this;
    }

    public EngineConfig gravity(float x, float y) {
      engine.getPhysicsWorld().setGravity(new Vec2(x, y));
      return this;
    }

    public EngineConfig windowTitle(String title) {
      engine.getWindow().setTitle(title);
      return this;
    }

    public GameEngine apply() {
      return engine;
    }
  }

  public boolean isDebugPhysics() {
    return debugPhysics;
  }

  public boolean isDebugColliders() {
    return debugColliders;
  }

  public boolean isDebugGrid() {
    return debugGrid;
  }
}
