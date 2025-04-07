// Shape.java
package com.engine.graph;

import java.awt.Graphics2D;

import java.awt.Color;

/**
 * Abstract shape class that implements Renderable.
 * It uses a Transform to handle position, rotation, and scale.
 */
public abstract class Shape implements Renderable {
  protected Color color;

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public Shape(Color color) {
    this.color = color;
  }

  /**
   * Draw the actual shape in local coordinates.
   */
  protected abstract void drawShape(Graphics2D g);

  /**
   * Render the shape applying its transformation.
   */
  @Override
  public void render(Graphics2D g) {
    Graphics2D g2 = (Graphics2D) g.create();
    // Apply translation, rotation, and scaling
    g2.setColor(color);
    drawShape(g2);
    g2.dispose();
  }
}
