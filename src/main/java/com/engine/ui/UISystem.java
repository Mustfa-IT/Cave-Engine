package com.engine.ui;

import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.inject.Inject;

import java.util.function.Consumer;

import com.engine.components.Transform;
import com.engine.components.UIComponent;
import com.engine.core.GameFrame;
import com.engine.editor.Editor;
import com.engine.entity.EntityRegistrar;
import com.engine.events.EventSystem;
import com.engine.events.EventTypes;
import com.engine.events.GameEventListener;
import com.engine.input.InputManager;
import com.engine.input.InputManager.Priority;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * System for creating and managing UI elements.
 * Acts as a factory for UI elements and handles their events.
 *
 * <p>
 * This class provides methods to create various UI components such as buttons,
 * labels, panels, sliders, and debug overlays. It also handles mouse input
 * events
 * for these components and manages UI element states such as hover, focus, and
 * drag.
 * </p>
 *
 * <p>
 * Usage example:
 * </p>
 *
 * <pre>
 * UISystem uiSystem = new UISystem(gameWindow, ecsInstance);
 * Entity button = uiSystem.createButton("Click Me", 100, 100, 200, 50);
 * </pre>
 */
public class UISystem {
  private static final Logger LOGGER = Logger.getLogger(UISystem.class.getName());
  private final GameFrame window;
  private final Dominion ecs;
  private final EventSystem eventSystem;
  private EntityRegistrar currentRegistrar;
  private UIElement hoveredElement;
  private UIElement focusedElement;
  private UIElement draggedElement;
  private InputManager inputManager;
  private boolean initialized = false;

  // Editor integration
  private Editor editor;
  private boolean editorActive = false;

  @Inject
  public UISystem(GameFrame window, Dominion ecs, EventSystem eventSystem) {
    this.window = window;
    this.ecs = ecs;
    this.eventSystem = eventSystem;
    // We no longer set up event handling here - it will be done when
    // registerWithInputManager is called
  }

  /**
   * Set the editor reference for integration
   */
  public void setEditor(Editor editor) {
    this.editor = editor;
    if (editor != null) {
      editor.setUISystem(this);
      LOGGER.info("UISystem integrated with Editor");
    }
  }

  /**
   * Update UI system with editor state
   */
  public void setEditorActive(boolean active) {
    this.editorActive = active;
  }

  /**
   * Register this UI system with the input manager for event handling.
   * This must be called before the UI system will respond to input.
   */
  public void registerWithInputManager(InputManager inputManager) {
    if (initialized) {
      return;
    }

    this.inputManager = inputManager;

    // Register mouse event handlers with LOW priority (will run after Editor)
    // This ensures UI elements only receive events if the Editor doesn't consume
    // them
    inputManager.addMouseListener(this::handleMouseEvent, Priority.LOW);

    initialized = true;
    LOGGER.info("UISystem registered with InputManager");
  }

  /**
   * Central handler for all mouse events
   */
  private boolean handleMouseEvent(MouseEvent e) {
    // Skip if editor is active and not allowing UI interaction
    if (editorActive && editor != null && !editor.isUIInteractionAllowed()) {
      return false;
    }

    int x = e.getX();
    int y = e.getY();

    switch (e.getID()) {
      case MouseEvent.MOUSE_CLICKED:
        return handleMouseClick(x, y);
      case MouseEvent.MOUSE_PRESSED:
        return handleMousePress(x, y);
      case MouseEvent.MOUSE_RELEASED:
        return handleMouseRelease(x, y);
      case MouseEvent.MOUSE_MOVED:
        return handleMouseMove(x, y);
      case MouseEvent.MOUSE_DRAGGED:
        return handleMouseDrag(x, y);
    }
    return false;
  }

  /**
   * Set the current entity registrar
   */
  public void setCurrentRegistrar(EntityRegistrar registrar) {
    this.currentRegistrar = registrar;
  }

