package com.engine.editor;

import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.engine.core.GameFrame;
import com.engine.core.GameWindow;
import com.engine.input.InputManager;

@Singleton
public class Editor {
  private static final Logger LOGGER = Logger.getLogger(Editor.class.getName());

  private final List<EditorElement> elements = new ArrayList<>();
  private final GameWindow gameWindow;
  private final GameFrame gameFrame;

  private boolean active = false;
  private EditorElement draggedElement = null;
  private int dragOffsetX, dragOffsetY;
  private boolean draggingGameFrame = false;
  private int frameDragStartX, frameDragStartY;

  // Panel resizing support
  private EditorPanel resizingPanel = null;

  // Grid & snapping
  private boolean showGrid = false;
  private boolean enableSnapping = true;
  private int gridSize = 20;

  // Theme support
  private EditorTheme currentTheme = EditorTheme.DEFAULT;

  // Layout management
  private static final String LAYOUTS_DIRECTORY = "editorLayouts/";

  @Inject
  public Editor(GameWindow gameWindow, GameFrame gameFrame) {
    this.gameWindow = gameWindow;
    this.gameFrame = gameFrame;
    setupEventHandlers();

    // Create layouts directory if it doesn't exist
    try {
      Files.createDirectories(Paths.get(LAYOUTS_DIRECTORY));
    } catch (Exception e) {
      LOGGER.warning("Failed to create layouts directory: " + e.getMessage());
    }

    LOGGER.info("Editor initialized");
  }

  // This method needs to be called from GameEngine after InputManager is
  // initialized
  public void registerInputHandlers(InputManager inputManager) {
    // Register mouse handlers with higher priority (1) than the default ones (0)
    inputManager.addMouseListener(this::handleMouseEvent, 1);
    LOGGER.info("Editor input handlers registered with high priority");
  }

  // Unified mouse event handler
  private boolean handleMouseEvent(MouseEvent e) {
    // Only handle events if editor is active
    if (!active) {
      return false;
    }

    // Process based on event type
    switch (e.getID()) {
      case MouseEvent.MOUSE_PRESSED:
        return handleMousePressed(e);
      case MouseEvent.MOUSE_RELEASED:
        return handleMouseReleased(e);
      case MouseEvent.MOUSE_DRAGGED:
        return handleMouseDragged(e);
      case MouseEvent.MOUSE_MOVED:
        return handleMouseMoved(e);
    }

    return false; // Don't consume other events
  }

  /**
   * Find the deepest editor element at the given position, respecting panel
   * hierarchy
   *
   * @param x        X coordinate
   * @param y        Y coordinate
   * @param elements List of elements to check
   * @return The found element or null
   */
  private EditorElement findElementAt(int x, int y, List<EditorElement> elements) {
    // Create a copy if this is the main elements list
    List<EditorElement> elementsCopy = new ArrayList<>(elements);

    // Check from top to bottom (last element drawn is on top)
    for (int i = elementsCopy.size() - 1; i >= 0; i--) {
      EditorElement element = elementsCopy.get(i);

      if (element.contains(x, y)) {
        // If this is a panel, check if any of its children contain the point
        if (element instanceof EditorPanel) {
          EditorPanel panel = (EditorPanel) element;
          EditorElement childElement = findElementAt(x, y, panel.getChildren());

          if (childElement != null) {
            // Found a child element that contains the point
            return childElement;
          }
        }
        return element;
      }
    }
    return null;
  }

