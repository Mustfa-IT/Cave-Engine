package com.engine.editor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.function.Function;

public class PropertyPanel extends EditorPanel {
  // Use LinkedHashMap to maintain insertion order for properties
  private Map<String, Property<?>> properties = new LinkedHashMap<>();
  private Map<String, PropertyChangeListener> changeListeners = new HashMap<>();
  private int propertyHeight = 20;
  private int propertyPadding = 5;

  // Property groups
  private Map<String, List<String>> propertyGroups = new LinkedHashMap<>();
  private Map<String, Boolean> groupExpanded = new HashMap<>();

  // Tooltips
  private Map<String, String> tooltips = new HashMap<>();
  private String currentTooltip = null;
  private Point tooltipPosition = new Point();

  // Editing state
  private String editingProperty = null;
  private String editBuffer = "";
  private String validationError = null;
  private long validationErrorTime = 0;
  private static final long ERROR_DISPLAY_DURATION = 2000; // 2 seconds

  // Color picker state
  private boolean showingColorPicker = false;
  private String colorPickerProperty = null;
  private Rectangle colorPickerRect = new Rectangle();
  private Color[] colorPalette = new Color[] {
      Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE,
      Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.ORANGE,
      Color.PINK, Color.LIGHT_GRAY, Color.GRAY, Color.DARK_GRAY
  };

  // Dropdown menu state
  private boolean showingDropdown = false;
  private String dropdownProperty = null;
  private Rectangle dropdownRect = new Rectangle();
  private List<String> dropdownOptions = new ArrayList<>();
  private int highlightedOption = -1;

  // Interface for property change callbacks
  public interface PropertyChangeListener {
    void onPropertyChanged(String name, Object oldValue, Object newValue);
  }

  // Type-safe property wrapper
  public static class Property<T> {
    private T value;
    private Class<T> type;
    private Function<String, T> parser;
    private String[] options; // For dropdown properties

    public Property(T initialValue, Class<T> type) {
      this.value = initialValue;
      this.type = type;

      // Set up default parsers for common types
      if (type == Integer.class) {
        this.parser = s -> type.cast(Integer.parseInt(s));
      } else if (type == Float.class) {
        this.parser = s -> type.cast(Float.parseFloat(s));
      } else if (type == Double.class) {
        this.parser = s -> type.cast(Double.parseDouble(s));
      } else if (type == Boolean.class) {
        this.parser = s -> type.cast(Boolean.parseBoolean(s));
      } else if (type == String.class) {
        this.parser = s -> type.cast(s);
      } else if (type == Color.class) {
        this.parser = s -> {
          try {
            if (s.startsWith("#")) {
              return type.cast(Color.decode(s));
            } else {
              // Handle color by name using reflection
              return type.cast(Color.class.getField(s.toUpperCase()).get(null));
            }
          } catch (Exception e) {
            throw new IllegalArgumentException("Invalid color: " + s);
          }
        };
      }
    }

    public void setOptions(String[] options) {
      this.options = options;
    }

    public String[] getOptions() {
      return options;
    }

    public boolean hasOptions() {
      return options != null && options.length > 0;
    }

    public T getValue() {
      return value;
    }

    public void setValue(T value) {
      this.value = value;
    }

    public Class<T> getType() {
      return type;
    }

    public boolean isEnum() {
      return type.isEnum();
    }

    public String[] getEnumOptions() {
      if (isEnum()) {
        Object[] enumConstants = type.getEnumConstants();
        String[] options = new String[enumConstants.length];
        for (int i = 0; i < enumConstants.length; i++) {
          options[i] = enumConstants[i].toString();
        }
        return options;
      }
      return new String[0];
    }

    public T parse(String text) throws IllegalArgumentException {
      if (parser != null) {
        return parser.apply(text);
      } else if (isEnum()) {
        // Handle enum parsing
        Object[] enumConstants = type.getEnumConstants();
        for (Object constant : enumConstants) {
          if (constant.toString().equalsIgnoreCase(text)) {
            return type.cast(constant);
          }
        }
        throw new IllegalArgumentException("Invalid enum value: " + text);
      }
      throw new IllegalArgumentException("No parser available for type: " + type.getName());
    }
  }

  public PropertyPanel(int x, int y, int width, int height, String name) {
    super(x, y, width, height, name);
  }