  /**
   * Register created UI entity with current registrar
   */
  private Entity registerWithRegistrar(Entity entity) {
    if (entity == null) {
      LOGGER.warning("Attempted to register null entity with registrar");
      return null;
    }
    if (currentRegistrar != null) {
      try {
        Entity registered = currentRegistrar.registerEntity(entity);
        LOGGER.fine("UI entity registered with scene: " + entity);
        return registered;
      } catch (Exception e) {
        LOGGER.warning("Failed to register entity with registrar: " + e.getMessage());
      }
    } else {
      LOGGER.fine("No current registrar available for UI entity: " + entity);
    }
    return entity;
  }

  /**
   * Factory method to create a button
   */
  public Entity createButton(String text, float x, float y, float width, float height) {
    Button button = new Button(text, x, y, width, height);
    Entity entity = ecs.createEntity(
        "ui_button_" + text,
        new UIComponent(button),
        new Transform(x, y, 0, 1, 1));
    return registerWithRegistrar(entity);
  }

  /**
   * Factory method to create a label
   */
  public Entity createLabel(String text, float x, float y) {
    Label label = new Label(text, x, y);
    Entity entity = ecs.createEntity(
        "ui_label_" + text,
        new UIComponent(label),
        new Transform(x, y, 0, 1, 1));
    return registerWithRegistrar(entity);
  }

  /**
   * Factory method to create a panel
   */
  public Entity createPanel(float x, float y, float width, float height) {
    Panel panel = new Panel(x, y, width, height);
    Entity entity = ecs.createEntity(
        "ui_panel",
        new UIComponent(panel),
        new Transform(x, y, 0, 1, 1));
    return registerWithRegistrar(entity);
  }

  /**
   * Factory method to create a slider
   */
  public Entity createSlider(String label, float x, float y, float width, float height,
      float minValue, float maxValue, float initialValue) {
    Slider slider = new Slider(label, x, y, width, height, minValue, maxValue, initialValue);
    Entity entity = ecs.createEntity(
        "ui_slider_" + label,
        new UIComponent(slider),
        new Transform(x, y, 0, 1, 1));
    return registerWithRegistrar(entity);
  }

  /**
   * Set a callback for when a slider's value changes.
   *
   * @param sliderEntity The entity containing the slider UI component
   * @param callback     A consumer function that will be called with the new
   *                     value when the slider changes
   * @throws NullPointerException if the callback is null
   */
  public void setSliderCallback(Entity sliderEntity, Consumer<Float> callback) {
    if (sliderEntity == null)
      return;

    if (callback == null) {
      throw new NullPointerException("Callback cannot be null");
    }

    UIComponent uiComp = sliderEntity.get(UIComponent.class);
    if (uiComp != null && uiComp.getUi() instanceof Slider) {
      Slider slider = (Slider) uiComp.getUi();

      // Wrap the callback to also fire an event
      slider.setOnValueChanged(value -> {
        // Fire value changed event
        eventSystem.fireEvent(EventTypes.UI_VALUE_CHANGED,
            "element", slider,
            "entity", sliderEntity,
            "value", value);

        // Call the original callback
        callback.accept(value);
      });
    }
  }

  @SuppressWarnings("null")
  public void removeSliderCallBacks(Entity sliderEntity) {
    if (sliderEntity == null)
      return;
    UIComponent uiComp = sliderEntity.get(UIComponent.class);
    if (uiComp == null)
      throw new NullPointerException("UIComponent for the Slider can't be found ");

    Slider slider = (Slider) uiComp.getUi();
    slider.removeSliderCallBacks();
  }

  @SuppressWarnings("null")
  public void removeSliderCallBack(Entity sliderEntity, Consumer<Float> callBack) {
    if (sliderEntity == null)
      return;
    UIComponent uiComp = sliderEntity.get(UIComponent.class);
    if (uiComp == null)
      throw new NullPointerException("UIComponent for the Slider can't be found ");
    Slider slider = (Slider) uiComp.getUi();
    slider.removeSliderCallBack(callBack);
  }

