package com.engine.editor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PropertyPanel extends EditorPanel {
  private Map<String, Object> properties = new HashMap<>();
  private Map<String, PropertyChangeListener> changeListeners = new HashMap<>();
  private int propertyHeight = 20;
  private int propertyPadding = 5;

  // Track which property is being edited
  private String editingProperty = null;
  private String editBuffer = "";

  // Interface for property change callbacks
  public interface PropertyChangeListener {
    void onPropertyChanged(String name, Object oldValue, Object newValue);
  }

  public PropertyPanel(int x, int y, int width, int height, String name) {
    super(x, y, width, height, name);
  }

  public void setProperty(String name, Object value) {
    Object oldValue = properties.get(name);
    properties.put(name, value);

    // Notify listeners
    notifyPropertyChanged(name, oldValue, value);
  }

  public Object getProperty(String name) {
    return properties.get(name);
  }

  public void setPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    changeListeners.put(propertyName, listener);
  }

  private void notifyPropertyChanged(String name, Object oldValue, Object newValue) {
    PropertyChangeListener listener = changeListeners.get(name);
    if (listener != null) {
      listener.onPropertyChanged(name, oldValue, newValue);
    }
  }

  /**
   * Handle mouse click to begin editing a property
   */
  public boolean handlePropertyClick(int mouseX, int mouseY) {
    if (isCollapsed())
      return false;

    int yPos = getY() + headerHeight + propertyPadding;

    for (String propName : properties.keySet()) {
      int propBottom = yPos + propertyHeight;

      if (mouseY >= yPos && mouseY <= propBottom) {
        editingProperty = propName;
        editBuffer = String.valueOf(properties.get(propName));
        return true;
      }

      yPos += propertyHeight + propertyPadding;
    }

    return false;
  }

  /**
   * Handle keyboard input for editing properties
   */
  public void handleKeyTyped(char keyChar) {
    if (editingProperty == null)
      return;

    if (keyChar == '\n') {
      // Submit change
      try {
        Object oldValue = properties.get(editingProperty);
        Object newValue;

        if (oldValue instanceof Integer) {
          newValue = Integer.parseInt(editBuffer);
        } else if (oldValue instanceof Float) {
          newValue = Float.parseFloat(editBuffer);
        } else if (oldValue instanceof Double) {
          newValue = Double.parseDouble(editBuffer);
        } else if (oldValue instanceof Boolean) {
          newValue = Boolean.parseBoolean(editBuffer);
        } else {
          newValue = editBuffer;
        }

        properties.put(editingProperty, newValue);
        notifyPropertyChanged(editingProperty, oldValue, newValue);
      } catch (Exception e) {
        // Revert on error
        editBuffer = String.valueOf(properties.get(editingProperty));
      }
      editingProperty = null;
    } else if (keyChar == 27) { // ESC key
      // Cancel edit
      editingProperty = null;
    } else if (keyChar == '\b') {
      // Backspace
      if (editBuffer.length() > 0) {
        editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
      }
    } else {
      // Add character
      editBuffer += keyChar;
    }
  }

  /**
   * Check if property is currently being edited
   */
  public boolean isEditing() {
    return editingProperty != null;
  }

  @Override
  public void render(Graphics2D g) {
    super.render(g);

    if (isCollapsed())
      return;

    // Draw properties
    g.setColor(getTheme().getTextColor());
    g.setFont(new Font("SansSerif", Font.PLAIN, 12));

    int yPos = getY() + headerHeight + propertyPadding;
    int maxLabelWidth = 0;

    // First pass to determine max label width
    for (String propName : properties.keySet()) {
      int width = g.getFontMetrics().stringWidth(propName);
      maxLabelWidth = Math.max(maxLabelWidth, width);
    }

    maxLabelWidth += 10; // Add some padding

    // Second pass to draw properties
    for (String propName : properties.keySet()) {
      // Draw property name
      g.setColor(getTheme().getTextColor());
      g.drawString(propName + ":", getX() + 10, yPos + 15);

      // Draw property value or edit field
      int valueX = getX() + maxLabelWidth + 10;
      if (propName.equals(editingProperty)) {
        // Draw edit field background
        g.setColor(new Color(80, 80, 80));
        g.fillRect(valueX, yPos, getWidth() - valueX - 10, propertyHeight);

        // Draw edit text
        g.setColor(Color.WHITE);
        g.drawString(editBuffer, valueX + 5, yPos + 15);

        // Draw cursor
        if (System.currentTimeMillis() % 1000 < 500) {
          int cursorX = valueX + 5 + g.getFontMetrics().stringWidth(editBuffer);
          g.drawLine(cursorX, yPos + 3, cursorX, yPos + propertyHeight - 3);
        }
      } else {
        // Draw regular value
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(properties.get(propName)), valueX, yPos + 15);
      }

      yPos += propertyHeight + propertyPadding;
    }
  }
}
