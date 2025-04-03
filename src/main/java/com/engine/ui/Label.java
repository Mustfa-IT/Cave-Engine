package com.engine.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class Label extends AbstractUIElement {
  private String text;
  private Color textColor;
  private Font font;
  private boolean centered;

  public Label(String text, float x, float y) {
    super(x, y, 0, 0);
    this.text = text;
    this.textColor = Color.BLACK;
    this.font = new Font("Arial", Font.PLAIN, 34);
    this.centered = false;
    updateDimensions();
  }

  private void updateDimensions() {
    // Create temporary image to measure text
    java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(1, 1,
        java.awt.image.BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setFont(font);
    java.awt.FontMetrics metrics = g.getFontMetrics();
    this.width = metrics.stringWidth(text);
    this.height = metrics.getHeight();
    g.dispose();
  }

  @Override
  public void render(Graphics2D g) {
    Font originalFont = g.getFont();
    Color originalColor = g.getColor();

    g.setFont(font);
    g.setColor(textColor);

    float drawX = x;
    if (centered) {
      java.awt.FontMetrics metrics = g.getFontMetrics();
      drawX = x - metrics.stringWidth(text) / 2f;
    }

    g.drawString(text, drawX, y + g.getFontMetrics().getAscent());

    g.setFont(originalFont);
    g.setColor(originalColor);
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
    updateDimensions();
  }

  public Color getTextColor() {
    return textColor;
  }

  public void setTextColor(Color color) {
    this.textColor = color;
  }

  public Font getFont() {
    return font;
  }

  public void setFont(Font font) {
    this.font = font;
    updateDimensions();
  }

  public boolean isCentered() {
    return centered;
  }

  public void setCentered(boolean centered) {
    this.centered = centered;
  }
}