  /**
   * Factory method to create a debug overlay for displaying performance metrics
   * and debug information.
   *
   * @param x The x-coordinate of the top-left corner of the overlay
   * @param y The y-coordinate of the top-left corner of the overlay
   * @return An entity containing the debug overlay UI component, or null if
   *         registration fails
   * @see DebugOverlay
   */
  public Entity createDebugOverlay(float x, float y) {
    int overlayWidth = (int) (window.getWidth() * 0.2); // 20% of the window width
    int overlayHeight = (int) (window.getHeight() * 0.20); // 15% of the window height
    DebugOverlay overlay = new DebugOverlay(x, y, overlayWidth, overlayHeight);
    Entity entity = ecs.createEntity(
        "ui_debug_overlay",
        new UIComponent(overlay),
        new Transform(x, y, 0, 1, 1));
    return registerWithRegistrar(entity);
  }

  /**
   * Add a UI element to an existing panel
   */
  public void addToPanel(Entity panelEntity, Entity elementEntity) {
    if (panelEntity == null || elementEntity == null) {
      return;
    }

    UIComponent panelComp = panelEntity.get(UIComponent.class);
    UIComponent elementComp = elementEntity.get(UIComponent.class);

    if (panelComp == null || elementComp == null) {
      return;
    }

    UIElement panelElement = panelComp.getUi();
    UIElement childElement = elementComp.getUi();

    if (panelElement instanceof Panel && childElement != null) {
      ((Panel) panelElement).addElement(childElement);

      // Fire element added event
      eventSystem.fireEvent(EventTypes.UI_ELEMENT_ADDED,
          "panel", panelElement,
          "element", childElement,
          "panelEntity", panelEntity,
          "elementEntity", elementEntity);
    }
  }

  /**
   * Add a listener for a specific UI event type
   *
   * @param eventType The UI event type to listen for (from EventTypes)
   * @param listener  The listener to be called when event is fired
   */
  public void addUIEventListener(String eventType, GameEventListener listener) {
    if (!eventType.startsWith("ui:")) {
      LOGGER.warning("Attempted to register non-UI event type: " + eventType);
      return;
    }
    eventSystem.addEventListener(eventType, listener);
  }

  /**
   * Add a listener for all UI events using pattern matching
   *
   * @param listener The listener to be called when any UI event is fired
   */
  public void addAllUIEventsListener(GameEventListener listener) {
    eventSystem.addPatternListener("ui:*", listener);
  }

  /**
   * Handle mouse movement to update hover states
   */
  private boolean handleMouseMove(int x, int y) {
    // Reset previous hover state if needed
    if (hoveredElement instanceof Button) {
      ((Button) hoveredElement).setHovered(false);

      // Fire hover end event
      eventSystem.fireEvent(EventTypes.UI_HOVER_END,
          "element", hoveredElement,
          "x", x,
          "y", y);
    }

    // Find element under cursor
    hoveredElement = findElementAt(x, y);

    // If no UI element is under cursor, don't consume the event
    if (hoveredElement == null) {
      return false;
    }

    // Update hover state
    if (hoveredElement instanceof Button) {
      ((Button) hoveredElement).setHovered(true);

      // Fire hover begin event
      eventSystem.fireEvent(EventTypes.UI_HOVER_BEGIN,
          "element", hoveredElement,
          "x", x,
          "y", y);
    }

    // Only consume the event if we found an interactive element
    return (hoveredElement instanceof Button) || (hoveredElement instanceof Slider);
  }