  /**
   * Add a property to a group
   */
  public void addPropertyToGroup(String groupName, String propertyName) {
    propertyGroups.computeIfAbsent(groupName, k -> new ArrayList<>())
        .add(propertyName);
    if (!groupExpanded.containsKey(groupName)) {
      groupExpanded.put(groupName, true); // Groups expanded by default
    }
  }

  /**
   * Create a new group and set its expanded state
   */
  public void createGroup(String groupName, boolean expanded) {
    propertyGroups.computeIfAbsent(groupName, k -> new ArrayList<>());
    groupExpanded.put(groupName, expanded);
  }

  /**
   * Toggle a group's expanded state
   */
  public void toggleGroup(String groupName) {
    if (groupExpanded.containsKey(groupName)) {
      groupExpanded.put(groupName, !groupExpanded.get(groupName));
    }
  }

  /**
   * Set tooltip text for a property
   */
  public void setPropertyTooltip(String propertyName, String tooltip) {
    tooltips.put(propertyName, tooltip);
  }

  /**
   * Set a generic property with automatic type detection
   */
  public <T> void setProperty(String name, T value) {
    if (properties.containsKey(name)) {
      // Property exists, update it
      @SuppressWarnings("unchecked")
      Property<T> property = (Property<T>) properties.get(name);
      T oldValue = property.getValue();
      property.setValue(value);

      // Notify listeners
      notifyPropertyChanged(name, oldValue, value);
    } else {
      // Create a new property
      @SuppressWarnings("unchecked")
      Class<T> type = (Class<T>) value.getClass();
      Property<T> property = new Property<>(value, type);
      properties.put(name, property);
    }
  }

  /**
   * Set a dropdown property with predefined options
   */
  public <T> void setDropdownProperty(String name, T value, String[] options) {
    @SuppressWarnings("unchecked")
    Class<T> type = (Class<T>) value.getClass();
    Property<T> property = new Property<>(value, type);
    property.setOptions(options);
    properties.put(name, property);
  }

  /**
   * Set an enum property
   */
  public <T extends Enum<T>> void setEnumProperty(String name, T value) {
    @SuppressWarnings("unchecked")
    Class<T> type = (Class<T>) value.getClass();
    Property<T> property = new Property<>(value, type);
    properties.put(name, property);
  }

