package com.engine.editor;

import java.awt.event.KeyEvent;
import java.awt.Color;
import com.engine.core.GameEngine;

public class EditorDefault {

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

    // Enhanced layout save/load shortcuts
    game.getInputManager().onKeyPress(KeyEvent.VK_S, e -> {
      if (editor.isActive() && e.isControlDown()) {
        if (e.isShiftDown()) {
          // Show layout name dialog
          String name = javax.swing.JOptionPane.showInputDialog(
              game.getGameFrame(),
              "Enter layout name:",
              "Save Layout",
              javax.swing.JOptionPane.QUESTION_MESSAGE);

          if (name != null && !name.trim().isEmpty()) {
            editor.saveLayout(name.trim());
          }
        } else {
          // Quick save with default name
          editor.saveLayout("default");
        }
        return true; // Consume the event
      }
      return false;
    });

    game.getInputManager().onKeyPress(KeyEvent.VK_L, e -> {
      if (editor.isActive() && e.isControlDown()) {
        if (e.isShiftDown()) {
          // Show layout selection dialog
          String[] layouts = editor.listAvailableLayouts();
          if (layouts.length == 0) {
            javax.swing.JOptionPane.showMessageDialog(
                game.getGameFrame(),
                "No saved layouts found.",
                "Load Layout",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
          } else {
            String selected = (String) javax.swing.JOptionPane.showInputDialog(
                game.getGameFrame(),
                "Select a layout to load:",
                "Load Layout",
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                layouts,
                layouts[0]);

            if (selected != null) {
              editor.loadLayout(selected);
            }
          }
        } else {
          // Quick load default layout
          editor.loadLayout("default");
        }
        return true; // Consume the event
      }
      return false;
    });

    // Theme save/load shortcuts
    game.getInputManager().onKeyPress(KeyEvent.VK_T, e -> {
      if (editor.isActive() && e.isControlDown()) {
        if (e.isShiftDown()) {
          // Save theme dialog
          String name = javax.swing.JOptionPane.showInputDialog(
              game.getGameFrame(),
              "Enter theme name:",
              "Save Theme",
              javax.swing.JOptionPane.QUESTION_MESSAGE);

          if (name != null && !name.trim().isEmpty()) {
            editor.saveTheme(name.trim());
          }
        } else {
          // Load theme dialog
          String[] themes = editor.listAvailableThemes();
          if (themes.length == 0) {
            javax.swing.JOptionPane.showMessageDialog(
                game.getGameFrame(),
                "No saved themes found.",
                "Load Theme",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
          } else {
            String selected = (String) javax.swing.JOptionPane.showInputDialog(
                game.getGameFrame(),
                "Select a theme to load:",
                "Load Theme",
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                themes,
                themes[0]);

            if (selected != null) {
              editor.loadTheme(selected);
            }
          }
        }
        return true; // Consume the event
      }
      return false;
    });

    // Create an enhanced property panel with grouped properties and different
    // property types
    PropertyPanel propertiesPanel = new PropertyPanel(10, 10, 250, 400, "Properties");

    // Create property groups
    propertiesPanel.createGroup("Transform", true);
    propertiesPanel.createGroup("Appearance", true);
    propertiesPanel.createGroup("Physics", false); // Collapsed by default

    // Add transform properties
    propertiesPanel.setProperty("Position X", 100);
    propertiesPanel.setProperty("Position Y", 100);
    propertiesPanel.setProperty("Rotation", 45.0f);
    propertiesPanel.setProperty("Scale", 1.0f);
    propertiesPanel.setPropertyTooltip("Scale", "Object scale factor (1.0 = original size)");

    // Add appearance properties
    propertiesPanel.setProperty("Color", Color.BLUE);
    propertiesPanel.setProperty("Visible", true);
    propertiesPanel.setDropdownProperty("Style", "Normal", new String[] { "Normal", "Wireframe", "Textured" });
    propertiesPanel.setPropertyTooltip("Style", "Visual rendering style");

    // Add physics properties
    propertiesPanel.setProperty("Mass", 10.0f);
    propertiesPanel.setProperty("Friction", 0.5f);
    propertiesPanel.setProperty("Restitution", 0.3f);
    propertiesPanel.setProperty("Is Static", false);
    propertiesPanel.setPropertyTooltip("Restitution", "Bounciness factor (0-1)");

    // Organize properties into groups
    propertiesPanel.addPropertyToGroup("Transform", "Position X");
    propertiesPanel.addPropertyToGroup("Transform", "Position Y");
    propertiesPanel.addPropertyToGroup("Transform", "Rotation");
    propertiesPanel.addPropertyToGroup("Transform", "Scale");

    propertiesPanel.addPropertyToGroup("Appearance", "Color");
    propertiesPanel.addPropertyToGroup("Appearance", "Visible");
    propertiesPanel.addPropertyToGroup("Appearance", "Style");

    propertiesPanel.addPropertyToGroup("Physics", "Mass");
    propertiesPanel.addPropertyToGroup("Physics", "Friction");
    propertiesPanel.addPropertyToGroup("Physics", "Restitution");
    propertiesPanel.addPropertyToGroup("Physics", "Is Static");

    // Add property change listeners
    propertiesPanel.setPropertyChangeListener("Position X", (name, oldVal, newVal) -> {
      System.out.println("Position X changed: " + oldVal + " -> " + newVal);
      // Here you could update the selected object's position
    });

    propertiesPanel.setPropertyChangeListener("Color", (name, oldVal, newVal) -> {
      System.out.println("Color changed to: " + newVal);
      // Update the selected object's color
    });

    propertiesPanel.setPropertyChangeListener("Is Static", (name, oldVal, newVal) -> {
      System.out.println("Static property changed: " + oldVal + " -> " + newVal);
      // Update physics body type
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
    System.out.println("Ctrl+S: Quick Save Layout, Ctrl+Shift+S: Save Layout As");
    System.out.println("Ctrl+L: Quick Load Layout, Ctrl+Shift+L: Browse Layouts");
    System.out.println("Ctrl+T: Load Theme, Ctrl+Shift+T: Save Current Theme");
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
