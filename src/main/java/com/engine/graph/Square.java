package com.engine.graph;

import java.awt.Graphics2D;

import com.engine.components.Transform;

public class Square extends Shape {
  private final double side;

  public Square(Transform transform, java.awt.Color color, double side) {
      super(transform, color);
      this.side = side;
  }

  @Override
  protected void drawShape(Graphics2D g) {
      int halfSide = (int) (side / 2);
      // Draw the square centered at (0,0)
      g.fillRect(-halfSide, -halfSide, (int) side, (int) side);

  }
}
