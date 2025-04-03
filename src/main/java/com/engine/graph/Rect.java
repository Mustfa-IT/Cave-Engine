package com.engine.graph;

import java.awt.Graphics2D;


public class Rect extends Shape {
  private final double width;
  private final double height;

  public Rect(java.awt.Color color, double width, double height) {
    super(color);
    this.width = width;
    this.height = height;
  }

  @Override
  protected void drawShape(Graphics2D g) {
    // Draw the rectangle centered at (0,0) - fixed to use halfHeight properly
    g.fillRect((int)-width/2,(int) -height/2,(int) width,(int) height);
  }
}