  /**
   * Find a panel that has a resize area at the given position
   *
   * @param x        X coordinate
   * @param y        Y coordinate
   * @param elements List of elements to check
   * @return The panel with a resize area or null
   */
  private EditorPanel findPanelForResize(int x, int y, List<EditorElement> elements) {
    // Create a copy if this is the main elements list
    List<EditorElement> elementsCopy =  new ArrayList<>(elements);

    // Check from top to bottom (last element drawn is on top)
    for (int i = elementsCopy.size() - 1; i >= 0; i--) {
      EditorElement element = elementsCopy.get(i);

      if (element instanceof EditorPanel) {
        EditorPanel panel = (EditorPanel) element;

        // First check children recursively
        EditorPanel childPanel = findPanelForResize(x, y, panel.getChildren());
        if (childPanel != null) {
          return childPanel;
        }

        // Then check if this panel has a resize area at the mouse position
        if (panel.contains(x, y) && panel.isResizable() && !panel.isCollapsed()) {
          EditorPanel.ResizeDirection dir = panel.getResizeDirection(x, y);
          if (dir != EditorPanel.ResizeDirection.NONE) {
            return panel;
          }
        }
      }
    }
    return null;
  }

  private boolean handleMousePressed(MouseEvent e) {
    // Use the new helper method to find the element under the cursor
    EditorElement element = findElementAt(e.getX(), e.getY(), elements);

    if (element != null) {
      if (element instanceof EditorPanel) {
        EditorPanel panel = (EditorPanel) element;

        // Check for collapse button first
        if (panel.isCollapseButtonClicked(e.getX(), e.getY())) {
          panel.toggleCollapsed();
          return true;
        }

        // Check for header drag - MOVED EARLIER
        if (panel.isHeaderClicked(e.getX(), e.getY()) && panel.isDraggable()) {
          draggedElement = element;
          dragOffsetX = e.getX() - element.getX();
          dragOffsetY = e.getY() - element.getY();
          bringToFront(element);
          LOGGER.info("Started dragging panel: " + panel.getName() +
              " with offset " + dragOffsetX + "," + dragOffsetY);
          return true;
        }

        // Check for resize
        if (panel.isResizable() && !panel.isCollapsed()) {
          EditorPanel.ResizeDirection dir = panel.getResizeDirection(e.getX(), e.getY());
          if (dir != EditorPanel.ResizeDirection.NONE) {
            panel.setResizing(true);
            panel.setResizeDir(dir);
            resizingPanel = panel;
            gameFrame.setCursor(Cursor.getPredefinedCursor(
                panel.getCursorForResizeDirection(dir)));
            return true;
          }
        }

        // Check for property editing in PropertyPanel
        if (element instanceof PropertyPanel) {
          PropertyPanel propPanel = (PropertyPanel) element;
          if (propPanel.handlePropertyClick(e.getX(), e.getY())) {
            return true;
          }
        }
      } else if (element.isDraggable()) {
        draggedElement = element;
        dragOffsetX = e.getX() - element.getX();
        dragOffsetY = e.getY() - element.getY();
        bringToFront(element);
        return true;
      }
    }

    // If right-click and not on any element, we'll drag the game frame
    if (e.getButton() == MouseEvent.BUTTON3) {
      draggingGameFrame = true;
      frameDragStartX = e.getXOnScreen();
      frameDragStartY = e.getYOnScreen();
      return true;
    }

    return false;
  }

  private boolean handleMouseReleased(MouseEvent e) {
    boolean consumed = draggedElement != null || draggingGameFrame || resizingPanel != null;

    draggedElement = null;
    draggingGameFrame = false;

    if (resizingPanel != null) {
      resizingPanel.setResizing(false);
      resizingPanel = null;
      gameFrame.setCursor(Cursor.getDefaultCursor());
    }

    return consumed;
  }

