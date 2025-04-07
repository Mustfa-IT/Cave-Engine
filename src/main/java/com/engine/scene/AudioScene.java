package com.engine.scene;

import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.inject.Inject;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import com.engine.audio.AudioListenerComponent;
import com.engine.audio.AudioSourceComponent;
import com.engine.components.PhysicsBodyComponent;
import com.engine.components.RenderableComponent;
import com.engine.components.Transform;
import com.engine.components.UIComponent;
import com.engine.components.RigidBody.Type;
import com.engine.entity.EntityFactory;
import com.engine.events.EventSystem;
import com.engine.events.EventTypes;
import com.engine.graph.Circle;
import com.engine.graph.Rect;
import com.engine.graph.Renderable;
import com.engine.input.InputManager;
import com.engine.ui.Slider;

import dev.dominion.ecs.api.Entity;

public class AudioScene extends Scene {
  private EventSystem eventSystem;
  private InputManager inputManager;

  // Audio sources
  private Entity musicSource;
  private Entity bellSource;

  private Slider masterVolumeSlider;

  // Player entity (with listener)
  private Entity player;

  // Track if audio is initialized
  private boolean audioInitialized = false;

  // Audio file IDs
  private static final String MUSIC_ID = "audio_demo_music";
  private static final String BELL_ID = "audio_demo_bell";

  @Inject
  public AudioScene(EntityFactory entityFactory) {
    super(entityFactory);
  }

  @Override
  public void initialize() {
    eventSystem = engine.getEventSystem();
    inputManager = engine.getInputManager();

    // Set up camera at (0,0) with zoom of 1.0
    engine.createCamera(0, 0, 1.0f);
    engine.getCameraSystem().getActiveCamera().add(new AudioListenerComponent());

    // Create player
    player = entityFactory.createDynamicCircle(0, 0, 5, Color.RED);

    // Load all audio assets
    loadAudioAssets();

    // Create the scene
    createSceneLayout();

    // Set up UI controls
    createUI();

    // Set up input handling
    setupInputHandlers();

    // Create event listeners
    setupEventListeners();

    // Play background music
    if (musicSource != null) {
      engine.playSound(musicSource, MUSIC_ID, true);
    }

    audioInitialized = true;
  }

  private void loadAudioAssets() {
    // Load music (looping background track)
    engine.getAssetManager().loadAudio(MUSIC_ID, "audio/demo/music_loop.wav");

    // Load sound effects
    engine.getAssetManager().loadAudio(BELL_ID, "audio/demo/bell.wav");
  }

  private void createSceneLayout() {
    // Create boundary walls
    createWalls();

    // Create music source (fixed position)
    musicSource = entityFactory.createCircle(-100f, -100f, 20f, Type.STATIC, Color.BLUE);
    engine.getAudioSystem().createAudioSource(musicSource);

    // Create bell sound rectangle
    bellSource = entityFactory.createGround(0, 0, 50, 50, Color.ORANGE);
    engine.getAudioSystem().createAudioSource(bellSource);
  }

  private void createWalls() {
    // Create boundary walls
    float wallThickness = 10;
    float arenaSize = 300;

    // Top wall
    entityFactory.createGround(0, -arenaSize, arenaSize * 2, wallThickness, Color.GRAY);

    // Bottom wall
    entityFactory.createGround(0, arenaSize, arenaSize * 2, wallThickness, Color.GRAY);

    // Left wall
    entityFactory.createGround(-arenaSize, 0, wallThickness, arenaSize * 2, Color.GRAY);

    // Right wall
    entityFactory.createGround(arenaSize, 0, wallThickness, arenaSize * 2, Color.GRAY);
  }

  private void createUI() {
    // Master volume slider
    masterVolumeSlider = (Slider) engine.getUiSystem()
        .createSlider("Master Volume", 120f, 40f, 150f, 20f, 0f, 100f, 100f)
        .get(UIComponent.class).getUi();

    // Set up slider listener
    masterVolumeSlider.setOnValueChanged(value -> {
      float volume = value / 100.0f;
      engine.setMasterVolume(volume);
    });
  }

  private void setupInputHandlers() {
    // WASD movement keys
    inputManager.addKeyListener(e -> {
      if (!audioInitialized)
        return false;

      Body body = player.get(PhysicsBodyComponent.class).getBody();
      float force = 100.0f;

      switch (e.getKeyCode()) {
        case KeyEvent.VK_W:
          body.applyForce(new Vec2(0, -force), new Vec2());
          return true;
        case KeyEvent.VK_S:
          body.applyForce(new Vec2(0, force), new Vec2());
          return true;
        case KeyEvent.VK_A:
          body.applyForce(new Vec2(-force, 0), new Vec2());
          return true;
        case KeyEvent.VK_D:
          body.applyForce(new Vec2(force, 0), new Vec2());
          return true;
      }
      return false;
    });

  }

  private void flashEntity(Entity entity) {
    if (entity != null && entity.has(RenderableComponent.class)) {
      Renderable render = entity.get(RenderableComponent.class).getR();

      // Store original color
      Color originalColor = null;
      if (render instanceof Rect) {
        originalColor = ((Rect) render).getColor();
        ((Rect) render).setColor(Color.WHITE);
      }

      // Schedule a task to reset the color after a brief flash
      if (originalColor != null) {
        final Color finalColor = originalColor;
        engine.scheduleTask(() -> {
          if (render instanceof Rect) {
            ((Rect) render).setColor(finalColor);
          }
        });
      }
    }
  }

  private void setupEventListeners() {
    // Listen for volume changes
    eventSystem.addEventListener(EventTypes.AUDIO_VOLUME_CHANGED, event -> {
      Float volume = (Float) event.getData("volume");
      if (volume != null) {
        masterVolumeSlider.setValue((int) (volume * 100));
      }
    });
  }

  private boolean muted = false;
  private boolean wasKey0Pressed = false; // Track previous key state

  @Override
  public void update(double deltaTime) {
    super.update(deltaTime);
    if (inputManager.isMouseButtonPressed(1)) {
      // Convert screen to world coordinates
      float[] worldPos = engine.getCameraSystem().screenToWorld((float) inputManager.getMousePosition().getX(),
          (float) inputManager.getMousePosition().getY());
      float worldX = worldPos[0];
      float worldY = worldPos[1];

      // Check if clicked on the bell source rectangle
      Transform transform = bellSource.get(Transform.class);
      RenderableComponent renderComp = bellSource.get(RenderableComponent.class);

      if (renderComp != null && renderComp.getR() instanceof Rect) {
        Rect rect = (Rect) renderComp.getR();
        float width = (float) rect.getWidth();
        float height = (float) rect.getHeight();

        // Check if the click is within the rectangle bounds
        if (Math.abs(worldX - transform.getX()) <= width / 2 &&
            Math.abs(worldY - transform.getY()) <= height / 2) {

          // Play the bell sound
          engine.playSound(bellSource, BELL_ID, false);

          // Visual feedback - flash the rectangle
          flashEntity(bellSource);
        }
      }
    }

    // Only trigger on the key press transition (not pressed -> pressed)
    boolean isKey0Pressed = inputManager.isKeyPressed(KeyEvent.VK_0);
    if (isKey0Pressed && !wasKey0Pressed) {
      muted = !muted;

      System.out.println("Key Pressed : Muted = " + muted);
      engine.setAudioMuted(muted);
    }
    wasKey0Pressed = isKey0Pressed;
  }

  @Override
  public void onDeactivate() {
    super.onDeactivate();
    // Stop all sounds when leaving the scene
    engine.setAudioMuted(false);
  }
}
