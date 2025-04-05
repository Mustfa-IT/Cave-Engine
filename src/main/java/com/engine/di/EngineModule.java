package com.engine.di;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;

import com.engine.core.CameraSystem;
import com.engine.core.EngineConfig;
import com.engine.core.GameEngine;
import com.engine.core.GameFrame;
import com.engine.core.GameWindow;
import com.engine.entity.EntityFactory;
import com.engine.graph.DebugRenderer;
import com.engine.graph.OverlayRenderer;
import com.engine.graph.RenderSystem;
import com.engine.graph.RenderingSystem;
import com.engine.input.InputManager;
import com.engine.physics.PhysicsSystem;
import com.engine.physics.PhysicsWorld;
import com.engine.scene.SceneManager;
import com.engine.ui.UISystem;
import com.engine.assets.AssetManager;
import com.engine.events.EventSystem;
import com.engine.animation.AnimationSystem;
import com.engine.editor.EditorModule;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dev.dominion.ecs.api.Dominion;

@Module(includes = { EditorModule.class })
public abstract class EngineModule {
  private static final Logger LOGGER = Logger.getLogger(EngineModule.class.getName());

  // Bind implementations to interfaces
  @Binds
  @Singleton
  abstract RenderingSystem bindRenderSystem(RenderSystem impl);

  @Binds
  @Singleton
  abstract PhysicsSystem bindPhysicsSystem(PhysicsWorld impl);

  @Binds
  @Singleton
  abstract OverlayRenderer bindOverlayRenderer(GameEngine impl);

  // Module with concrete providers
  @Module
  public static class ConcreteModule {
    private final Properties config;

    public ConcreteModule() {
      this.config = loadConfig();
    }

    private Properties loadConfig() {
      Properties config = new Properties();
      try {
        FileInputStream in = new FileInputStream("config.properties");
        config.load(in);
        in.close();
        LOGGER.info("Configuration loaded successfully");
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, "Could not load config.properties. Using defaults.", e);
      }
      return config;
    }

    @Provides
    @Singleton
    public Properties provideConfig() {
      return config;
    }

    @Provides
    @Singleton
    public Dominion provideECS() {
      return Dominion.create();
    }

    @Provides
    @Singleton
    public GameWindow provideGameWindow(Properties config) {
      int width = Integer.parseInt(config.getProperty("window.width", "800"));
      int height = Integer.parseInt(config.getProperty("window.height", "600"));
      String title = config.getProperty("window.title", "Physics Game");

      // Create the window first
      GameWindow window = new GameWindow();
      window.setTitle(title);
      window.setSize(width, height);
      window.setLocationRelativeTo(null);
      window.setDefaultCloseOperation(GameWindow.DISPOSE_ON_CLOSE);

      return window;
    }

    @Provides
    @Singleton
    public GameFrame provideGameFrame(GameWindow window, Properties config) {
      int width = Integer.parseInt(config.getProperty("window.width", "800"));
      int height = Integer.parseInt(config.getProperty("window.height", "600"));
      String title = config.getProperty("window.title", "Physics Game");

      // Create the frame with dimensions
      GameFrame gameFrame = new GameFrame(title, width, height);

      // Add frame to window
      window.add(gameFrame);

      return gameFrame;
    }

    @Provides
    @Singleton
    public CameraSystem provideCameraSystem(Dominion ecs) {
      return new CameraSystem(ecs);
    }

    @Provides
    @Singleton
    public EntityFactory provideEntityFactory(Dominion ecs, PhysicsSystem physicsWorld) {
      return new EntityFactory(ecs, (PhysicsWorld) physicsWorld);
    }

    @Provides
    @Singleton
    public UISystem provideUISystem(GameFrame window, Dominion ecs, EventSystem evnetSystem) {
      return new UISystem(window, ecs, evnetSystem);
    }

    @Provides
    @Singleton
    public SceneManager provideSceneManager(GameEngine engine, EntityFactory entityFactory, UISystem uiSystem) {
      return new SceneManager(engine, entityFactory, uiSystem);
    }

    @Provides
    @Singleton
    public InputManager provideInputManager(GameFrame window, CameraSystem cameraSystem) {
      return new InputManager(window, cameraSystem);
    }

    @Provides
    @Singleton
    public RenderSystem provideRenderSystem(GameFrame window, Dominion ecs, CameraSystem cameraSystem,
        EventSystem eventSystem) {
      return new RenderSystem(window, ecs, cameraSystem, eventSystem, new DebugRenderer(ecs));
    }

    @Provides
    @Singleton
    public EventSystem provideEventSystem(EngineConfig config) {
      return new EventSystem(config);
    }

    @Provides
    @Singleton
    public AssetManager provideAssetManager() {
      return new AssetManager();
    }

    @Provides
    @Singleton
    public AnimationSystem provideAnimationSystem(Dominion ecs, EventSystem eventSystem) {
      return new AnimationSystem(ecs, eventSystem);
    }

    @Provides
    @Singleton
    public EngineConfig provideEngineConfig() {
      EngineConfig engineConfig = new EngineConfig();

      // Set default values from properties file or use hardcoded defaults
      engineConfig.physicsIterations(
          Integer.parseInt(config.getProperty("physics.velocityIterations", "10")),
          Integer.parseInt(config.getProperty("physics.positionIterations", "8")));

      engineConfig.physicsTimeStep(
          Float.parseFloat(config.getProperty("physics.timeStep", "0.016667")));

      engineConfig.enableBodySleeping(
          Boolean.parseBoolean(config.getProperty("physics.enableSleeping", "false")));

      engineConfig.broadphaseOptimization(
          Boolean.parseBoolean(config.getProperty("physics.optimizeBroadphase", "false")));
      engineConfig.debugMode(
          Boolean.parseBoolean(config.getProperty("physics.debug", "false")),
          Boolean.parseBoolean(config.getProperty("collision.debug", "false")),
          Boolean.parseBoolean(config.getProperty("render.debug", "false")));
      engineConfig
          .setDebugEvents(Boolean.parseBoolean(config.getProperty("event.debug", "false")));

      engineConfig.targetFps(
          Integer.parseInt(config.getProperty("engine.targetFps", "60")));

      engineConfig
          .showPerformanceStats(Boolean.parseBoolean(config.getProperty("debug.showPerformanceStats", "false")));
      engineConfig.gravity(
          Float.parseFloat(config.getProperty("physics.gravityX", "0")),
          Float.parseFloat(config.getProperty("physics.gravityY", "9.8")));
      return engineConfig;
    }

    @Provides
    @Singleton
    public GameEngine provideGameEngine(GameFrame gameFrame, GameWindow window, // Add window parameter
        Dominion ecs, RenderSystem renderer,
        CameraSystem cameraSystem, PhysicsSystem physicsWorld,
        EntityFactory entityFactory, UISystem uiSystem,
        InputManager inputManager, EngineConfig config,
        EventSystem eventSystem, AssetManager assetManager,
        AnimationSystem animationSystem) {

      GameEngine engine = new GameEngine(gameFrame, window, ecs, renderer, cameraSystem, // Pass window here
          (PhysicsWorld) physicsWorld, entityFactory, uiSystem, inputManager,
          config, eventSystem, assetManager, animationSystem);

      // Initialize console right after engine creation
      engine.createConsole();

      return engine;
    }
  }
}
