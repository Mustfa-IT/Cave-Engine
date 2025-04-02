package com.engine.core;

import org.jbox2d.common.Vec2;

import com.engine.graph.RenderSystem;
import com.engine.pyhsics.PhysicsWorld;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Scheduler;

public class GameEngine {
  private GameWindow window;
  private boolean running;
  public Dominion ecs;
  private RenderSystem renderer;
  private PhysicsWorld physicsWorld; // Fixed typo from 'pyhsic'
  // In Box2D, Y-up is positive, so we need a negative gravity for downward force
  private Vec2 defaultGravity = new Vec2(1, 9.8f);
  // flag to check if the game engine was closed by the window in a normal way
  // if the game engine close and this is false then the game engine was
  // Interrupted either by Ctrl + C or something else
  private boolean closedByWindow;
  private Scheduler scheduler;

  public GameEngine() {
    this.window = new GameWindow("Physics Game");
    this.window.setOnClose(() -> {
      stop();
      this.closedByWindow = true;
      return null;
    });
    this.addShutdownHook(new Thread(this::stop));
    init();
    // Debugging: Log entity creation
    System.out.println("GameEngine initialized. Dominion world created.");
  }

  private void init() {
    this.ecs = Dominion.create();
    this.physicsWorld = new PhysicsWorld(defaultGravity, ecs);
    this.renderer = new RenderSystem(window, ecs);
    this.scheduler = ecs.createScheduler();
    this.scheduler.schedule(() -> renderer.render());
    this.scheduler.schedule(() -> this.physicsWorld.update(this.scheduler.deltaTime()));
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

  public PhysicsWorld getPhysicsWorld() {
    return physicsWorld;
  }
}
