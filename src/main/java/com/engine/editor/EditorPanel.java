package com.engine.editor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.List;

public class EditorPanel extends AbstractEditorElement {
  private Color backgroundColor;
  private Color borderColor;
  private boolean visible = true;
  private float opacity = 0.8f;
  private boolean collapsed = false;
  protected int headerHeight = 25;
  private int collapsedHeight;
  int expandedHeight;

  // Resizing support
  private boolean resizable = true;
  private boolean resizing = false;
  private int resizeBorderSize = 5;
  private int minWidth = 100;
  private int minHeight = 50;
  private ResizeDirection resizeDir = ResizeDirection.NONE;

  // Panel content
  private List<EditorElement> children = new ArrayList<>();

  // Theme support
  private EditorTheme theme = EditorTheme.DEFAULT;

  public enum ResizeDirection {
    NONE, N, NE, E, SE, S, SW, W, NW
  }

  public EditorPanel(int x, int y, int width, int height, String name) {
    super(x, y, width, height, name);
    backgroundColor = new Color(40, 40, 40, 200);
    borderColor = new Color(100, 100, 100);
    collapsedHeight = headerHeight;
    expandedHeight = height;
  }

  @Override
  public void render(Graphics2D g) {
    if (!visible)
      return;

    // Save original composite
    var origComposite = g.getComposite();

    // Draw panel background
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
    g.setColor(theme.getBackgroundColor());
    g.fillRect(x, y, width, collapsed ? headerHeight : height);

    // Draw border
    g.setColor(theme.getBorderColor());
    g.drawRect(x, y, width, collapsed ? headerHeight : height);

    // Draw header
    g.setColor(theme.getHeaderColor());
    g.fillRect(x, y, width, headerHeight);
    g.setColor(theme.getBorderColor());
    g.drawRect(x, y, width, headerHeight);

    // Draw title
    g.setColor(theme.getTextColor());
    g.setFont(new Font("SansSerif", Font.BOLD, 12));
    g.drawString(name, x + 5, y + headerHeight - 8);

    // Draw collapse/expand button
    g.setColor(Color.LIGHT_GRAY);
    g.fillRect(x + width - 20, y + 5, 15, 15);
    g.setColor(Color.BLACK);
    g.drawRect(x + width - 20, y + 5, 15, 15);
    if (collapsed) {
      g.drawString("+", x + width - 16, y + 17);
    } else {
      g.drawString("-", x + width - 16, y + 17);
    }

    // Draw resize handles if not collapsed and resizable
    if (!collapsed && resizable) {
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
      g.setColor(theme.getBorderColor());

      // Bottom-right corner resize handle
      g.fillRect(x + width - 10, y + height - 10, 10, 10);

      // Highlight active resize direction
      if (resizing) {
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.0f));

        switch (resizeDir) {
          case SE:
            g.drawRect(x + width - 10, y + height - 10, 10, 10);
            break;
          case E:
            g.drawRect(x + width - 3, y + height / 2 - 10, 3, 20);
            break;
          case S:
            g.drawRect(x + width / 2 - 10, y + height - 3, 20, 3);
            break;
          // Add other cases as needed
          default:
            break;
        }
      }
    }

    // Draw content if not collapsed
    if (!collapsed) {
      // Draw child elements
      for (EditorElement child : children) {
        // Create clipping to ensure children don't render outside panel
        int contentY = y + headerHeight;
        int contentHeight = height - headerHeight;

        g.clipRect(x + 1, contentY, width - 2, contentHeight - 1);
        child.render(g);
        g.setClip(null); // Reset clip
      }
    }

    // Restore original composite
    g.setComposite(origComposite);
  }

  /**
   * Adds a child element to this panel
   */
  public void addChild(EditorElement element) {
    // Adjust the element's position to be relative to panel
    element.setPosition(element.getX() + x, element.getY() + y + headerHeight);
    children.add(element);
  }

  /**
   * Removes a child element from this panel
   */
  public void removeChild(EditorElement element) {
    children.remove(element);
  }

  /**
   * Get the list of child elements
   *
   * @return List of child elements
   */
  public List<EditorElement> getChildren() {
    return new ArrayList<>(children); // Return a copy to prevent outside modification
  }

  /**
   * Determines the resize direction based on mouse position
   */
  public ResizeDirection getResizeDirection(int mouseX, int mouseY) {
    if (!resizable || collapsed)
      return ResizeDirection.NONE;

    boolean onRight = Math.abs(mouseX - (x + width)) <= resizeBorderSize;
    boolean onBottom = Math.abs(mouseY - (y + height)) <= resizeBorderSize;
    boolean onLeft = Math.abs(mouseX - x) <= resizeBorderSize;
    boolean onTop = Math.abs(mouseY - y) <= resizeBorderSize;

    // Check corners first (they have priority)
    if (onRight && onBottom)
      return ResizeDirection.SE;
    if (onLeft && onBottom)
      return ResizeDirection.SW;
    if (onRight && onTop)
      return ResizeDirection.NE;
    if (onLeft && onTop)
      return ResizeDirection.NW;

    // Then check sides
    if (onRight)
      return ResizeDirection.E;
    if (onBottom)
      return ResizeDirection.S;
    if (onLeft)
      return ResizeDirection.W;
    if (onTop && !isHeaderClicked(mouseX, mouseY))
      return ResizeDirection.N;

    return ResizeDirection.NONE;
  }

  /**
   * Gets cursor type for current resize direction
   */
  public int getCursorForResizeDirection(ResizeDirection dir) {
    switch (dir) {
      case N:
        return Cursor.N_RESIZE_CURSOR;
      case NE:
        return Cursor.NE_RESIZE_CURSOR;
      case E:
        return Cursor.E_RESIZE_CURSOR;
      case SE:
        return Cursor.SE_RESIZE_CURSOR;
      case S:
        return Cursor.S_RESIZE_CURSOR;
      case SW:
        return Cursor.SW_RESIZE_CURSOR;
      case W:
        return Cursor.W_RESIZE_CURSOR;
      case NW:
        return Cursor.NW_RESIZE_CURSOR;
      default:
        return Cursor.DEFAULT_CURSOR;
    }
  }

  /**
   * Resize the panel according to the current resize direction
   */
  public void resize(int mouseX, int mouseY) {
    if (!resizing || resizeDir == ResizeDirection.NONE)
      return;

    int newWidth = width;
    int newHeight = height;
    int newX = x;
    int newY = y;

    switch (resizeDir) {
      case E:
        newWidth = Math.max(minWidth, mouseX - x);
        break;
      case S:
        newHeight = Math.max(minHeight, mouseY - y);
        break;
      case SE:
        newWidth = Math.max(minWidth, mouseX - x);
        newHeight = Math.max(minHeight, mouseY - y);
        break;
      case SW:
        newWidth = Math.max(minWidth, x + width - mouseX);
        newHeight = Math.max(minHeight, mouseY - y);
        newX = mouseX;
        break;
      case W:
        newWidth = Math.max(minWidth, x + width - mouseX);
        newX = mouseX;
        break;
      case NW:
        newWidth = Math.max(minWidth, x + width - mouseX);
        newHeight = Math.max(minHeight, y + height - mouseY);
        newX = mouseX;
        newY = mouseY;
        break;
      case N:
        newHeight = Math.max(minHeight, y + height - mouseY);
        newY = mouseY;
        break;
      case NE:
        newWidth = Math.max(minWidth, mouseX - x);
        newHeight = Math.max(minHeight, y + height - mouseY);
        newY = mouseY;
        break;
    }

    // Update panel dimensions
    x = newX;
    y = newY;
    width = newWidth;
    height = newHeight;
    expandedHeight = newHeight;
  }

  @Override
  public void setPosition(int newX, int newY) {
    // Calculate movement delta
    int deltaX = newX - x;
    int deltaY = newY - y;

    // Update this panel's position using parent method
    super.setPosition(newX, newY);

    // Update all child elements to maintain their relative positions
    for (EditorElement child : children) {
      child.setPosition(child.getX() + deltaX, child.getY() + deltaY);
    }
  }

  public boolean isCollapsed() {
    return collapsed;
  }

  public void setCollapsed(boolean collapsed) {
    this.collapsed = collapsed;
    height = collapsed ? collapsedHeight : expandedHeight;
  }

  public void toggleCollapsed() {
    setCollapsed(!collapsed);
  }

  public void setOpacity(float opacity) {
    this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
  }

  public float getOpacity() {
    return opacity;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public boolean isVisible() {
    return visible;
  }

  /**
   * Check if this panel can be dragged by the user
   *
   * @return true if the panel is draggable
   */
  public boolean isDraggable() {
    return true; // Panels are draggable by default
  }

  /**
   * Check if the click was on the panel's header (for dragging)
   */
  public boolean isHeaderClicked(int clickX, int clickY) {
    // First check if in header area (between top of panel and bottom of header)
    if (!visible || clickX < x || clickX > x + width ||
        clickY < y || clickY > y + headerHeight) {
      return false;
    }

    // Now check if it's not on the collapse button
    if (clickX >= x + width - 20 && clickX <= x + width - 5 &&
        clickY >= y + 5 && clickY <= y + 20) {
      return false;
    }

    return true;
  }

  /**
   * Check if the click was on the panel's collapse/expand button
   */
  public boolean isCollapseButtonClicked(int clickX, int clickY) {
    return visible && clickX >= x + width - 20 && clickX <= x + width - 5 &&
        clickY >= y + 5 && clickY <= y + 20;
  }

  public boolean isResizable() {
    return resizable;
  }

  public void setResizable(boolean resizable) {
    this.resizable = resizable;
  }

  public boolean isResizing() {
    return resizing;
  }

  public void setResizing(boolean resizing) {
    this.resizing = resizing;
  }

  public ResizeDirection getResizeDir() {
    return resizeDir;
  }

  public void setResizeDir(ResizeDirection resizeDir) {
    this.resizeDir = resizeDir;
  }

  public void setTheme(EditorTheme theme) {
    this.theme = theme;
  }

  public EditorTheme getTheme() {
    return theme;
  }

  public int getHeaderHeight() {
    return headerHeight;
  }
}