  private boolean handleMouseDragged(MouseEvent e) {
    if (draggedElement != null) {
      int newX = e.getX() - dragOffsetX;
      int newY = e.getY() - dragOffsetY;

      // Apply snapping if enabled
      if (enableSnapping) {
        newX = snap(newX, gridSize);
        newY = snap(newY, gridSize);
      }

      draggedElement.setPosition(newX, newY);
      return true;
    } else if (resizingPanel != null) {
      resizingPanel.resize(e.getX(), e.getY());
      return true;
    } else if (draggingGameFrame) {
      // Fix: use getYOnScreen for Y coordinate calculation, not getXOnScreen
      int deltaX = e.getXOnScreen() - frameDragStartX;
      int deltaY = e.getYOnScreen() - frameDragStartY;

      // Get current location
      int newX = gameWindow.getLocation().x + deltaX;
      int newY = gameWindow.getLocation().y + deltaY;

      // Ensure window stays at least partially on screen
      if (newX > -gameWindow.getWidth() / 2 &&
          newX < java.awt.Toolkit.getDefaultToolkit().getScreenSize().width - 50 &&
          newY > 0 &&
          newY < java.awt.Toolkit.getDefaultToolkit().getScreenSize().height - 50) {
        gameWindow.setLocation(newX, newY);
      }

      // Update drag start positions
      frameDragStartX = e.getXOnScreen();
      frameDragStartY = e.getYOnScreen();
      return true;
    }

    return false;
  }

  private boolean handleMouseMoved(MouseEvent e) {
    // Check if we're over any panel's resize area
    EditorPanel resizePanel = findPanelForResize(e.getX(), e.getY(), elements);
    if (resizePanel != null) {
      EditorPanel.ResizeDirection dir = resizePanel.getResizeDirection(e.getX(), e.getY());
      int cursorType = resizePanel.getCursorForResizeDirection(dir);
      gameFrame.setCursor(Cursor.getPredefinedCursor(cursorType));
      return true;
    }

    // Reset cursor if not over a resizable area
    if (gameFrame.getCursor().getType() != Cursor.DEFAULT_CURSOR) {
      gameFrame.setCursor(Cursor.getDefaultCursor());
      return true;
    }

    return false;
  }

  // Remove direct event handlers since we're using InputManager for event
  // handling
  private void setupEventHandlers() {
    // Remove the duplicate event handlers
    // We're now using the InputManager-based event handling exclusively
    // through the registerInputHandlers() method
    LOGGER.info("Using InputManager for event handling instead of direct listeners");
  }

  /**
   * Snap value to nearest grid point
   */
  private int snap(int value, int gridSize) {
    return Math.round(value / (float) gridSize) * gridSize;
  }

  /**
   * Brings an element to the front by moving it to the end of the list
   */
  private void bringToFront(EditorElement element) {
    synchronized (elements) {
      if (elements.remove(element)) {
        elements.add(element);
      }
    }
  }

  public void addElement(EditorElement element) {
    synchronized (elements) {
      elements.add(element);
    }
  }

  public void removeElement(EditorElement element) {
    synchronized (elements) {
      elements.remove(element);
    }
  }

  public EditorElement getElementByName(String name) {
    synchronized (elements) {
      for (EditorElement element : elements) {
        if (element.getName().equals(name)) {
          return element;
        }
      }
    }
    return null;
  }

  public void render(Graphics2D g) {
    if (!active)
      return;

    // Draw grid if enabled
    if (showGrid) {
      drawGrid(g);
    }

    // Create a copy of the elements list to avoid ConcurrentModificationException
    List<EditorElement> elementsCopy;
    synchronized (elements) {
      elementsCopy = new ArrayList<>(elements);
    }

    // Draw panels from back to front using the copy
    for (EditorElement element : elementsCopy) {
      element.render(g);
    }
  }

  public void update(double deltaTime) {
    if (!active)
      return;

    // Create a copy of the elements list to avoid ConcurrentModificationException
    List<EditorElement> elementsCopy;
    synchronized (elements) {
      elementsCopy = new ArrayList<>(elements);
    }

    // Update all elements using the copy
    for (EditorElement element : elementsCopy) {
      element.update(deltaTime);
    }
  }

  /**
   * Draw alignment grid for panel positioning
   */
  private void drawGrid(Graphics2D g) {
    g.setColor(new java.awt.Color(80, 80, 80, 40));

    // Draw vertical lines
    for (int x = 0; x < gameFrame.getWidth(); x += gridSize) {
      g.drawLine(x, 0, x, gameFrame.getHeight());
    }

    // Draw horizontal lines
    for (int y = 0; y < gameFrame.getHeight(); y += gridSize) {
      g.drawLine(0, y, gameFrame.getWidth(), y);
    }
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
    LOGGER.info("Editor active state set to: " + active);
  }

