package com.engine.graph;

import java.awt.Graphics2D;

import com.engine.components.Transform;

public class Rect extends Shape {
  private final double width;
  private final double height;

  public Rect(Transform transform, java.awt.Color color, double width, double height) {
    super(transform, color);
    this.width = width;
    this.height = height;
  }

  @Override
  protected void drawShape(Graphics2D g) {
    int halfWidth = (int) (width / 2);
    int halfHeight = (int) (height / 2);
    // Draw the rectangle centered at (0,0) - fixed to use halfHeight properly
    g.fillRect(-halfWidth, -halfHeight, (int) width, (int) height);
  }
}
