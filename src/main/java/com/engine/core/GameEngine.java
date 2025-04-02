package com.engine.core;

import com.engine.graph.Renderer;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Scheduler;

public class GameEngine {
  private GameWindow window;
  private boolean running;
  public Dominion world;
  private Renderer renderer;
  // flag to check if the game engine was closed by the window in a normal way
  // if the game engine close and this is false then the game engine was
  // Interrupted either by Ctrl + C or something else
  private boolean closedByWindow;
  private Scheduler scheduler;

  public GameEngine() {
    this.window = new GameWindow("My Game");
    this.window.setOnClose(() -> {
      stop();
      this.closedByWindow = true;
      return null;
    });
    this.addShutdownHook(new Thread(this::stop));
    this.world = Dominion.create();
    this.renderer = new Renderer(window, world);
    // Create a scheduler and schedule the render system
    this.scheduler = world.createScheduler();
    this.scheduler.schedule(() -> renderer.render()); // Add render system
    // Debugging: Log entity creation
    System.out.println("GameEngine initialized. Dominion world created.");
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

  public Dominion getWorld() {
    return world;
  }

  public void setWorld(Dominion world) {
    this.world = world;
  }

  public Renderer getRenderer() {
    return renderer;
  }

  public void setRenderer(Renderer renderer) {
    this.renderer = renderer;
  }
}