  public void toggleActive() {
    setActive(!active);
  }

  /**
   * Toggle grid visibility
   */
  public void toggleGrid() {
    showGrid = !showGrid;
  }

  /**
   * Toggle snap-to-grid
   */
  public void toggleSnapping() {
    enableSnapping = !enableSnapping;
  }

  /**
   * Set the current theme for all panels
   */
  public void setTheme(EditorTheme theme) {
    currentTheme = theme;

    // Apply to all panels
    synchronized (elements) {
      for (EditorElement element : elements) {
        if (element instanceof EditorPanel) {
          ((EditorPanel) element).setTheme(theme);
        }
      }
    }
  }

  /**
   * Toggle between available themes
   */
  public void cycleTheme() {
    if (currentTheme == EditorTheme.DEFAULT) {
      setTheme(EditorTheme.LIGHT);
    } else {
      setTheme(EditorTheme.DEFAULT);
    }
  }

  /**
   * Save current layout to file
   */
  public void saveLayout(String name) {
    try {
      File file = new File(LAYOUTS_DIRECTORY + name + ".layout");
      try (FileWriter writer = new FileWriter(file)) {
        // Write number of elements
        writer.write(elements.size() + "\n");
        // Write each element's data
        synchronized (elements) {
          for (EditorElement element : elements) {
            if (element instanceof EditorPanel) {
              EditorPanel panel = (EditorPanel) element;
              writer.write(String.format("%s,%d,%d,%d,%d,%b,%b\n",
                  panel.getName(),
                  panel.getX(),
                  panel.getY(),
                  panel.getWidth(),
                  panel.getHeight(),
                  panel.isVisible(),
                  panel.isCollapsed()));
            }
          }
        }
      }
      LOGGER.info("Layout saved: " + name);
    } catch (Exception e) {
      LOGGER.warning("Failed to save layout: " + e.getMessage());
    }
  }

  /**
   * Load layout from file
   */
  public void loadLayout(String name) {
    try {
      File file = new File(LAYOUTS_DIRECTORY + name + ".layout");
      if (!file.exists()) {
        LOGGER.warning("Layout file not found: " + name);
        return;
      }

      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        // Read number of elements
        int count = Integer.parseInt(reader.readLine());
        // Map to store loaded panel data
        Map<String, String[]> panelData = new HashMap<>();
        // Read each element's data
        for (int i = 0; i < count; i++) {
          String line = reader.readLine();
          if (line == null)
            break;

          String[] parts = line.split(",");
          if (parts.length >= 7) {
            panelData.put(parts[0], parts);
          }
        }
        // Apply loaded data to existing panels
        synchronized (elements) {
          for (EditorElement element : elements) {
            if (element instanceof EditorPanel) {
              String[] data = panelData.get(element.getName());
              if (data != null) {
                element.setPosition(Integer.parseInt(data[1]), Integer.parseInt(data[2]));
                EditorPanel panel = (EditorPanel) element;
                int width = Integer.parseInt(data[3]);
                int height = Integer.parseInt(data[4]);
                panel.width = width;
                panel.height = height;
                panel.expandedHeight = height;
                panel.setVisible(Boolean.parseBoolean(data[5]));
                panel.setCollapsed(Boolean.parseBoolean(data[6]));
              }
            }
          }
        }
      }

      LOGGER.info("Layout loaded: " + name);
    } catch (Exception e) {
      LOGGER.warning("Failed to load layout: " + e.getMessage());
    }
  }

  // Factory methods for creating common editor elements
  public EditorPanel createPanel(int x, int y, int width, int height, String name) {
    EditorPanel panel = new EditorPanel(x, y, width, height, name);
    panel.setTheme(currentTheme);
    addElement(panel);
    return panel;
  }
}
