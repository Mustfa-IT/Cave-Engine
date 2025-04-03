package com.engine.graph;

import java.awt.Graphics2D;

/**
 * A Circle shape that extends the abstract Shape.
 */
public class Circle extends Shape {
  private final double diameter; // Store as diameter for consistency

  public Circle(java.awt.Color color, double diameter) {
    super(color);
    this.diameter = diameter;
  }

  @Override
  protected void drawShape(Graphics2D g) {
    int radius = (int) (diameter / 2);
    // Draw the circle centered at (0,0) - this works correctly in world space
    g.fillOval(-radius, -radius, (int) diameter, (int) diameter);
  }
}
