package com.engine.editor;

import java.awt.Color;

public class EditorTheme {
  private String name;
  private Color backgroundColor;
  private Color headerColor;
  private Color borderColor;
  private Color titleColor;
  private Color textColor;
  private Color selectionColor;
  private Color resizeHandleColor;

  // Default dark theme
  public static final EditorTheme DEFAULT = new EditorTheme(
      "Dark",
      new Color(40, 40, 40, 220),
      new Color(60, 60, 60),
      new Color(100, 100, 100),
      Color.WHITE,
      Color.LIGHT_GRAY,
      new Color(65, 105, 225),
      new Color(200, 200, 200, 150));

  // Light theme
  public static final EditorTheme LIGHT = new EditorTheme(
      "Light",
      new Color(240, 240, 240, 220),
      new Color(220, 220, 220),
      new Color(180, 180, 180),
      Color.BLACK,
      Color.DARK_GRAY,
      new Color(51, 153, 255),
      new Color(120, 120, 120, 150));

  public EditorTheme(String name, Color backgroundColor, Color headerColor,
      Color borderColor, Color titleColor, Color textColor,
      Color selectionColor, Color resizeHandleColor) {
    this.name = name;
    this.backgroundColor = backgroundColor;
    this.headerColor = headerColor;
    this.borderColor = borderColor;
    this.titleColor = titleColor;
    this.textColor = textColor;
    this.selectionColor = selectionColor;
    this.resizeHandleColor = resizeHandleColor;
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

  public Color getBorderColor() {
    return borderColor;
  }

  public Color getTitleColor() {
    return titleColor;
  }

  public Color getTextColor() {
    return textColor;
  }

  public Color getSelectionColor() {
    return selectionColor;
  }

  public Color getResizeHandleColor() {
    return resizeHandleColor;
  }
}
