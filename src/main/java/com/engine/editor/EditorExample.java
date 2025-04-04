package com.engine.editor;

import java.awt.event.KeyEvent;
import com.engine.core.GameEngine;

public class EditorExample {

  public static void setupEditorSystem(GameEngine game, Editor editor) {
    // Register the editor's input handlers with the input manager
    editor.registerInputHandlers(game.getInputManager());

    // Set up toggle key (F1 to show/hide editor)
    game.getInputManager().onKeyPress(KeyEvent.VK_F1, e -> {
      editor.toggleActive();
      return true; // Consume the event
    });

    // Setup shortcuts for editor functionality
    game.getInputManager().onKeyPress(KeyEvent.VK_F2, e -> {
      if (editor.isActive()) {
        editor.toggleGrid();
        return true; // Consume the event
      }
      return false;
    });

    game.getInputManager().onKeyPress(KeyEvent.VK_F3, e -> {
      if (editor.isActive()) {
        editor.toggleSnapping();
        return true; // Consume the event
      }
      return false;
    });

    game.getInputManager().onKeyPress(KeyEvent.VK_F4, e -> {
      if (editor.isActive()) {
        editor.cycleTheme();
        return true; // Consume the event
      }
      return false;
    });

    // Layout save/load shortcuts
    game.getInputManager().onKeyPress(KeyEvent.VK_S, e -> {
      if (editor.isActive() && e.isControlDown()) {
        editor.saveLayout("default");
        return true; // Consume the event
      }
      return false;
    });

    game.getInputManager().onKeyPress(KeyEvent.VK_L, e -> {
      if (editor.isActive() && e.isControlDown()) {
        editor.loadLayout("default");
        return true; // Consume the event
      }
      return false;
    });

    // Create property panel with editable properties
    PropertyPanel propertiesPanel = new PropertyPanel(10, 10, 200, 300, "Properties");
    propertiesPanel.setProperty("Position X", 100);
    propertiesPanel.setProperty("Position Y", 100);
    propertiesPanel.setProperty("Rotation", 45.0f);
    propertiesPanel.setProperty("Scale", 1.0f);

    // Add property change listeners
    propertiesPanel.setPropertyChangeListener("Position X", (name, oldVal, newVal) -> {
      System.out.println("Position X changed: " + oldVal + " -> " + newVal);
      // Here you could update the selected object's position
    });

    // Set up keyboard input for property editing
    game.addEditorKeyListener(e -> {
      if (propertiesPanel.isEditing()) {
        propertiesPanel.handleKeyTyped(e.getKeyChar());
      }
    });

    editor.addElement(propertiesPanel);

    // Create scene hierarchy panel
    EditorPanel hierarchyPanel = editor.createPanel(10, 320, 200, 250, "Hierarchy");

    // Create inspector panel
    EditorPanel inspectorPanel = editor.createPanel(
        game.getGameFrame().getWidth() - 210,
        10,
        200,
        300,
        "Inspector");

    // Create console panel at bottom of screen
    EditorPanel consolePanel = editor.createPanel(
        10,
        game.getGameFrame().getHeight() - 210,
        game.getGameFrame().getWidth() - 20,
        200,
        "Console");

    // Create a nested panel to demonstrate panel hierarchy
    EditorPanel nestedPanel = new EditorPanel(10, 30, 180, 100, "Nested Panel");
    inspectorPanel.addChild(nestedPanel);

    // Register editor's render method to be called each frame
    game.addOverlayRenderer(editor::render);

    // Schedule editor updates
    game.scheduleTask(() -> editor.update(game.getDeltaTime()));

    System.out.println("Editor setup complete. Press F1 to toggle editor.");
    System.out.println("F2: Toggle Grid, F3: Toggle Snapping, F4: Toggle Theme");
    System.out.println("Ctrl+S: Save Layout, Ctrl+L: Load Layout");
  }

  /**
   * Creates a helper method to be called from Main to set up the editor
   */
  public static void integrateWithGame(GameEngine game) {
    // Retrieve editor instance from DI
    Editor editor = game.getInjector().editor();

    // Set up the editor with panels and key bindings
    setupEditorSystem(game, editor);
  }
}
