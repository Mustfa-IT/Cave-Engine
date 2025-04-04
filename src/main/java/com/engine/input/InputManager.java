package com.engine.input;

import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.engine.core.CameraSystem;
import com.engine.core.GameFrame;

/**
 * Centralized input handling system for keyboard and mouse events
 */
public class InputManager {
  private static final Logger LOGGER = Logger.getLogger(InputManager.class.getName());

  private final GameFrame window;
  private final CameraSystem cameraSystem;

  // Track key states (pressed or not)
  private final Map<Integer, Boolean> keyStates = new HashMap<>();

  // Mouse position in screen coordinates
  private Point mouseScreenPosition = new Point(0, 0);

  // Track mouse button states
  private final Map<Integer, Boolean> mouseButtonStates = new HashMap<>();

  // Input event callbacks
  private final Map<Integer, Consumer<KeyEvent>> keyPressCallbacks = new HashMap<>();
  private final Map<Integer, Consumer<KeyEvent>> keyReleaseCallbacks = new HashMap<>();
  private final Map<Integer, Consumer<MouseEvent>> mouseButtonCallbacks = new HashMap<>();
  private Consumer<Point> mouseMoveCallback = null;

  // Custom key handlers
  private final List<Function<KeyEvent, Boolean>> keyListeners = new ArrayList<>();
  private final List<Function<KeyEvent, Boolean>> keyTypedListeners = new ArrayList<>();

  public InputManager(GameFrame window, CameraSystem cameraSystem) {
    this.window = window;
    this.cameraSystem = cameraSystem;
    setupListeners();
  }

  /**
   * Set up input event listeners
   */
  private void setupListeners() {
    // Keyboard listener
    KeyListener keyListener = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        // First check if any custom listeners want to consume this event
        if (processKeyEvent(e, keyListeners)) {
          return;
        }

        int keyCode = e.getKeyCode();
        keyStates.put(keyCode, true);

        // Handle any registered callbacks
        if (keyPressCallbacks.containsKey(keyCode)) {
          keyPressCallbacks.get(keyCode).accept(e);
        }
      }

      @Override
      public void keyTyped(KeyEvent e) {
        // Check if any custom listeners want to consume this event
        processKeyEvent(e, keyTypedListeners);
      }

      @Override
      public void keyReleased(KeyEvent e) {
        // First check if any custom listeners want to consume this event
        if (processKeyEvent(e, keyListeners)) {
          return;
        }

        int keyCode = e.getKeyCode();
        keyStates.put(keyCode, false);

        // Handle any registered callbacks
        if (keyReleaseCallbacks.containsKey(keyCode)) {
          keyReleaseCallbacks.get(keyCode).accept(e);
        }
      }
    };

    // Mouse motion listener
    MouseMotionAdapter mouseMotionAdapter = new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        mouseScreenPosition.setLocation(e.getX(), e.getY());

        if (mouseMoveCallback != null) {
          mouseMoveCallback.accept(mouseScreenPosition);
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        mouseScreenPosition.setLocation(e.getX(), e.getY());

        if (mouseMoveCallback != null) {
          mouseMoveCallback.accept(mouseScreenPosition);
        }
      }
    };

    // Mouse button listener
    MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        int button = e.getButton();
        mouseButtonStates.put(button, true);

        if (mouseButtonCallbacks.containsKey(button)) {
          mouseButtonCallbacks.get(button).accept(e);
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        int button = e.getButton();
        mouseButtonStates.put(button, false);
      }
    };

    // Register listeners with window
    window.addKeyListener(keyListener);
    window.addMouseMotionListener(mouseMotionAdapter);
    window.addMouseListener(mouseAdapter);

    LOGGER.info("Input manager initialized");
  }

  /**
   * Process key events through listeners, return true if consumed
   */
  private boolean processKeyEvent(KeyEvent e, List<Function<KeyEvent, Boolean>> listeners) {
    for (Function<KeyEvent, Boolean> listener : listeners) {
      if (listener.apply(e)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if a specific key is currently pressed
   *
   * @param keyCode KeyEvent code to check
   * @return true if key is pressed, false otherwise
   */
  public boolean isKeyPressed(int keyCode) {
    return keyStates.getOrDefault(keyCode, false);
  }

  /**
   * Check if a specific mouse button is currently pressed
   *
   * @param button MouseEvent button code to check
   * @return true if button is pressed, false otherwise
   */
  public boolean isMouseButtonPressed(int button) {
    return mouseButtonStates.getOrDefault(button, false);
  }

  /**
   * Get the current mouse position in screen coordinates
   *
   * @return Point containing screen coordinates
   */
  public Point getMousePosition() {
    return new Point(mouseScreenPosition);
  }

  /**
   * Get the current mouse position in world coordinates
   *
   * @return float[] containing world [x,y] coordinates
   */
  public float[] getMouseWorldPosition() {
    return cameraSystem.screenToWorld(mouseScreenPosition.x, mouseScreenPosition.y);
  }

  /**
   * Register a callback for when a key is pressed
   *
   * @param keyCode  KeyEvent code to listen for
   * @param callback Consumer to handle the key event
   * @return this InputManager for method chaining
   */
  public InputManager onKeyPress(int keyCode, Consumer<KeyEvent> callback) {
    keyPressCallbacks.put(keyCode, callback);
    return this;
  }

  /**
   * Register a callback for when a key is released
   *
   * @param keyCode  KeyEvent code to listen for
   * @param callback Consumer to handle the key event
   * @return this InputManager for method chaining
   */
  public InputManager onKeyRelease(int keyCode, Consumer<KeyEvent> callback) {
    keyReleaseCallbacks.put(keyCode, callback);
    return this;
  }

  /**
   * Register a callback for when a mouse button is pressed
   *
   * @param button   MouseEvent button code to listen for
   * @param callback Consumer to handle the mouse event
   * @return this InputManager for method chaining
   */
  public InputManager onMousePress(int button, Consumer<MouseEvent> callback) {
    mouseButtonCallbacks.put(button, callback);
    return this;
  }

  /**
   * Register a callback for when the mouse is moved
   *
   * @param callback Consumer to handle the mouse position
   * @return this InputManager for method chaining
   */
  public InputManager onMouseMove(Consumer<Point> callback) {
    this.mouseMoveCallback = callback;
    return this;
  }

  /**
   * Clear all input bindings
   */
  public void clearAllBindings() {
    keyPressCallbacks.clear();
    keyReleaseCallbacks.clear();
    mouseButtonCallbacks.clear();
    mouseMoveCallback = null;
  }

  /**
   * Add a general key event listener
   *
   * @param listener The listener that returns true if it consumed the event
   */
  public void addKeyListener(Function<KeyEvent, Boolean> listener) {
    if (listener != null) {
      keyListeners.add(listener);
    }
  }

  /**
   * Add a listener specifically for KEY_TYPED events
   *
   * @param listener The listener that returns true if it consumed the event
   */
  public void addKeyTypedListener(Function<KeyEvent, Boolean> listener) {
    if (listener != null) {
      keyTypedListeners.add(listener);
    }
  }
}
