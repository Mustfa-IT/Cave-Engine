package com.engine.di;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;

import org.jbox2d.common.Vec2;

import com.engine.core.CameraSystem;
import com.engine.core.GameEngine;
import com.engine.core.GameWindow;
import com.engine.entity.EntityFactory;
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

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dev.dominion.ecs.api.Dominion;

@Module
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
      int width = Integer.parseInt(config.getProperty("window.width", "1200"));
      int height = Integer.parseInt(config.getProperty("window.height", "800"));
      String title = config.getProperty("window.title", "Physics Game");
      return new GameWindow(title, width, height);
    }

    @Provides
    @Singleton
    public Vec2 provideGravity(Properties config) {
      return new Vec2(
          Float.parseFloat(config.getProperty("physics.gravityX", "0")),
          Float.parseFloat(config.getProperty("physics.gravityY", "9.8")));
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
    public UISystem provideUISystem(GameWindow window, Dominion ecs) {
      return new UISystem(window, ecs);
    }

    @Provides
    @Singleton
    public SceneManager provideSceneManager(GameEngine engine, EntityFactory entityFactory, UISystem uiSystem) {
      return new SceneManager(engine, entityFactory, uiSystem);
    }

    @Provides
    @Singleton
    public InputManager provideInputManager(GameWindow window, CameraSystem cameraSystem) {
      return new InputManager(window, cameraSystem);
    }

    @Provides
    @Singleton
    public RenderSystem provideRenderSystem(GameWindow window, Dominion ecs, CameraSystem cameraSystem) {
      return new RenderSystem(window, ecs, cameraSystem);
    }

    @Provides
    @Singleton
    public EventSystem provideEventSystem() {
      return new EventSystem();
    }

    @Provides
    @Singleton
    public AssetManager provideAssetManager() {
      return new AssetManager();
    }

    @Provides
    @Singleton
    public GameEngine provideGameEngine(GameWindow window, Dominion ecs, RenderSystem renderer,
        CameraSystem cameraSystem, PhysicsSystem physicsWorld,
        EntityFactory entityFactory, UISystem uiSystem,
        InputManager inputManager, Properties config,
        EventSystem eventSystem, AssetManager assetManager) {
      GameEngine engine = new GameEngine(window, ecs, renderer, cameraSystem,
          (PhysicsWorld) physicsWorld, entityFactory, uiSystem, inputManager,
          config, eventSystem, assetManager);

      // Initialize console right after engine creation
      engine.createConsole();

      return engine;
    }
  }
}