  /**
   * Handle mouse click events
   */
  private boolean handleMouseClick(int x, int y) {
    UIElement clickedElement = findElementAt(x, y);

    if (clickedElement == null) {
      // If we had a focused element before, fire a focus lost event
      if (focusedElement != null) {
        eventSystem.fireEvent(EventTypes.UI_FOCUS_LOST,
            "element", focusedElement,
            "x", x,
            "y", y);
        focusedElement = null;
      }
      return false;
    }

    // If focusing on a different element than before
    if (focusedElement != clickedElement) {
      // Fire focus lost on previous element if it exists
      if (focusedElement != null) {
        eventSystem.fireEvent(EventTypes.UI_FOCUS_LOST,
            "element", focusedElement,
            "x", x,
            "y", y);
      }

      // Set new focus and fire focus gained event
      focusedElement = clickedElement;
      eventSystem.fireEvent(EventTypes.UI_FOCUS_GAINED,
          "element", clickedElement,
          "x", x,
          "y", y);
    }

    if (clickedElement instanceof Button) {
      ((Button) clickedElement).click();

      // Fire click event
      eventSystem.fireEvent(EventTypes.UI_CLICK,
          "element", clickedElement,
          "x", x,
          "y", y);

      return true; // Consume the event
    }

    return clickedElement instanceof Slider; // Only consume if it's an interactive element
  }

  /**
   * Handle mouse press events
   */
  private boolean handleMousePress(int x, int y) {
    UIElement clickedElement = findElementAt(x, y);

    if (clickedElement == null) {
      return false;
    }

    if (clickedElement instanceof Slider) {
      if (((Slider) clickedElement).startDrag(x, y)) {
        draggedElement = clickedElement;

        // Fire drag begin event
        eventSystem.fireEvent(EventTypes.UI_DRAG_BEGIN,
            "element", clickedElement,
            "x", x,
            "y", y);

        return true; // Consume the event
      }
    }

    return clickedElement instanceof Slider || clickedElement instanceof Button;
  }

  /**
   * Handle mouse release events
   */
  private boolean handleMouseRelease(int x, int y) {
    boolean hadDraggedElement = draggedElement != null;

    if (draggedElement instanceof Slider) {
      ((Slider) draggedElement).stopDrag();

      // Fire drag end event
      eventSystem.fireEvent(EventTypes.UI_DRAG_END,
          "element", draggedElement,
          "x", x,
          "y", y);

      draggedElement = null;
      return true; // Consume the event
    }

    draggedElement = null;
    return hadDraggedElement;
  }

  /**
   * Handle mouse drag events
   */
  private boolean handleMouseDrag(int x, int y) {
    if (draggedElement instanceof Slider) {
      ((Slider) draggedElement).handleMouseDrag(x, y);

      // Fire drag update event
      eventSystem.fireEvent(EventTypes.UI_DRAG_UPDATE,
          "element", draggedElement,
          "x", x,
          "y", y);

      return true; // Consume the event
    }

    // If we're not dragging a slider, check if we're hovering over any interactive
    // element
    UIElement elementUnderCursor = findElementAt(x, y);
    return elementUnderCursor instanceof Slider || elementUnderCursor instanceof Button;
  }

  /**
   * Find the UI element at the specified screen coordinates
   * Modified to respect visibility of UI components
   */
  private UIElement findElementAt(int x, int y) {
    UIElement result = null;

    // Query all entities with UIComponent - use toArray() for stable enumeration
    var entities = ecs.findEntitiesWith(UIComponent.class).stream().toList();

    // Check each UI component to see if it contains the point
    // Process in reverse order (last drawn = on top)
    for (int i = entities.size() - 1; i >= 0; i--) {
      var entityResult = entities.get(i);
      UIComponent uiComponent = entityResult.comp();

      // Only check visible components
      if (uiComponent != null && uiComponent.isVisible() &&
          uiComponent.contains(x, y)) {
        result = uiComponent.getUi();
        break;
      }
    }
    return result;
  }

  /**
   * Update all UI elements
   */
  public void update(double deltaTime) {
    ecs.findEntitiesWith(UIComponent.class).forEach(result -> {
      UIComponent uiComponent = result.comp();
      if (uiComponent.getUi() instanceof AbstractUIElement) {
        ((AbstractUIElement) uiComponent.getUi()).update(deltaTime);
      }
    });
  }
}
