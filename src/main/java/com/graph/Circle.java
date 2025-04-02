package com.graph;

import java.awt.Graphics2D;

import com.components.Transform;

/**
 * A Circle shape that extends the abstract Shape.
 */
public class Circle extends Shape {
    private final double radius;

    public Circle(Transform transform, java.awt.Color color, double radius) {
        super(transform, color);
        this.radius = radius;
    }

    @Override
    protected void drawShape(Graphics2D g) {
        int diameter = (int) (radius * 2);
        // Draw the circle centered at (0,0)
        g.fillOval((int) -radius, (int) -radius, diameter, diameter);
    }
}
