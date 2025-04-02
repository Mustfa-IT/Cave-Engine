package com.engine.graph;

import java.awt.Graphics2D;

import com.engine.components.Transform;

/**
 * A Circle shape that extends the abstract Shape.
 */
public class Circle extends Shape {
  private final double diameter; // Store as diameter for consistency

  public Circle(Transform transform, java.awt.Color color, double diameter) {
    super(transform, color);
    this.diameter = diameter;
  }

  @Override
  protected void drawShape(Graphics2D g) {
    int radius = (int) (diameter / 2);
    // Draw the circle centered at (0,0)
    g.fillOval(-radius, -radius, (int) diameter, (int) diameter);
  }
}
