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
  private final Map<Integer, Function<KeyEvent, Boolean>> keyPressCallbacks = new HashMap<>();
  private final Map<Integer, Function<KeyEvent, Boolean>> keyReleaseCallbacks = new HashMap<>();
  private final Map<Integer, Function<MouseEvent, Boolean>> mouseButtonCallbacks = new HashMap<>();
  private Consumer<Point> mouseMoveCallback = null;

  // Custom key handlers - divide listeners by priority
  private final List<Function<KeyEvent, Boolean>> keyListeners = new ArrayList<>();
  private final List<Function<KeyEvent, Boolean>> keyTypedListeners = new ArrayList<>();

  // Store mouse listeners in priority buckets (High, Normal, Low)
  private final Map<Priority, List<Function<MouseEvent, Boolean>>> mouseListenersByPriority = new HashMap<>();

  // Priority enum for mouse event handling
  public enum Priority {
    HIGH(0),
    NORMAL(1),
    LOW(2);

    private final int value;

    Priority(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public InputManager(GameFrame window, CameraSystem cameraSystem) {
    this.window = window;
    this.cameraSystem = cameraSystem;

    // Initialize priority buckets
    for (Priority priority : Priority.values()) {
      mouseListenersByPriority.put(priority, new ArrayList<>());
    }

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
          Function<KeyEvent, Boolean> callback = keyPressCallbacks.get(keyCode);
          if (callback.apply(e)) {
            // Event was consumed, stop processing
            return;
          }
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
          Function<KeyEvent, Boolean> callback = keyReleaseCallbacks.get(keyCode);
          callback.apply(e);
        }
      }
    };

    // Mouse motion listener
    MouseMotionAdapter mouseMotionAdapter = new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        mouseScreenPosition.setLocation(e.getX(), e.getY());

        // Process through all priority levels (HIGH to LOW)
        boolean consumed = processMouseEventByPriority(e);

        if (!consumed && mouseMoveCallback != null) {
          mouseMoveCallback.accept(mouseScreenPosition);
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        mouseScreenPosition.setLocation(e.getX(), e.getY());

        // Process through all priority levels (HIGH to LOW)
        boolean consumed = processMouseEventByPriority(e);

        if (!consumed && mouseMoveCallback != null) {
          mouseMoveCallback.accept(mouseScreenPosition);
        }
      }
    };

    // Mouse button listener
    MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        // Process through all priority levels (HIGH to LOW)
        boolean consumed = processMouseEventByPriority(e);

        if (!consumed) {
          int button = e.getButton();
          mouseButtonStates.put(button, true);

          if (mouseButtonCallbacks.containsKey(button)) {
            Function<MouseEvent, Boolean> callback = mouseButtonCallbacks.get(button);
            callback.apply(e);
          }
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        // Process through all priority levels (HIGH to LOW)
        boolean consumed = processMouseEventByPriority(e);

        if (!consumed) {
          int button = e.getButton();
          mouseButtonStates.put(button, false);
        }
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        // Process through registered mouse listeners respecting priority levels
        processMouseEventByPriority(e);
      }
    };

    // Register listeners with window
    window.addKeyListener(keyListener);
    window.addMouseMotionListener(mouseMotionAdapter);
    window.addMouseListener(mouseAdapter);

    LOGGER.info("Input manager initialized");
  }

  /**
   * Process mouse events through all priority levels
   *
   * @param e MouseEvent to process
   * @return true if the event was consumed by any listener
   */
  private boolean processMouseEventByPriority(MouseEvent e) {
    // Process listeners in priority order (HIGH to LOW)
    for (Priority priority : Priority.values()) {
      if (processMouseEvent(e, mouseListenersByPriority.get(priority))) {
        LOGGER.finest("Event consumed by priority level: " + priority);
        return true;
      }
    }
    return false;
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
   * Process mouse events through listeners, return true if consumed
   */
  private boolean processMouseEvent(MouseEvent e, List<Function<MouseEvent, Boolean>> listeners) {
    for (Function<MouseEvent, Boolean> listener : listeners) {
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
  public InputManager onKeyPress(int keyCode, Function<KeyEvent, Boolean> callback) {
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
  public InputManager onKeyRelease(int keyCode, Function<KeyEvent, Boolean> callback) {
    keyReleaseCallbacks.put(keyCode, callback);
    return this;
  }

  /**
   * Register a callback for when a mouse button is pressed
   *
   * @param button   MouseEvent button code to listen for
   * @param callback Function to handle the mouse event, returns true if consumed
   * @return this InputManager for method chaining
   */
  public InputManager onMousePress(int button, Function<MouseEvent, Boolean> callback) {
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

  /**
   * Add a general mouse event listener with specific priority
   *
   * @param listener The listener that returns true if it consumed the event
   * @param priority Priority level (HIGH, NORMAL, LOW)
   */
  public void addMouseListener(Function<MouseEvent, Boolean> listener, Priority priority) {
    if (listener != null) {
      mouseListenersByPriority.get(priority).add(listener);
      LOGGER.info("Added mouse listener with priority " + priority);
    }
  }

  /**
   * Add a mouse listener with normal priority
   */
  public void addMouseListener(Function<MouseEvent, Boolean> listener) {
    addMouseListener(listener, Priority.NORMAL);
  }

  /**
   * Add a mouse listener with high priority (legacy support)
   */
  public void addMouseListener(Function<MouseEvent, Boolean> listener, int priority) {
    // Convert old-style priority (higher numbers) to new enum
    if (priority > 0) {
      addMouseListener(listener, Priority.HIGH);
    } else {
      addMouseListener(listener, Priority.NORMAL);
    }
  }
}
