package com.engine.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public class Panel extends AbstractUIElement {
  private List<UIElement> children;
  private Color backgroundColor;
  private boolean drawBackground;

  public Panel(float x, float y, float width, float height) {
    super(x, y, width, height);
    this.children = new ArrayList<>();
    this.backgroundColor = new Color(200, 200, 200, 150); // Semi-transparent gray
    this.drawBackground = true;
  }

  @Override
  public void render(Graphics2D g) {
    Graphics2D localG = (Graphics2D) g.create();

    // Draw panel background if enabled
    if (drawBackground) {
      localG.setColor(backgroundColor);
      localG.fillRect((int) x, (int) y, (int) width, (int) height);
    }

    // Render all children
    for (UIElement child : children) {
      child.render(localG);
    }

    localG.dispose();
  }

  public void addElement(UIElement element) {
    children.add(element);
  }

  public void removeElement(UIElement element) {
    children.remove(element);
  }

  public boolean isDrawBackground() {
    return drawBackground;
  }

  public void setDrawBackground(boolean drawBackground) {
    this.drawBackground = drawBackground;
  }

  public Color getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(Color backgroundColor) {
    this.backgroundColor = backgroundColor;
  }

  @Override
  public boolean contains(float pointX, float pointY) {
    if (super.contains(pointX, pointY)) {
      // Check if any child element contains the point
      for (UIElement child : children) {
        if (child.contains(pointX, pointY)) {
          return true;
        }
      }
      return true;
    }
    return false;
  }
}
