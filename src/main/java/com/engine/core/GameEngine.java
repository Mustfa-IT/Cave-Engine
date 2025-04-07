package com.engine.core;

import com.engine.components.CameraComponent;
import com.engine.components.UIComponent;
import com.engine.di.DaggerEngineComponent;
import com.engine.di.EngineComponent;
import com.engine.di.EngineModule;
import com.engine.components.GameObjectComponent;
import com.engine.entity.EntityFactory;
import com.engine.graph.OverlayRenderer;
import com.engine.graph.RenderSystem;
import com.engine.input.InputManager;
import com.engine.physics.PhysicsWorld;
import com.engine.scene.Scene;
import com.engine.scene.SceneManager;
import com.engine.ui.UISystem;
import com.engine.events.EventSystem;
import com.engine.events.GameEvent;
import com.engine.assets.AssetManager;
import com.engine.animation.AnimationSystem;
import com.engine.particles.ParticleSystem;
import com.engine.particles.ParticleEmitter;
import com.engine.audio.AudioSystem;
import com.engine.audio.AudioListenerComponent;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Scheduler;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GameEngine implements OverlayRenderer {
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

  private final GameFrame gameFrame;
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
  private Entity debugOverlayEntity;
  private com.engine.ui.DebugOverlay debugOverlay;

  // Console system
  private Console console;

  // Integrated components
  private final EntityFactory entityFactory;
  private SceneManager sceneManager;
  private final UISystem uiSystem;
  private final InputManager inputManager;
  private final EngineConfig config;

  // New systems
  private final EventSystem eventSystem;
  private final AssetManager assetManager;
  private final AnimationSystem animationSystem;
  private final ParticleSystem particleSystem;
  private final AudioSystem audioSystem;

  // List to store additional overlay renderers
  private final List<Consumer<Graphics2D>> overlayRenderers = new ArrayList<>();

  // Reference to the engine component for DI access
  private static EngineComponent engineComponent;

  @Inject
  public GameEngine(GameFrame gameFrame, GameWindow window, Dominion ecs, RenderSystem renderer,
      CameraSystem cameraSystem, PhysicsWorld physicsWorld,
      EntityFactory entityFactory, UISystem uiSystem,
      InputManager inputManager, EngineConfig config,
      EventSystem eventSystem, AssetManager assetManager,
      AnimationSystem animationSystem, ParticleSystem particleSystem,
      AudioSystem audioSystem) {
    this.gameFrame = gameFrame;
    this.window = window; // Use the provided window instance
    this.ecs = ecs;
    this.renderer = renderer;
    this.cameraSystem = cameraSystem;
    this.physicsWorld = physicsWorld;
    this.entityFactory = entityFactory;
    this.uiSystem = uiSystem;
    this.inputManager = inputManager;
    this.config = config;
    this.eventSystem = eventSystem;
    this.assetManager = assetManager;
    this.animationSystem = animationSystem;
    this.particleSystem = particleSystem;
    this.audioSystem = audioSystem;
    this.debugGrid = this.config.isShowGrid();
    this.debugColliders = this.config.isDebugColliders();
    this.debugPhysics = this.config.isDebugPhysics();
    this.targetFps = this.config.getTargetFps();

    // Tell the renderer about this engine so it can call renderOverlays
    renderer.setOverlayRenderer(this);

    // Register game engine events
    setupEventListeners();

    // Set up window close listener to properly shut down the engine
    window.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        stop();
      }
    });

    addShutdownHook(new Thread(this::stop));
    init();
    LOGGER.info("GameEngine initialized. Dominion world created.");
  }

  private void init() {
    try {
      // Set up the window resize handler
      this.gameFrame.setOnResize((width, height) -> {
        cameraSystem.updateAllViewports(width, height);
      });

      // Register UI system with input manager
      uiSystem.registerWithInputManager(inputManager);

      // Register editor with input manager
      if (engineComponent != null && engineComponent.editor() != null) {
        engineComponent.editor().registerInputHandlers(inputManager);
      }

      // Create camera at world origin (0,0)
      this.cameraSystem.createCamera(gameFrame.getWidth(), gameFrame.getHeight(), 0, 0);
      // Create scene manager with reference to engine, entity factory, and UI system
      this.sceneManager = new SceneManager(this, entityFactory, uiSystem);

      // Initialize audio system
      if (!audioSystem.initialize()) {
        LOGGER.warning("Failed to initialize audio system");
      }

      // Connect AssetManager to AudioSystem for audio loading
      assetManager.setAudioSystem(audioSystem, eventSystem);

      // Setup the main loop scheduler
      this.scheduler = ecs.createScheduler();
      this.scheduler.schedule(this::update);
      this.scheduler.schedule(() -> renderer.render());
      this.scheduler.schedule(() -> cameraSystem.update((float) this.scheduler.deltaTime()));
      this.scheduler.schedule(() -> uiSystem.update(this.scheduler.deltaTime()));
      this.scheduler.schedule(() -> animationSystem.update(this.scheduler.deltaTime()));
      // Add particle system to the scheduler
      this.scheduler.schedule(() -> particleSystem.update((float) this.scheduler.deltaTime()));
      // Add audio system to the scheduler
      this.scheduler.schedule(() -> audioSystem.update(this.scheduler.deltaTime()));

      // Initialize FPS counter
      lastFpsReportTime = System.currentTimeMillis();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize game engine", e);
      throw new RuntimeException("Engine initialization failed", e);
    }
  }

  private void setupEventListeners() {
    // Listen for game pause/resume events
    eventSystem.addEventListener("game:pause", event -> pause());
    eventSystem.addEventListener("game:resume", event -> resume());

    // Listen for scene change events
    eventSystem.addEventListener("scene:change", event -> {
      String sceneName = event.getData("name", "");
      if (!sceneName.isEmpty()) {
        setActiveScene(sceneName);
      }
    });
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

      // Process events
      eventSystem.processEvents();

      // Update physics
      this.physicsWorld.update(deltaTime);

      // Update GameObjects
      updateGameObjects(deltaTime);

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

      // Update debug stats
      updateDebugStats();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error in game update loop", e);
    }
  }

  /**
   * Update all GameObjects in the world
   */
  private void updateGameObjects(double deltaTime) {
    ecs.findEntitiesWith(GameObjectComponent.class).forEach(result -> {
      Entity entity = result.entity();
      GameObjectComponent component = result.comp();

      // Skip if the GameObject has been destroyed
      if (component.isDestroyed()) {
        return;
      }

      // Initialize GameObject if needed
      component.initIfNeeded(entity);

      // Update GameObject
      component.getGameObject().update(deltaTime);
    });
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

    // Make the window visible now that everything is set up
    this.window.setVisible(true);
    this.gameFrame.initialize();

    this.cameraSystem.updateAllViewports(gameFrame.getWidth(), gameFrame.getHeight());
    this.setDebugDisplay(debugPhysics, debugColliders, this.debugGrid);
    LOGGER.info("Starting the Game Engine with target FPS: " + targetFps);
    scheduler.tickAtFixedRate(targetFps); // Start scheduler at target FPS
    return this;
  }

  public void stop() {
    if (closedByWindow)
      return;

    engineState = State.STOPPED;
    LOGGER.info("Stopping the Game Engine...");

    // Clean up resources
    audioSystem.shutdown(); // Shutdown audio resources
    assetManager.shutdown();
    // Notify listeners
    eventSystem.fireEvent(new GameEvent("game:shutdown"));
    scheduler.shutDown(); // Stop the scheduler

    // Properly dispose the window
    window.dispose();
  }

  public void addShutdownHook(Thread hook) {
    Runtime.getRuntime().addShutdownHook(hook);
  }

  public GameFrame getGameFrame() {
    return gameFrame;
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
   * Get the current FPS rate
   *
   * @return Current frames per second
   */
  public double getFps() {
    return averageFps;
  }

  /**
   * Creates and initializes the debug overlay
   */
  public GameEngine createDebugOverlay() {
    if (debugOverlayEntity == null) {
      debugOverlayEntity = uiSystem.createDebugOverlay(10, 10);
      if (debugOverlayEntity != null && debugOverlayEntity.has(UIComponent.class)) {
        UIComponent uiComp = debugOverlayEntity.get(UIComponent.class);
        debugOverlay = (com.engine.ui.DebugOverlay) uiComp.getUi();
        LOGGER.info("Debug overlay created");
      }
    }
    return this;
  }

  /**
   * Toggle visibility of the debug overlay
   */
  public GameEngine toggleDebugOverlay() {
    if (debugOverlay != null) {
      debugOverlay.toggleVisibility();
      LOGGER.info("Debug overlay visibility toggled: " + debugOverlay.isVisible());
    } else {
      createDebugOverlay();
    }
    return this;
  }

  /**
   * Update debug overlay with current stats
   */
  private void updateDebugStats() {
    if (debugOverlay != null && debugOverlay.isVisible()) {
      // Update FPS
      debugOverlay.updateStat("FPS", averageFps);
      debugOverlay.updateStat("Gravity", this.config.getGravity().toString());
      // // Update entity count
      // int entityCount = ecs.findEntitiesWith().count();
      // debugOverlay.updateStat("Entities", entityCount);

      // Update memory usage
      Runtime runtime = Runtime.getRuntime();
      long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
      debugOverlay.updateStat("Memory", usedMemory + " MB");

      // Update physics stats
      debugOverlay.updateStat("Bodies", physicsWorld.getBodyCount());

      // Update particle stats
      if (particleSystem != null) {
        debugOverlay.updateStat("Particles", particleSystem.getParticleCount());
      }

      // Update assets count
      debugOverlay.updateStat("Assets", assetManager.getAssetCount());
    }
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
    EngineConfig config = new EngineConfig();
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
      BufferedImage image = gameFrame.captureScreen();
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
    Entity camera = cameraSystem.createCamera(gameFrame.getWidth(), gameFrame.getHeight(), x, y);
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
    Entity fadeOverlay = uiSystem.createPanel(0, 0, gameFrame.getWidth(), gameFrame.getHeight());
    UIComponent uiComp = fadeOverlay.get(UIComponent.class);

    // Fade out (if fadeDurationMs > 0)
    if (fadeDurationMs > 0) {
      // Animation logic would go here
      // TODO:IMPROVE ON THIS PART
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
   * Creates and initializes the console system
   */
  public GameEngine createConsole() {
    if (console == null) {
      console = new Console(this);

      // Set up key event handling for the console
      inputManager.addKeyListener(e -> {
        if (console.isVisible()) {
          console.handleKeyInput(e);
          return true; // Consume the event when console is open
        }
        return false;
      });

      // Set up key typed event handling
      inputManager.addKeyTypedListener(e -> {
        if (console.isVisible()) {
          console.handleTypedKey(e.getKeyChar());
          return true;
        }
        return false;
      });

      LOGGER.info("Console system initialized");
    }
    return this;
  }

  /**
   * Toggle console visibility
   */
  public GameEngine toggleConsole() {
    if (console == null) {
      createConsole();
    }
    console.toggleVisibility();
    LOGGER.info("Console visibility: " + console.isVisible());
    return this;
  }

  /**
   * Render any additional engine overlays like console
   * Implementation of OverlayRenderer interface
   */
  @Override
  public void renderOverlays(Graphics2D g) {
    if (console != null && console.isVisible()) {
      console.render(g, gameFrame.getWidth());
    }

    // Render all additional overlay renderers
    for (var renderer : overlayRenderers) {
      renderer.accept(g);
    }
  }

  /**
   * Adds an additional overlay renderer
   *
   * @param renderer The renderer function that takes a Graphics2D object
   * @return this GameEngine instance for method chaining
   */
  public GameEngine addOverlayRenderer(Consumer<Graphics2D> renderer) {
    overlayRenderers.add(renderer);
    return this;
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

  /**
   * Get the event system
   *
   * @return The event system instance
   */
  public EventSystem getEventSystem() {
    return eventSystem;
  }

  /**
   * Get the asset manager
   *
   * @return The asset manager instance
   */
  public AssetManager getAssetManager() {
    return assetManager;
  }

  /**
   * Get the animation system
   *
   * @return The animation system instance
   */
  public AnimationSystem getAnimationSystem() {
    return animationSystem;
  }

  /**
   * Get the particle system
   *
   * @return The particle system instance
   */
  public ParticleSystem getParticleSystem() {
    return particleSystem;
  }

  /**
   * Create a particle effect at the specified position
   *
   * @param type The type of effect ("fire", "smoke", "explosion")
   * @param x    X position
   * @param y    Y position
   * @return The created emitter
   */
  public ParticleEmitter createParticleEffect(String type, float x, float y) {
    if (particleSystem == null) {
      LOGGER.warning("Particle system not initialized");
      return null;
    }

    switch (type.toLowerCase()) {
      case "fire":
        return particleSystem.createFire(x, y);
      case "smoke":
        return particleSystem.createSmoke(x, y);
      case "explosion":
        return particleSystem.createExplosion(x, y, 30);
      default:
        LOGGER.warning("Unknown particle effect type: " + type);
        return null;
    }
  }

  /**
   * Create a custom particle emitter with the specified parameters
   *
   * @param type   Emitter type ("color", "sprite", "physical")
   * @param x      X position
   * @param y      Y position
   * @param params Configuration parameters
   * @return The created emitter
   */
  public ParticleEmitter createParticleEmitter(String type, float x, float y, Map<String, Object> params) {
    if (particleSystem == null) {
      LOGGER.warning("Particle system not initialized");
      return null;
    }

    return particleSystem.createEmitter(type, x, y, params);
  }

  /**
   * Attach a particle emitter to an entity
   *
   * @param emitter The particle emitter
   * @param entity  The entity to attach to
   */
  public void attachParticleEmitterToEntity(ParticleEmitter emitter, Entity entity) {
    if (particleSystem != null) {
      particleSystem.attachEmitterToEntity(emitter, entity);
    }
  }

  /**
   * Get access to the Dagger injector
   *
   * @return The EngineComponent injector
   */
  public EngineComponent getInjector() {
    return engineComponent;
  }

  /**
   * Add a keyboard listener for editor property editing
   *
   * @param handler The handler for keyboard events
   */
  public void addEditorKeyListener(Consumer<KeyEvent> handler) {
    inputManager.addKeyTypedListener(e -> {
      if (engineComponent.editor().isActive()) {
        handler.accept(e);
        return true;
      }
      return false;
    });
  }

  public static GameEngine createEngine() {
    engineComponent = DaggerEngineComponent.builder()
        .concreteModule(new EngineModule.ConcreteModule())
        .build();

    GameEngine engine = engineComponent.engine();

    // Register editor with input manager after creation
    if (engineComponent.editor() != null) {
      engineComponent.editor().registerInputHandlers(engine.getInputManager());
    }

    return engine;
  }

  public void scheduleTask(Runnable r) {
    scheduler.schedule(r);
  }

  public double getDeltaTime() {
    return this.scheduler.deltaTime();
  }

  /**
   * Create an audio listener on the given entity (typically the camera)
   *
   * @param entity Entity to attach audio listener to
   * @return The engine instance for method chaining
   */
  public GameEngine createAudioListener(Entity entity) {
    audioSystem.createAudioListener(entity);
    return this;
  }

  /**
   * Play a sound from the entity's position
   *
   * @param entity  The entity to play the sound from
   * @param soundId The identifier of the loaded sound asset
   * @param loop    Whether the sound should loop
   * @return The engine instance for method chaining
   */
  public GameEngine playSound(Entity entity, String soundId, boolean loop) {
    audioSystem.playSound(entity, soundId, loop);
    return this;
  }

  /**
   * Stop a sound that's currently playing on an entity
   *
   * @param entity The entity to stop sounds on
   * @return The engine instance for method chaining
   */
  public GameEngine stopSound(Entity entity) {
    audioSystem.stopSound(entity);
    return this;
  }

  /**
   * Set the global audio mute state
   *
   * @param muted Whether audio should be muted
   * @return The engine instance for method chaining
   */
  public GameEngine setAudioMuted(boolean muted) {
    audioSystem.setMuted(muted);
    return this;
  }

  /**
   * Set the global master volume
   *
   * @param volume Volume level from 0.0 to 1.0
   * @return The engine instance for method chaining
   */
  public GameEngine setMasterVolume(float volume) {
    audioSystem.setMasterVolume(volume);
    return this;
  }

  /**
   * Get the audio system
   *
   * @return The audio system instance
   */
  public AudioSystem getAudioSystem() {
    return audioSystem;
  }

}
