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

  // Key denoising
  private boolean keyDenoisingEnabled = true;
  private long keyDebounceTime = 50; // milliseconds to wait before accepting another key event
  private final Map<Integer, Long> lastKeyEventTimes = new HashMap<>();
  private final Map<Integer, Boolean> rawKeyStates = new HashMap<>();

  // Mouse position in screen coordinates
  private Point mouseScreenPosition = new Point(0, 0);

  // Mouse position denoising
  private boolean denoisingEnabled = true;
  private float denoisingStrength = 1f; // 0.0 to 1.0, higher means more smoothing
  private int denoisingBufferSize = 5;
  private final List<Point> mousePositionHistory = new ArrayList<>();
  private Point rawMouseScreenPosition = new Point(0, 0);

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
        int keyCode = e.getKeyCode();
        rawKeyStates.put(keyCode, true);

        // Apply key denoising if enabled
        if (keyDenoisingEnabled) {
          if (!applyKeyDenoising(keyCode, true)) {
            return; // Event was filtered out by denoising
          }
        } else {
          keyStates.put(keyCode, true);
        }

        // First check if any custom listeners want to consume this event
        if (processKeyEvent(e, keyListeners)) {
          return;
        }

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
        int keyCode = e.getKeyCode();
        rawKeyStates.put(keyCode, false);

        // Apply key denoising if enabled
        if (keyDenoisingEnabled) {
          if (!applyKeyDenoising(keyCode, false)) {
            return; // Event was filtered out by denoising
          }
        } else {
          keyStates.put(keyCode, false);
        }

        // First check if any custom listeners want to consume this event
        if (processKeyEvent(e, keyListeners)) {
          return;
        }

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
        rawMouseScreenPosition.setLocation(e.getX(), e.getY());

        // Apply denoising if enabled
        if (denoisingEnabled) {
          applyDenoising(rawMouseScreenPosition);
        } else {
          mouseScreenPosition.setLocation(rawMouseScreenPosition);
        }

        // Process through all priority levels (HIGH to LOW)
        boolean consumed = processMouseEventByPriority(e);

        if (!consumed && mouseMoveCallback != null) {
          mouseMoveCallback.accept(mouseScreenPosition);
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        rawMouseScreenPosition.setLocation(e.getX(), e.getY());

        // Apply denoising if enabled
        if (denoisingEnabled) {
          applyDenoising(rawMouseScreenPosition);
        } else {
          mouseScreenPosition.setLocation(rawMouseScreenPosition);
        }

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
   * Apply denoising to key input
   *
   * @param keyCode   The key code from the input event
   * @param isPressed Whether the key is pressed or released
   * @return true if event should be processed, false if filtered out
   */
  private boolean applyKeyDenoising(int keyCode, boolean isPressed) {
    long currentTime = System.currentTimeMillis();
    long lastEventTime = lastKeyEventTimes.getOrDefault(keyCode, 0L);

    // Check if enough time has passed since last event for this key
    if (currentTime - lastEventTime < keyDebounceTime) {
      LOGGER.finest("Filtered key event due to debounce: " + keyCode);
      return false;
    }

    // Update last event time for this key
    lastKeyEventTimes.put(keyCode, currentTime);

    // Check if state has actually changed
    Boolean currentState = keyStates.getOrDefault(keyCode, false);
    if (currentState == isPressed) {
      // State hasn't changed, filter out redundant event
      return false;
    }

    // Update the denoised state
    keyStates.put(keyCode, isPressed);
    return true;
  }

  /**
   * Enable or disable key denoising
   *
   * @param enabled True to enable denoising, false to disable
   * @return this InputManager for method chaining
   */
  public InputManager setKeyDenoisingEnabled(boolean enabled) {
    this.keyDenoisingEnabled = enabled;
    if (!enabled) {
      lastKeyEventTimes.clear();
      // Sync key states with raw states
      for (Map.Entry<Integer, Boolean> entry : rawKeyStates.entrySet()) {
        keyStates.put(entry.getKey(), entry.getValue());
      }
    }
    return this;
  }

  /**
   * Set the debounce time for key denoising
   *
   * @param milliseconds Time to wait before accepting another key event
   * @return this InputManager for method chaining
   */
  public InputManager setKeyDebounceTime(long milliseconds) {
    this.keyDebounceTime = Math.max(0, milliseconds);
    return this;
  }

  /**
   * Get the raw (unfiltered) key state
   *
   * @param keyCode KeyEvent code to check
   * @return true if key is pressed in raw state, false otherwise
   */
  public boolean isRawKeyPressed(int keyCode) {
    return rawKeyStates.getOrDefault(keyCode, false);
  }

  /**
   * Apply denoising to mouse input
   *
   * @param rawPosition The raw mouse position from the input event
   */
  private void applyDenoising(Point rawPosition) {
    // Add the new position to the history
    mousePositionHistory.add(new Point(rawPosition));

    // Limit history size
    while (mousePositionHistory.size() > denoisingBufferSize) {
      mousePositionHistory.remove(0);
    }

    // Not enough data points for smoothing yet
    if (mousePositionHistory.size() < 2) {
      mouseScreenPosition.setLocation(rawPosition);
      return;
    }

    // Weighted moving average - newer positions have more weight
    double totalWeight = 0;
    double weightedX = 0;
    double weightedY = 0;

    // Calculate weighted values
    for (int i = 0; i < mousePositionHistory.size(); i++) {
      // Weight increases with index (more recent = higher weight)
      double weight = i + 1;
      Point p = mousePositionHistory.get(i);

      weightedX += p.x * weight;
      weightedY += p.y * weight;
      totalWeight += weight;
    }

    // Calculate the smoothed position
    int smoothedX = (int) (weightedX / totalWeight);
    int smoothedY = (int) (weightedY / totalWeight);

    // Interpolate between raw and smoothed position based on strength
    int finalX = (int) ((1 - denoisingStrength) * rawPosition.x + denoisingStrength * smoothedX);
    int finalY = (int) ((1 - denoisingStrength) * rawPosition.y + denoisingStrength * smoothedY);

    mouseScreenPosition.setLocation(finalX, finalY);
  }

  /**
   * Enable or disable input denoising
   *
   * @param enabled True to enable denoising, false to disable
   * @return this InputManager for method chaining
   */
  public InputManager setDenoisingEnabled(boolean enabled) {
    this.denoisingEnabled = enabled;
    if (!enabled) {
      mousePositionHistory.clear();
    }
    return this;
  }

  /**
   * Set the strength of the denoising filter
   *
   * @param strength Value from 0.0 (no smoothing) to 1.0 (maximum smoothing)
   * @return this InputManager for method chaining
   */
  public InputManager setDenoisingStrength(float strength) {
    this.denoisingStrength = Math.max(0.0f, Math.min(1.0f, strength));
    return this;
  }

  /**
   * Set the size of the denoising buffer (how many previous positions to track)
   *
   * @param size Number of positions to store in history (higher = smoother but
   *             more lag)
   * @return this InputManager for method chaining
   */
  public InputManager setDenoisingBufferSize(int size) {
    this.denoisingBufferSize = Math.max(2, size);
    return this;
  }

  /**
   * Get the raw (unfiltered) mouse position
   *
   * @return Point containing raw screen coordinates
   */
  public Point getRawMousePosition() {
    return new Point(rawMouseScreenPosition);
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