  /**
   * Get property value with type safety
   */
  @SuppressWarnings("unchecked")
  public <T> T getPropertyValue(String name) {
    Property<?> property = properties.get(name);
    if (property != null) {
      return (T) property.getValue();
    }
    return null;
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
   * Gets the absolute screen position of the panel, accounting for parent panels
   *
   * @return Point with absolute x,y coordinates
   */
  private Point getAbsolutePosition() {
    int absoluteX = getX();
    int absoluteY = getY();

    // If this panel is nested within other panels, we need to account for their
    // positions
    EditorElement parent = getParent();
    while (parent != null) {
      if (parent instanceof EditorPanel) {
        EditorPanel parentPanel = (EditorPanel) parent;
        absoluteX += parentPanel.getX();
        // For Y, we need to account for the header height in parent panels
        absoluteY += parentPanel.getY() + (parent == getParent() ? parentPanel.getHeaderHeight() : 0);
      }

      // Check if the parent has a parent
      if (parent instanceof AbstractEditorElement) {
        parent = ((AbstractEditorElement) parent).getParent();
      } else {
        parent = null;
      }
    }

    return new Point(absoluteX, absoluteY);
  }

  /**
   * Handle mouse click to begin editing a property or toggle boolean properties
   */
  @SuppressWarnings("unchecked")
  public boolean handlePropertyClick(int mouseX, int mouseY) {
    if (isCollapsed())
      return false;

    // Reset tooltip
    currentTooltip = null;

    // Get absolute position of this panel
    Point absPos = getAbsolutePosition();
    int absoluteX = absPos.x;
    int absoluteY = absPos.y;

    // First check if we need to handle dropdown or color picker clicks
    if (showingDropdown && dropdownRect.contains(mouseX, mouseY)) {
      // Calculate which option was clicked
      int optionHeight = 20;
      int optionIndex = (mouseY - dropdownRect.y) / optionHeight;

      if (optionIndex >= 0 && optionIndex < dropdownOptions.size()) {
        // Get the selected option
        String selectedOption = dropdownOptions.get(optionIndex);
        Property<?> property = properties.get(dropdownProperty);

        if (property != null) {
          try {
            Object oldValue = property.getValue();
            Object newValue = property.parse(selectedOption);
            Property<Object> typedProperty = (Property<Object>) property;
            typedProperty.setValue(newValue);
            notifyPropertyChanged(dropdownProperty, oldValue, newValue);
          } catch (Exception e) {
            validationError = "Invalid value: " + e.getMessage();
            validationErrorTime = System.currentTimeMillis();
          }
        }
      }

      showingDropdown = false;
      dropdownProperty = null;
      return true;
    }

    // Handle color picker if it's showing
    if (showingColorPicker && colorPickerRect.contains(mouseX, mouseY)) {
      // Calculate which color was clicked
      int colorSize = 20;
      int colorPadding = 2;
      int colorsPerRow = Math.min(colorPalette.length, 5);

      int clickOffsetX = mouseX - colorPickerRect.x;
      int clickOffsetY = mouseY - colorPickerRect.y;

      int col = clickOffsetX / (colorSize + colorPadding);
      int row = clickOffsetY / (colorSize + colorPadding);

      if (col >= 0 && col < colorsPerRow && row >= 0) {
        int colorIndex = row * colorsPerRow + col;
        if (colorIndex < colorPalette.length) {
          // Set the color
          Property<?> property = properties.get(colorPickerProperty);
          if (property != null && property.getType() == Color.class) {
            Property<Color> colorProperty = (Property<Color>) property;
            Color oldValue = colorProperty.getValue();
            Color newValue = colorPalette[colorIndex];
            colorProperty.setValue(newValue);
            notifyPropertyChanged(colorPickerProperty, oldValue, newValue);
          }
        }
      }

      showingColorPicker = false;
      colorPickerProperty = null;
      return true;
    }

    // If any dropdown or color picker was showing, hide it if clicked elsewhere
    if (showingDropdown || showingColorPicker) {
      showingDropdown = false;
      dropdownProperty = null;
      showingColorPicker = false;
      colorPickerProperty = null;
      return true;
    }

    // Normal property handling
    int currentY = absoluteY + headerHeight + propertyPadding;

    // First handle group headers
    for (String groupName : propertyGroups.keySet()) {
      int groupHeaderHeight = 20;
      Rectangle groupRect = new Rectangle(
          absoluteX + 5,
          currentY,
          getWidth() - 10,
          groupHeaderHeight);

      if (groupRect.contains(mouseX, mouseY)) {
        toggleGroup(groupName);
        return true;
      }

      currentY += groupHeaderHeight + 2;

      // Skip properties in collapsed groups
      if (!groupExpanded.getOrDefault(groupName, true)) {
        continue;
      }

      // Process properties in this group
      List<String> groupProps = propertyGroups.get(groupName);
      for (String propName : groupProps) {
        Property<?> property = properties.get(propName);
        if (property == null)
          continue;

        Rectangle propRect = new Rectangle(
            absoluteX + 10,
            currentY,
            getWidth() - 20,
            propertyHeight);

        // Check for tooltip hover
        if (propRect.contains(mouseX, mouseY) && tooltips.containsKey(propName)) {
          currentTooltip = tooltips.get(propName);
          tooltipPosition.setLocation(mouseX, mouseY - 20);
        }

        // Handle property value area clicks
        int labelWidth = Math.max(100, getWidth() / 2);
        Rectangle valueRect = new Rectangle(
            absoluteX + labelWidth,
            currentY,
            getWidth() - labelWidth - 10,
            propertyHeight);

        if (valueRect.contains(mouseX, mouseY)) {
          // Handle based on property type
          if (property.getType() == Boolean.class) {
            // Toggle boolean
            Boolean oldValue = (Boolean) property.getValue();
            Boolean newValue = !oldValue;
            ((Property<Boolean>) property).setValue(newValue);
            notifyPropertyChanged(propName, oldValue, newValue);
          } else if (property.getType() == Color.class) {
            // Show color picker
            showingColorPicker = true;
            colorPickerProperty = propName;
            colorPickerRect = new Rectangle(
                valueRect.x,
                valueRect.y + valueRect.height,
                Math.min(5, colorPalette.length) * 22,
                ((colorPalette.length + 4) / 5) * 22);
          } else if (property.hasOptions() || property.isEnum()) {
            // Show dropdown menu
            showingDropdown = true;
            dropdownProperty = propName;

            // Get all available options
            String[] options = property.hasOptions() ? property.getOptions() : property.getEnumOptions();
            dropdownOptions = new ArrayList<>(Arrays.asList(options));

            // Calculate dropdown size and position
            int dropdownHeight = dropdownOptions.size() * 20;
            dropdownRect = new Rectangle(
                valueRect.x,
                valueRect.y + valueRect.height,
                valueRect.width,
                Math.min(dropdownHeight, 200)); // Max height 200px

            // Highlight the current value
            Object currentValue = property.getValue();
            for (int i = 0; i < dropdownOptions.size(); i++) {
              if (dropdownOptions.get(i).equals(currentValue.toString())) {
                highlightedOption = i;
                break;
              }
            }
          } else {
            // Start editing text properties
            editingProperty = propName;
            editBuffer = String.valueOf(property.getValue());
          }
          return true;
        }

        currentY += propertyHeight + propertyPadding;
      }
    }

    // Now handle ungrouped properties
    for (String propName : properties.keySet()) {
      // Skip properties that are in groups
      boolean inGroup = false;
      for (List<String> groupProps : propertyGroups.values()) {
        if (groupProps.contains(propName)) {
          inGroup = true;
          break;
        }
      }
      if (inGroup)
        continue;

      Property<?> property = properties.get(propName);

      Rectangle propRect = new Rectangle(
          absoluteX + 10,
          currentY,
          getWidth() - 20,
          propertyHeight);

      // Check for tooltip hover
      if (propRect.contains(mouseX, mouseY) && tooltips.containsKey(propName)) {
        currentTooltip = tooltips.get(propName);
        tooltipPosition.setLocation(mouseX, mouseY - 20);
      }

      // Handle property value area clicks
      int labelWidth = Math.max(100, getWidth() / 2);
      Rectangle valueRect = new Rectangle(
          absoluteX + labelWidth,
          currentY,
          getWidth() - labelWidth - 10,
          propertyHeight);

      if (valueRect.contains(mouseX, mouseY)) {
        // Handle based on property type
        if (property.getType() == Boolean.class) {
          // Toggle boolean
          Boolean oldValue = (Boolean) property.getValue();
          Boolean newValue = !oldValue;
          ((Property<Boolean>) property).setValue(newValue);
          notifyPropertyChanged(propName, oldValue, newValue);
        } else if (property.getType() == Color.class) {
          // Show color picker
          showingColorPicker = true;
          colorPickerProperty = propName;
          colorPickerRect = new Rectangle(
              valueRect.x,
              valueRect.y + valueRect.height,
              Math.min(5, colorPalette.length) * 22,
              ((colorPalette.length + 4) / 5) * 22);
        } else if (property.hasOptions() || property.isEnum()) {
          // Show dropdown menu
          showingDropdown = true;
          dropdownProperty = propName;

          // Get all available options
          String[] options = property.hasOptions() ? property.getOptions() : property.getEnumOptions();
          dropdownOptions = new ArrayList<>(Arrays.asList(options));

          // Calculate dropdown size and position
          int dropdownHeight = dropdownOptions.size() * 20;
          dropdownRect = new Rectangle(
              valueRect.x,
              valueRect.y + valueRect.height,
              valueRect.width,
              Math.min(dropdownHeight, 200)); // Max height 200px

          // Highlight the current value
          Object currentValue = property.getValue();
          highlightedOption = -1; // Reset
          for (int i = 0; i < dropdownOptions.size(); i++) {
            if (dropdownOptions.get(i).equals(currentValue.toString())) {
              highlightedOption = i;
              break;
            }
          }
        } else {
          // Start editing text properties
          editingProperty = propName;
          editBuffer = String.valueOf(property.getValue());
        }
        return true;
      }

      currentY += propertyHeight + propertyPadding;
    }

    return false;
  }

  /**
   * Handle mouse movement to show tooltips and highlight dropdown items
   */
  public boolean handleMouseMove(int mouseX, int mouseY) {
    // Handle dropdown hover highlighting
    if (showingDropdown && dropdownRect.contains(mouseX, mouseY)) {
      int optionHeight = 20;
      int hoveredOption = (mouseY - dropdownRect.y) / optionHeight;

      if (hoveredOption >= 0 && hoveredOption < dropdownOptions.size()) {
        highlightedOption = hoveredOption;
        return true;
      }
    }

    return false;
  }

  /**
   * Handle keyboard input for editing properties
   */
  public void handleKeyTyped(char keyChar) {
    if (editingProperty == null)
      return;

    Property<?> property = properties.get(editingProperty);
    if (property == null) {
      editingProperty = null;
      return;
    }

    if (keyChar == '\n') {
      // Submit change
      try {
        Object oldValue = property.getValue();
        Object newValue = property.parse(editBuffer);

        // Use the correct type for setValue
        if (property.getType() == Boolean.class) {
          ((Property<Boolean>) property).setValue((Boolean) newValue);
        } else if (property.getType() == Integer.class) {
          ((Property<Integer>) property).setValue((Integer) newValue);
        } else if (property.getType() == Float.class) {
          ((Property<Float>) property).setValue((Float) newValue);
        } else if (property.getType() == Double.class) {
          ((Property<Double>) property).setValue((Double) newValue);
        } else if (property.getType() == String.class) {
          ((Property<String>) property).setValue((String) newValue);
        } else if (property.getType() == Color.class) {
          ((Property<Color>) property).setValue((Color) newValue);
        } else {
          // Generic fallback - less type safe
          @SuppressWarnings("unchecked")
          Property<Object> typedProperty = (Property<Object>) property;
          typedProperty.setValue(newValue);
        }

        notifyPropertyChanged(editingProperty, oldValue, newValue);
        editingProperty = null;
      } catch (Exception e) {
        // Show error message
        validationError = "Invalid value: " + e.getMessage();
        validationErrorTime = System.currentTimeMillis();
      }
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

    // Get absolute position for rendering
    Point absPos = getAbsolutePosition();
    int absoluteX = absPos.x;
    int absoluteY = absPos.y;

    // Draw properties
    g.setColor(getTheme().getTextColor());
    g.setFont(new Font("SansSerif", Font.PLAIN, 12));

    int currentY = absoluteY + headerHeight + propertyPadding;
    int maxLabelWidth = 0;

    // First pass to calculate max label width
    for (String propName : properties.keySet()) {
      int width = g.getFontMetrics().stringWidth(propName);
      maxLabelWidth = Math.max(maxLabelWidth, width);
    }
    maxLabelWidth = Math.min(maxLabelWidth + 10, getWidth() / 2);

    // Render property groups first
    for (String groupName : propertyGroups.keySet()) {
      // Draw group header
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      g.setColor(getTheme().getHeaderColor());
      g.fillRect(absoluteX + 5, currentY, getWidth() - 10, 20);
      g.setColor(getTheme().getBorderColor());
      g.drawRect(absoluteX + 5, currentY, getWidth() - 10, 20);
      g.setColor(getTheme().getTextColor());
      g.drawString(groupName, absoluteX + 10, currentY + 15);

      // Draw expand/collapse icon
      boolean expanded = groupExpanded.getOrDefault(groupName, true);
      g.drawString(expanded ? "▼" : "►", absoluteX + getWidth() - 20, currentY + 15);

      currentY += 20 + 2; // Group header height + padding

      // Skip rendering properties in collapsed groups
      if (!expanded) {
        continue;
      }

      // Render properties in this group
      g.setFont(new Font("SansSerif", Font.PLAIN, 12));
      List<String> groupProps = propertyGroups.get(groupName);
      for (String propName : groupProps) {
        Property<?> property = properties.get(propName);
        if (property == null)
          continue;

        // Draw property name with indent
        g.setColor(getTheme().getTextColor());
        g.drawString(propName + ":", absoluteX + 15, currentY + 15);

        // Draw property value or edit field
        int valueX = absoluteX + maxLabelWidth + 10;

        if (propName.equals(editingProperty)) {
          renderEditField(g, valueX, currentY);
        } else {
          // Draw different controls based on property type
          renderPropertyValue(g, valueX, currentY, propName, property);
        }

        currentY += propertyHeight + propertyPadding;
      }
    }

    // Render ungrouped properties
    g.setFont(new Font("SansSerif", Font.PLAIN, 12));
    for (String propName : properties.keySet()) {
      // Skip properties that are in groups
      boolean inGroup = false;
      for (List<String> groupProps : propertyGroups.values()) {
        if (groupProps.contains(propName)) {
          inGroup = true;
          break;
        }
      }
      if (inGroup)
        continue;

      Property<?> property = properties.get(propName);

      // Draw property name
      g.setColor(getTheme().getTextColor());
      g.drawString(propName + ":", absoluteX + 10, currentY + 15);

      // Draw property value or edit field
      int valueX = absoluteX + maxLabelWidth + 10;

      if (propName.equals(editingProperty)) {
        renderEditField(g, valueX, currentY);
      } else {
        // Draw different controls based on property type
        renderPropertyValue(g, valueX, currentY, propName, property);
      }

      currentY += propertyHeight + propertyPadding;
    }

    // Draw dropdown menu if active
    if (showingDropdown) {
      renderDropdownMenu(g);
    }

    // Draw color picker if active
    if (showingColorPicker) {
      renderColorPicker(g);
    }

    // Draw validation error if present
    if (validationError != null &&
        System.currentTimeMillis() - validationErrorTime < ERROR_DISPLAY_DURATION) {
      g.setColor(Color.RED);
      g.drawString(validationError, absoluteX + 10, absoluteY + height - 10);
    }

    // Draw tooltip
    if (currentTooltip != null) {
      renderTooltip(g, currentTooltip, tooltipPosition.x, tooltipPosition.y);
    }
  }

  /**
   * Render the edit field for text properties
   */
  private void renderEditField(Graphics2D g, int x, int y) {
    // Draw edit field background
    g.setColor(new Color(80, 80, 80));
    g.fillRect(x, y, getWidth() - (x - getAbsolutePosition().x) - 10, propertyHeight);

    // Draw edit text
    g.setColor(Color.WHITE);
    g.drawString(editBuffer, x + 5, y + 15);

    // Draw cursor
    if (System.currentTimeMillis() % 1000 < 500) {
      int cursorX = x + 5 + g.getFontMetrics().stringWidth(editBuffer);
      g.drawLine(cursorX, y + 3, cursorX, y + propertyHeight - 3);
    }
  }

  /**
   * Render the dropdown menu
   */
  private void renderDropdownMenu(Graphics2D g) {
    int x = dropdownRect.x;
    int y = dropdownRect.y;
    int width = dropdownRect.width;
    int height = dropdownRect.height;

    // Draw background
    g.setColor(new Color(60, 60, 60, 240));
    g.fillRect(x, y, width, height);
    g.setColor(new Color(100, 100, 100));
    g.drawRect(x, y, width, height);

    // Draw options
    int optionHeight = 20;
    for (int i = 0; i < dropdownOptions.size(); i++) {
      int optionY = y + i * optionHeight;

      // Highlight selected/hovered option
      if (i == highlightedOption) {
        g.setColor(new Color(100, 100, 160));
        g.fillRect(x + 1, optionY, width - 2, optionHeight);
      }

      // Draw option text
      g.setColor(Color.WHITE);
      g.drawString(dropdownOptions.get(i), x + 5, optionY + 15);

      // Draw separator
      if (i < dropdownOptions.size() - 1) {
        g.setColor(new Color(80, 80, 80));
        g.drawLine(x + 1, optionY + optionHeight, x + width - 1, optionY + optionHeight);
      }
    }
  }

  /**
   * Render the color picker popup
   */
  private void renderColorPicker(Graphics2D g) {
    int x = colorPickerRect.x;
    int y = colorPickerRect.y;
    int colorSize = 20;
    int colorPadding = 2;
    int colorsPerRow = Math.min(colorPalette.length, 5);

    // Draw background
    g.setColor(new Color(60, 60, 60, 240));
    g.fillRect(x - 5, y - 5,
        colorsPerRow * (colorSize + colorPadding) + 10,
        ((colorPalette.length + colorsPerRow - 1) / colorsPerRow) * (colorSize + colorPadding) + 10);
    g.setColor(new Color(100, 100, 100));
    g.drawRect(x - 5, y - 5,
        colorsPerRow * (colorSize + colorPadding) + 10,
        ((colorPalette.length + colorsPerRow - 1) / colorsPerRow) * (colorSize + colorPadding) + 10);

    // Draw colors
    for (int i = 0; i < colorPalette.length; i++) {
      int col = i % colorsPerRow;
      int row = i / colorsPerRow;
      int colorX = x + col * (colorSize + colorPadding);
      int colorY = y + row * (colorSize + colorPadding);

      g.setColor(colorPalette[i]);
      g.fillRect(colorX, colorY, colorSize, colorSize);
      g.setColor(Color.DARK_GRAY);
      g.drawRect(colorX, colorY, colorSize, colorSize);
    }
  }

  /**
   * Render tooltip
   */
  private void renderTooltip(Graphics2D g, String text, int x, int y) {
    int width = g.getFontMetrics().stringWidth(text) + 10;
    int height = 20;

    g.setColor(new Color(60, 60, 60, 220));
    g.fillRect(x, y - height, width, height);
    g.setColor(Color.WHITE);
    g.drawRect(x, y - height, width, height);
    g.drawString(text, x + 5, y - 5);
  }

  /**
   * Render a property value based on its type
   */
  private void renderPropertyValue(Graphics2D g, int x, int y, String propName, Property<?> property) {
    // Common calculations
    int valueWidth = getWidth() - (x - getAbsolutePosition().x) - 10;
    int controlWidth = Math.min(100, valueWidth / 3); // Width for control elements
    int controlX = x + valueWidth - controlWidth; // Position controls at right side

    // First draw value area background with slight shading
    g.setColor(new Color(50, 50, 50, 120));
    g.fillRect(x, y, valueWidth, propertyHeight);
    g.setColor(new Color(70, 70, 70));
    g.drawRect(x, y, valueWidth, propertyHeight);

    if (property.getType() == Boolean.class) {
      // Handle boolean properties with checkbox on right
      boolean value = (Boolean) property.getValue();

      // Draw the value text on the left
      g.setColor(Color.WHITE);
      g.drawString(value ? "True" : "False", x + 5, y + 15);

      // Draw checkbox on right
      int checkboxSize = 16;
      int checkboxX = controlX + (controlWidth - checkboxSize) / 2;

      g.setColor(Color.DARK_GRAY);
      g.fillRect(checkboxX, y + 2, checkboxSize, checkboxSize);
      g.setColor(Color.LIGHT_GRAY);
      g.drawRect(checkboxX, y + 2, checkboxSize, checkboxSize);

      // Draw check mark if value is true
      if (value) {
        g.setColor(Color.WHITE);
        g.drawLine(checkboxX + 3, y + 8, checkboxX + 7, y + 12);
        g.drawLine(checkboxX + 7, y + 12, checkboxX + 13, y + 6);
      }
    } else if (property.getType() == Color.class) {
      // Handle color properties with color swatch on right
      Color value = (Color) property.getValue();

      // Draw color name/hex on the left
      g.setColor(Color.WHITE);
      String colorText = String.format("#%02X%02X%02X", value.getRed(), value.getGreen(), value.getBlue());
      g.drawString(colorText, x + 5, y + 15);

      // Draw color swatch on right
      int swatchWidth = Math.min(controlWidth - 4, 30);
      int swatchHeight = 16;
      int swatchX = controlX + (controlWidth - swatchWidth) / 2;

      g.setColor(Color.DARK_GRAY);
      g.fillRect(swatchX - 1, y + 2 - 1, swatchWidth + 2, swatchHeight + 2);
      g.setColor(value);
      g.fillRect(swatchX, y + 2, swatchWidth, swatchHeight);
      g.setColor(Color.WHITE);
      g.drawRect(swatchX, y + 2, swatchWidth, swatchHeight);
    } else if (property.hasOptions() || property.isEnum()) {
      // Handle dropdown properties with dropdown button on right
      Object currentValue = property.getValue();

      // Calculate dropdown button area
      int buttonWidth = Math.min(controlWidth, 30);
      int buttonX = controlX + (controlWidth - buttonWidth) / 2;

      // Draw value text on the left
      g.setColor(Color.WHITE);
      String valueText = String.valueOf(currentValue);
      // Trim if too long
      if (g.getFontMetrics().stringWidth(valueText) > (controlX - x - 10)) {
        int charsToShow = valueText.length();
        while (charsToShow > 3
            && g.getFontMetrics().stringWidth(valueText.substring(0, charsToShow) + "...") > (controlX - x - 10)) {
          charsToShow--;
        }
        valueText = valueText.substring(0, charsToShow) + "...";
      }
      g.drawString(valueText, x + 5, y + 15);

      // Draw dropdown arrow
      g.setColor(Color.WHITE);
      int[] xPoints = { buttonX + 10, buttonX + buttonWidth / 2, buttonX + buttonWidth - 10 };
      int[] yPoints = { y + 7, y + propertyHeight - 7, y + 7 };
      g.fillPolygon(xPoints, yPoints, 3);
    } else if (property.getType() == Integer.class || property.getType() == Float.class ||
        property.getType() == Double.class) {
      // Handle numeric properties
      Object value = property.getValue();

      // Draw formatted number
      g.setColor(Color.WHITE);
      String formattedValue;

      if (property.getType() == Integer.class) {
        formattedValue = value.toString();
      } else {
        // Format float/double to show max 2 decimal places
        double doubleValue = ((Number) value).doubleValue();
        formattedValue = String.format("%.2f", doubleValue);
        // Remove trailing zeros
        if (formattedValue.endsWith(".00")) {
          formattedValue = formattedValue.substring(0, formattedValue.length() - 3);
        } else if (formattedValue.endsWith("0")) {
          formattedValue = formattedValue.substring(0, formattedValue.length() - 1);
        }
      }

      g.drawString(formattedValue, x + 5, y + 15);

      // Draw subtle increase/decrease controls on right
      int buttonWidth = Math.min(controlWidth / 2, 20);
      int buttonX = controlX + (controlWidth - buttonWidth * 2) / 2;

      // // Draw decrease button (-)
      // g.setColor(new Color(60, 60, 60));
      // g.fillRect(buttonX, y + 2, buttonWidth, propertyHeight - 4);
      // g.setColor(new Color(100, 100, 100));
      // g.drawRect(buttonX, y + 2, buttonWidth, propertyHeight - 4);
      // g.setColor(Color.WHITE);
      // g.drawLine(buttonX + 2, y + propertyHeight / 2, buttonX + buttonWidth - 2, y
      // + propertyHeight / 2);

      // // Draw increase button (+)
      // g.setColor(new Color(60, 60, 60));
      // g.fillRect(buttonX + buttonWidth, y + 2, buttonWidth, propertyHeight - 4);
      // g.setColor(new Color(100, 100, 100));
      // g.drawRect(buttonX + buttonWidth, y + 2, buttonWidth, propertyHeight - 4);
      // g.setColor(Color.WHITE);
      // g.drawLine(buttonX + buttonWidth + 2, y + propertyHeight / 2, buttonX +
      // buttonWidth * 2 - 2,
      // y + propertyHeight / 2);
      // g.drawLine(buttonX + buttonWidth + buttonWidth / 2, y + 4, buttonX +
      // buttonWidth + buttonWidth / 2,
      // y + propertyHeight - 4);
    } else if (property.getType() == String.class) {
      // Handle string properties with text field appearance
      String value = (String) property.getValue();

      // Draw string value with subtle text field appearance
      g.setColor(Color.WHITE);

      // Trim if too long
      String displayValue = value;
      if (g.getFontMetrics().stringWidth(displayValue) > valueWidth - 10) {
        int charsToShow = displayValue.length();
        while (charsToShow > 3 &&
            g.getFontMetrics().stringWidth(displayValue.substring(0, charsToShow) + "...") > valueWidth - 10) {
          charsToShow--;
        }
        displayValue = displayValue.substring(0, charsToShow) + "...";
      }

      g.drawString(displayValue, x + 5, y + 15);

      // Draw edit hint on right
      g.setColor(new Color(100, 100, 100));
      g.drawString("✎", x + valueWidth - 20, y + 15);
    } else {
      // Generic fallback for any other type
      g.setColor(Color.WHITE);
      g.drawString(String.valueOf(property.getValue()), x + 5, y + 15);
    }
  }
}
