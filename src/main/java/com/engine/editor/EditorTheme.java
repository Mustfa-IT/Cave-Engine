package com.engine.editor;

import java.awt.Color;

/**
 * Defines visual styles for the editor UI components
 */
public class EditorTheme {
  // Default built-in themes
  public static final EditorTheme DEFAULT = new EditorTheme(
      "DEFAULT",
      new Color(40, 40, 40, 220), // background
      new Color(60, 60, 60, 220), // header
      new Color(220, 220, 220), // text
      new Color(80, 80, 80) // border
  );

  public static final EditorTheme LIGHT = new EditorTheme(
      "LIGHT",
      new Color(220, 220, 220, 220), // background
      new Color(200, 200, 200, 220), // header
      new Color(20, 20, 20), // text
      new Color(150, 150, 150) // border
  );

  private final String name;
  private final Color backgroundColor;
  private final Color headerColor;
  private final Color textColor;
  private final Color borderColor;

  /**
   * Create a new custom theme
   */
  public EditorTheme(String name, Color backgroundColor, Color headerColor, Color textColor, Color borderColor) {
    this.name = name;
    this.backgroundColor = backgroundColor;
    this.headerColor = headerColor;
    this.textColor = textColor;
    this.borderColor = borderColor;
  }

  public String getName() {
    return name;
  }

  public Color getBackgroundColor() {
    return backgroundColor;
  }

  public Color getHeaderColor() {
    return headerColor;
  }

  public Color getTextColor() {
    return textColor;
  }

  public Color getBorderColor() {
    return borderColor;
  }
}
