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
    List<EditorElement> elementsCopy = new ArrayList<>(elements);

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

  // Theme management
  private static final String THEMES_DIRECTORY = "editorThemes/";

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
   * Cycle through all available themes
   */
  public void cycleTheme() {
    String[] themes = listAvailableThemes();

    if (themes.length == 0) {
      // Fall back to built-in themes
      if (currentTheme == EditorTheme.DEFAULT) {
        setTheme(EditorTheme.LIGHT);
      } else {
        setTheme(EditorTheme.DEFAULT);
      }
      return;
    }

    // Find the current theme in the list or start with the first one
    int currentIndex = -1;
    String currentThemeName = currentTheme.getName();

    for (int i = 0; i < themes.length; i++) {
      if (themes[i].equals(currentThemeName)) {
        currentIndex = i;
        break;
      }
    }

    // Move to the next theme or back to the first
    currentIndex = (currentIndex + 1) % themes.length;
    loadTheme(themes[currentIndex]);
  }

  /**
   * Save current theme to a file
   */
  public void saveTheme(String name) {
    try {
      // Create themes directory if it doesn't exist
      Files.createDirectories(Paths.get(THEMES_DIRECTORY));

      File file = new File(THEMES_DIRECTORY + name + ".theme");
      try (FileWriter writer = new FileWriter(file)) {
        // Write theme properties
        writer.write("name=" + currentTheme.getName() + "\n");
        writer.write("backgroundColor=" + colorToHex(currentTheme.getBackgroundColor()) + "\n");
        writer.write("headerColor=" + colorToHex(currentTheme.getHeaderColor()) + "\n");
        writer.write("textColor=" + colorToHex(currentTheme.getTextColor()) + "\n");
        writer.write("borderColor=" + colorToHex(currentTheme.getBorderColor()) + "\n");
      }
      LOGGER.info("Theme saved: " + name);
    } catch (Exception e) {
      LOGGER.warning("Failed to save theme: " + e.getMessage());
    }
  }

  /**
   * Load theme from a file
   */
  public void loadTheme(String name) {
    try {
      File file = new File(THEMES_DIRECTORY + name + ".theme");
      if (!file.exists()) {
        LOGGER.warning("Theme file not found: " + name);
        return;
      }

      // Create a properties object to load the theme
      java.util.Properties props = new java.util.Properties();
      try (FileReader reader = new FileReader(file)) {
        props.load(reader);
      }

      // Create a new theme from the properties
      String themeName = props.getProperty("name", name);
      java.awt.Color bgColor = hexToColor(props.getProperty("backgroundColor", "#222222"));
      java.awt.Color headerColor = hexToColor(props.getProperty("headerColor", "#333333"));
      java.awt.Color textColor = hexToColor(props.getProperty("textColor", "#FFFFFF"));
      java.awt.Color borderColor = hexToColor(props.getProperty("borderColor", "#444444"));

      EditorTheme theme = new EditorTheme(themeName, bgColor, headerColor, textColor, borderColor);
      setTheme(theme);

      LOGGER.info("Theme loaded: " + name);
    } catch (Exception e) {
      LOGGER.warning("Failed to load theme: " + e.getMessage());
    }
  }

  /**
   * Get a list of available theme names
   */
  public String[] listAvailableThemes() {
    List<String> themes = new ArrayList<>();
    try {
      // Create themes directory if it doesn't exist
      Files.createDirectories(Paths.get(THEMES_DIRECTORY));

      File themeDir = new File(THEMES_DIRECTORY);
      File[] themeFiles = themeDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".theme"));

      if (themeFiles != null) {
        for (File file : themeFiles) {
          String themeName = file.getName();
          // Remove .theme extension
          themeName = themeName.substring(0, themeName.length() - 6);
          themes.add(themeName);
        }
      }
    } catch (Exception e) {
      LOGGER.warning("Error listing themes: " + e.getMessage());
    }
    return themes.toArray(new String[0]);
  }

  /**
   * Convert color to hex string
   */
  private String colorToHex(java.awt.Color color) {
    return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
  }

  /**
   * Convert hex string to color
   */
  private java.awt.Color hexToColor(String hex) {
    if (hex == null || !hex.startsWith("#")) {
      return java.awt.Color.BLACK;
    }
    try {
      return new java.awt.Color(
          Integer.parseInt(hex.substring(1, 3), 16),
          Integer.parseInt(hex.substring(3, 5), 16),
          Integer.parseInt(hex.substring(5, 7), 16));
    } catch (Exception e) {
      LOGGER.warning("Invalid hex color: " + hex);
      return java.awt.Color.BLACK;
    }
  }

  /**
   * Save current layout to file
   */
  public void saveLayout(String name) {
    try {
      // Create layouts directory if it doesn't exist
      Files.createDirectories(Paths.get(LAYOUTS_DIRECTORY));

      File file = new File(LAYOUTS_DIRECTORY + name + ".layout");
      try (FileWriter writer = new FileWriter(file)) {
        // Write layout version and theme
        writer.write("version=1.0\n");
        writer.write("theme=" + currentTheme.getName() + "\n");

        // Write number of elements
        int panelCount = 0;
        synchronized (elements) {
          for (EditorElement element : elements) {
            if (element instanceof EditorPanel) {
              panelCount++;
            }
          }
        }

        writer.write("panelCount=" + panelCount + "\n");

        // Write each element's data
        synchronized (elements) {
          for (EditorElement element : elements) {
            if (element instanceof EditorPanel) {
              EditorPanel panel = (EditorPanel) element;
              writer.write(String.format("panel.name=%s\n", panel.getName()));
              writer.write(String.format("panel.x=%d\n", panel.getX()));
              writer.write(String.format("panel.y=%d\n", panel.getY()));
              writer.write(String.format("panel.width=%d\n", panel.getWidth()));
              writer.write(String.format("panel.height=%d\n", panel.getHeight()));
              writer.write(String.format("panel.visible=%b\n", panel.isVisible()));
              writer.write(String.format("panel.collapsed=%b\n", panel.isCollapsed()));
              writer.write(String.format("panel.draggable=%b\n", panel.isDraggable()));
              writer.write(String.format("panel.resizable=%b\n", panel.isResizable()));
              writer.write("panel.end\n");
            }
          }
        }
      }
      LOGGER.info("Layout saved: " + name);
    } catch (Exception e) {
      LOGGER.warning("Failed to save layout: " + e.getMessage());
      e.printStackTrace();
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
        // Read header
        String versionLine = reader.readLine();
        if (versionLine == null || !versionLine.startsWith("version=")) {
          LOGGER.warning("Invalid layout file format");
          return;
        }

        // Read theme
        String themeLine = reader.readLine();
        if (themeLine != null && themeLine.startsWith("theme=")) {
          String themeName = themeLine.substring(6);
          // Try to load the theme if it's not a built-in one
          if (!themeName.equals("DEFAULT") && !themeName.equals("LIGHT")) {
            try {
              loadTheme(themeName);
            } catch (Exception e) {
              LOGGER.warning("Could not load theme: " + themeName);
            }
          }
        }

        // Read panel count
        String countLine = reader.readLine();
        if (countLine == null || !countLine.startsWith("panelCount=")) {
          LOGGER.warning("Invalid layout file format: missing panel count");
          return;
        }

        // Map to store loaded panel data
        Map<String, Map<String, String>> panelsData = new HashMap<>();

        // Start reading panel data
        String line;
        String currentPanel = null;
        Map<String, String> panelProps = null;

        while ((line = reader.readLine()) != null) {
          if (line.startsWith("panel.name=")) {
            // Start of a new panel
            currentPanel = line.substring(11);
            panelProps = new HashMap<>();
            panelsData.put(currentPanel, panelProps);
          } else if (line.equals("panel.end")) {
            // End of current panel
            currentPanel = null;
            panelProps = null;
          } else if (currentPanel != null && panelProps != null && line.contains("=")) {
            // Panel property
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
              panelProps.put(parts[0], parts[1]);
            }
          }
        }

        // Apply loaded data to existing panels
        synchronized (elements) {
          for (EditorElement element : elements) {
            if (element instanceof EditorPanel) {
              Map<String, String> data = panelsData.get(element.getName());
              if (data != null) {
                try {
                  // Apply properties
                  if (data.containsKey("panel.x") && data.containsKey("panel.y")) {
                    element.setPosition(
                        Integer.parseInt(data.get("panel.x")),
                        Integer.parseInt(data.get("panel.y")));
                  }

                  EditorPanel panel = (EditorPanel) element;
                  if (data.containsKey("panel.width") && data.containsKey("panel.height")) {
                    int width = Integer.parseInt(data.get("panel.width"));
                    int height = Integer.parseInt(data.get("panel.height"));
                    panel.width = width;
                    panel.height = height;
                    panel.expandedHeight = height;
                  }

                  if (data.containsKey("panel.visible")) {
                    panel.setVisible(Boolean.parseBoolean(data.get("panel.visible")));
                  }

                  if (data.containsKey("panel.collapsed")) {
                    panel.setCollapsed(Boolean.parseBoolean(data.get("panel.collapsed")));
                  }

                  if (data.containsKey("panel.draggable")) {
                    panel.setDraggable(Boolean.parseBoolean(data.get("panel.draggable")));
                  }

                  if (data.containsKey("panel.resizable")) {
                    panel.setResizable(Boolean.parseBoolean(data.get("panel.resizable")));
                  }
                } catch (Exception e) {
                  LOGGER.warning("Error applying properties to panel " + element.getName() + ": " + e.getMessage());
                }
              }
            }
          }
        }
      }

      LOGGER.info("Layout loaded: " + name);
    } catch (Exception e) {
      LOGGER.warning("Failed to load layout: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Get a list of available layout names
   */
  public String[] listAvailableLayouts() {
    List<String> layouts = new ArrayList<>();
    try {
      // Create layouts directory if it doesn't exist
      Files.createDirectories(Paths.get(LAYOUTS_DIRECTORY));

      File layoutDir = new File(LAYOUTS_DIRECTORY);
      File[] layoutFiles = layoutDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".layout"));

      if (layoutFiles != null) {
        for (File file : layoutFiles) {
          String layoutName = file.getName();
          // Remove .layout extension
          layoutName = layoutName.substring(0, layoutName.length() - 7);
          layouts.add(layoutName);
        }
      }
    } catch (Exception e) {
      LOGGER.warning("Error listing layouts: " + e.getMessage());
    }
    return layouts.toArray(new String[0]);
  }

  // Factory methods for creating common editor elements
  public EditorPanel createPanel(int x, int y, int width, int height, String name) {
    EditorPanel panel = new EditorPanel(x, y, width, height, name);
    panel.setTheme(currentTheme);
    addElement(panel);
    return panel;
  }
}
