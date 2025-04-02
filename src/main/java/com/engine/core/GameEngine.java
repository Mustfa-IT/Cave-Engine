package com.engine.core;

import org.jbox2d.common.Vec2;

import com.engine.entity.EntityFactory;
import com.engine.graph.RenderSystem;
import com.engine.pyhsics.PhysicsWorld;
import com.engine.scene.Scene;
import com.engine.scene.SceneManager;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Scheduler;

public class GameEngine {
  private GameWindow window;
  private boolean running;
  public Dominion ecs;
  private RenderSystem renderer;
  private PhysicsWorld physicsWorld;
  private Vec2 defaultGravity = new Vec2(0, 9.8f);
  private boolean closedByWindow;
  private Scheduler scheduler;

  // Integrated components
  private EntityFactory entityFactory;
  private SceneManager sceneManager;

  public GameEngine() {
    this.window = new GameWindow("Physics Game");
    this.window.setOnClose(() -> {
      stop();
      this.closedByWindow = true;
      return null;
    });
    this.addShutdownHook(new Thread(this::stop));
    init();
    System.out.println("GameEngine initialized. Dominion world created.");
  }

  private void init() {
    // Core systems initialization
    this.ecs = Dominion.create();
    this.physicsWorld = new PhysicsWorld(defaultGravity, ecs);
    this.renderer = new RenderSystem(window, ecs);

    // Create entity factory
    this.entityFactory = new EntityFactory(ecs, physicsWorld);

    // Create scene manager with reference to this engine
    this.sceneManager = new SceneManager(this);

    // Setup the main loop scheduler
    this.scheduler = ecs.createScheduler();
    this.scheduler.schedule(this::update);
    this.scheduler.schedule(() -> renderer.render());
  }

  /**
   * Main engine update method, calls physics and scene updates
   */
  private void update() {
    double deltaTime = this.scheduler.deltaTime();

    // Update physics
    this.physicsWorld.update(deltaTime);

    // Update current scene
    if (sceneManager.getCurrentScene() != null) {
      sceneManager.update(deltaTime);
    }
  }

  /**
   * Register a scene with the engine
   */
  public void registerScene(String name, Scene scene) {
    sceneManager.registerScene(name, scene);
  }

  /**
   * Set the active scene by name
   */
  public void setActiveScene(String sceneName) {
    sceneManager.setActiveScene(sceneName);
  }

  /**
   * Initialize all scenes
   */
  public void initializeScenes() {
    sceneManager.initializeAllScenes();
  }

  public void start() {
    if (running)
      return;
    running = true;
    this.window.initialize();
    scheduler.tickAtFixedRate(60); // Start scheduler at 60 FPS
  }

  public void stop() {
    if (closedByWindow)
      return;
    running = false;
    System.out.println("Stopping the Game Engine...");
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
    return running;
  }

  public void setRunning(boolean running) {
    this.running = running;
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
}
