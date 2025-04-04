package com;

import java.awt.event.KeyEvent;
import java.awt.Graphics2D;
import java.awt.Color;
import java.io.File;

import com.engine.core.GameEngine;
import com.engine.components.UIComponent;
import com.engine.core.AbstractGameObject;
import com.engine.gameobject.GameObject;
import com.engine.entity.EntityFactory.PhysicsParameters;
import com.engine.scene.SimpleScene;
import com.engine.ui.Button;
import com.engine.events.GameEvent;
import com.engine.events.EventTypes;
import com.engine.editor.EditorExample;

import dev.dominion.ecs.api.Entity;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyType;

public class Main {
  public static void main(String[] args) {
    // Create assets directory if it doesn't exist
    File assetsDir = new File("assets");
    if (!assetsDir.exists() && !assetsDir.mkdirs()) {
      System.err.println("Failed to create assets directory.");
    }

    // Use Dagger to create the engine with simplified initialization
    GameEngine game = GameEngine.createEngine();

    // Configure the engine with enhanced fluent API
    game.createScene("test", () -> new SimpleScene(game.getEntityFactory()));

    // Set active scene after all scenes are created
    game.setActiveScene("test");

    // Start the engine
    game.start();

    // Set up input handlers for creating custom GameObjects
    setupInputHandlers(game);

    // Create UI elements for controlling the engine
    setupUIControls(game);

    // Set up event listeners
    setupEventListeners(game);

    // Set up the editor system
    setupEditorSystem(game);
  }

  /**
   * Sets up event listeners to demonstrate the event system
   */
  private static void setupEventListeners(GameEngine game) {
    var eventSystem = game.getEventSystem();

    // Listen for scene changes
    eventSystem.addEventListener(EventTypes.SCENE_CHANGE, event -> {
      System.out.println("Scene changed to: " + event.getData("name", "unknown"));
    });

    // Listen for game paused/resumed
    eventSystem.addEventListener(EventTypes.GAME_PAUSE, event -> {
      System.out.println("Main Game paused");
    });

    eventSystem.addEventListener(EventTypes.GAME_RESUME, event -> {
      System.out.println("Main Game resumed");
    });

    // // Demonstrate wildcard listeners for physics events
    // eventSystem.addPatternListener("physics:*", event -> {
    // System.out.println("Physics event detected: " + event.getType());
    // });

    // // Listen for all collision events
    // eventSystem.addPatternListener("collision:*", event -> {
    // Entity entityA = event.getData("entityA", null);
    // Entity entityB = event.getData("entityB", null);
    // if (entityA != null && entityB != null) {
    // System.out.println("Collision " +
    // (event.getType().equals(EventTypes.COLLISION_BEGIN) ? "started" : "ended") +
    // " between entities: " + entityA + " and " + entityB);
    // }
    // });

    // Listen for gravity changes
    eventSystem.addEventListener(EventTypes.PHYSICS_GRAVITY_CHANGED, event -> {
      Vec2 gravity = event.getData("gravity", null);
      if (gravity != null) {
        System.out.println("Gravity changed to: " + gravity.x + ", " + gravity.y);
      }
    });

    // Fire an event when the game starts
    eventSystem.fireEvent(new GameEvent(EventTypes.GAME_START)
        .addData("time", System.currentTimeMillis())
        .addData("version", "1.0"));
  }

  /**
   * Sets up input handlers for various game controls
   *
   * @param game The game engine instance
   */
  private static void setupInputHandlers(GameEngine game) {
    // Get input manager from the engine
    var inputManager = game.getInputManager();
    var entityFactory = game.getEntityFactory();

    // Setup pause toggle with P key
    inputManager.onKeyPress(KeyEvent.VK_P, e -> {
      game.togglePause();
      return true; // Consume the event
    });

    // Debug visualization toggle with D key
    inputManager.onKeyPress(KeyEvent.VK_D, e -> {
      boolean currentDebugState = game.isDebugColliders();
      game.setDebugDisplay(!currentDebugState, !currentDebugState, game.isDebugGrid());
      return true; // Consume the event
    });

    // Scene switching
    inputManager.onKeyPress(KeyEvent.VK_1, e -> {
      game.setActiveScene("test");
      return true; // Consume the event
    });

    // Debug overlay toggle with F3
    inputManager.onKeyPress(KeyEvent.VK_F3, e -> {
      game.toggleDebugOverlay();
      return true; // Consume the event
    });

    // Console toggle with backquote/tilde
    inputManager.onKeyPress(KeyEvent.VK_BACK_QUOTE, e -> {
      game.toggleConsole();
      return true; // Consume the event
    });

    // Screenshot with F12
    inputManager.onKeyPress(KeyEvent.VK_F12, e -> {
      game.takeScreenshot("screenshot_" + System.currentTimeMillis() + ".png");
      return true; // Consume the event
    });

    // Create physics GameObject with right click
    inputManager.onMousePress(3, e -> {
      // Only process if editor is not active
      if (game.getInjector().editor().isActive()) {
        return false; // Let editor handle it
      }

      float[] worldPos = inputManager.getMouseWorldPosition();
      PhysicsParameters circlePhysics = PhysicsParameters.circle(
          30, BodyType.DYNAMIC, 1.0f, 0.3f, 0.5f);
      entityFactory.createGameObject(
          worldPos[0], worldPos[1],
          new BouncingObject(),
          circlePhysics);
      return true; // Consume the event
    });

    // Create box physics GameObject with middle click
    inputManager.onMousePress(2, e -> {
      // Only process if editor is not active
      if (game.getInjector().editor().isActive()) {
        return false; // Let editor handle it
      }

      float[] worldPos = inputManager.getMouseWorldPosition();
      Entity entity = entityFactory.createGameObject(worldPos[0], worldPos[1], new ColorChangingBox());
      entityFactory.addBoxPhysics(entity, 40, 40, BodyType.DYNAMIC, 0.5f, 0.3f, 0.8f);
      return true; // Consume the event
    });
  }

