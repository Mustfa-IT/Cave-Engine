package com.engine.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.engine.components.Transform;
import com.engine.components.UIComponent;
import com.engine.core.GameWindow;
import com.engine.entity.EntityRegistrar;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * System for creating and managing UI elements.
 * Acts as a factory for UI elements and handles their events.
 */
public class UISystem {
  private final GameWindow window;
  private final Dominion ecs;
  private EntityRegistrar currentRegistrar;
  private UIElement hoveredElement;
  private UIElement focusedElement;

  public UISystem(GameWindow window, Dominion ecs) {
    this.window = window;
    this.ecs = ecs;
    setupEventHandling();
  }

  /**
   * Set the current entity registrar
   */
  public void setCurrentRegistrar(EntityRegistrar registrar) {
    this.currentRegistrar = registrar;
  }

  private void setupEventHandling() {
    MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        handleMouseClick(e.getX(), e.getY());
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        handleMouseMove(e.getX(), e.getY());
      }

      @Override
      public void mousePressed(MouseEvent e) {
        handleMousePress(e.getX(), e.getY());
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        handleMouseRelease(e.getX(), e.getY());
      }
    };

    window.addMouseListener(mouseAdapter);
    window.addMouseMotionListener(mouseAdapter);
  }

  /**
   * Register created UI entity with current registrar
   */
  private Entity registerWithRegistrar(Entity entity) {
    if (currentRegistrar != null && entity != null) {
      return currentRegistrar.registerEntity(entity);
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
    }
  }

  /**
   * Handle mouse movement to update hover states
   */
  private void handleMouseMove(int x, int y) {
    // Reset previous hover state
    if (hoveredElement instanceof Button) {
      ((Button) hoveredElement).setHovered(false);
    }

    // Find element under cursor
    hoveredElement = findElementAt(x, y);

    // Update hover state
    if (hoveredElement instanceof Button) {
      ((Button) hoveredElement).setHovered(true);
    }
  }

  /**
   * Handle mouse click events
   */
  private void handleMouseClick(int x, int y) {
    UIElement clickedElement = findElementAt(x, y);

    if (clickedElement instanceof Button) {
      ((Button) clickedElement).click();
    }

    focusedElement = clickedElement;
  }

  /**
   * Handle mouse press events
   */
  private void handleMousePress(int x, int y) {
    // Could be used for drag operations or visual feedback
  }

  /**
   * Handle mouse release events
   */
  private void handleMouseRelease(int x, int y) {
    // Could be used for drag operations
  }

  /**
   * Find the UI element at the specified screen coordinates
   */
  private UIElement findElementAt(int x, int y) {
    UIElement result = null;

    // Instead of looping through a local list, query all entities with UIComponent
    var entities = ecs.findEntitiesWith(UIComponent.class);

    // Check each UI component to see if it contains the point
    for (var entityResult : entities) {
      UIComponent uiComponent = entityResult.comp();

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
