package com.engine.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class Button extends AbstractUIElement {
  private String text;
  private Color backgroundColor;
  private Color textColor;
  private Color hoverColor;
  private boolean isHovered;
  private Font font;
  private List<ActionListener> clickListeners;

  public Button(String text, float x, float y, float width, float height) {
    super(x, y, width, height);
    this.text = text;
    this.backgroundColor = Color.GRAY;
    this.textColor = Color.WHITE;
    this.hoverColor = Color.DARK_GRAY;
    this.isHovered = false;
    this.font = new Font("Arial", Font.PLAIN, 14);
    this.clickListeners = new ArrayList<>();
  }

  @Override
  public void render(Graphics2D g) {
    // Draw button background
    g.setColor(isHovered ? hoverColor : backgroundColor);
    g.fillRect((int) x, (int) y, (int) width, (int) height);

    // Draw button border
    g.setColor(Color.BLACK);
    g.drawRect((int) x, (int) y, (int) width, (int) height);

    // Draw text centered
    g.setColor(textColor);
    g.setFont(font);
    java.awt.FontMetrics metrics = g.getFontMetrics(font);
    int textX = (int) (x + (width - metrics.stringWidth(text)) / 2);
    int textY = (int) (y + ((height - metrics.getHeight()) / 2) + metrics.getAscent());
    g.drawString(text, textX, textY);
  }

  public void setHovered(boolean hovered) {
    this.isHovered = hovered;
  }

  public void click() {
    for (ActionListener listener : clickListeners) {
      listener.actionPerformed(new java.awt.event.ActionEvent(this, 0, "click"));
    }
  }

  public void addClickListener(ActionListener listener) {
    clickListeners.add(listener);
  }

  public void removeClickListener(ActionListener listener) {
    clickListeners.remove(listener);
  }

  // Getters and setters
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Color getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(Color color) {
    this.backgroundColor = color;
  }

  public Color getTextColor() {
    return textColor;
  }

  public void setTextColor(Color color) {
    this.textColor = color;
  }

  public Color getHoverColor() {
    return hoverColor;
  }

  public void setHoverColor(Color color) {
    this.hoverColor = color;
  }
}