  /**
   * Sets up UI controls for the engine
   */
  private static void setupUIControls(GameEngine game) {
    var uiSystem = game.getUiSystem();

    // Create gravity slider
    Entity gravitySlider = uiSystem.createSlider("Gravity", 40, 50, 150, 20, -20, 20, 9.8f);
    var button = uiSystem.createButton("Click Me", 40, 80, 50, 20);
    var b = (Button) button.get(UIComponent.class).getUi();
    b.addClickListener((e) -> {
      System.out.println("Button Clicked");
    });
    // Set callback for when value changes
    uiSystem.setSliderCallback(gravitySlider, (value) -> {
      game.getPhysicsWorld().setGravity(new org.jbox2d.common.Vec2(0, -value));
    });

  }

  /**
   * Example custom GameObject that rotates and draws a square
   */
  static class RotatingSquare implements GameObject {
    private final float size;
    private final Color color;
    private float rotation = 0;

    public RotatingSquare(float size, Color color) {
      this.size = size;
      this.color = color;
    }

    @Override
    public void onStart(Entity entity) {
      System.out.println("RotatingSquare started!");
    }

    @Override
    public void update(double deltaTime) {
      // Rotate the square slowly
      rotation += deltaTime * 1.5f;
    }

    @Override
    public void render(Graphics2D g) {
      g.rotate(rotation);
      g.setColor(color);
      g.fillRect((int) (-size / 2), (int) (-size / 2), (int) size, (int) size);
    }

    @Override
    public void onDestroy() {
      System.out.println("RotatingSquare destroyed!");
    }
  }

  /**
   * Example physics GameObject that changes color when active
   */
  static class BouncingObject extends AbstractGameObject {
    private Color color = Color.CYAN;
    private float time = 0;

    @Override
    public void onStart(Entity entity) {
      super.onStart(entity);
      System.out.println("BouncingObject started!");
    }

    @Override
    public void update(double deltaTime) {
      time += deltaTime;

      // Cycle through colors based on time
      float hue = (time * 0.5f) % 1.0f;
      color = Color.getHSBColor(hue, 0.8f, 1.0f);
    }

    @Override
    public void render(Graphics2D g) {
      g.setColor(color);
      g.fillOval(-15, -15, 30, 30);

      // Draw a simple face
      g.setColor(Color.BLACK);
      g.fillOval(-7, -7, 4, 4); // left eye
      g.fillOval(3, -7, 4, 4); // right eye
      g.drawArc(-8, 0, 16, 8, 0, -180); // smile
    }

    @Override
    public void onDestroy() {
      System.out.println("BouncingObject destroyed!");
    }

  }

  /**
   * Example of a GameObject that changes color and will have physics added after
   * creation
   */
  static class ColorChangingBox extends AbstractGameObject {
    private Color color = Color.GREEN;
    private float time = 0;

    @Override
    public void update(double deltaTime) {
      time += deltaTime;
      // Change color based on time
      color = Color.getHSBColor((time * 0.3f) % 1.0f, 0.7f, 0.9f);
    }

    @Override
    public void render(Graphics2D g) {
      g.setColor(color);
      g.fillRect(-20, -20, 40, 40);
    }

    @Override
    public void onDestroy() {
      System.out.println("ColorChangingBox destroyed!");
    }

  }

  /**
   * Sets up the editor system
   */
  private static void setupEditorSystem(GameEngine game) {
    EditorExample.integrateWithGame(game);
  }

}
