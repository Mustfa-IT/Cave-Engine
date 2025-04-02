// Transform.java
package com.components;

/**
 * Encapsulates position, rotation (in radians) and scale.
 */
public class Transform {
  private double x;
  private double y;
  private double rotation; // in radians
  private double scaleX;
  private double scaleY;

  public Transform(double x, double y) {
    this(x, y, Math.toRadians(0), 1, 1);
  }

  /// Rotation in Radians
  public Transform(double x, double y, double rotation, double scaleX, double scaleY) {
    this.x = x;
    this.y = y;
    this.rotation = rotation;
    this.scaleX = scaleX;
    this.scaleY = scaleY;
  }

  // Getters and setters
  public double getX() {
    return x;
  }

  public void setX(double x) {
    this.x = x;
  }

  public double getY() {
    return y;
  }

  public void setY(double y) {
    this.y = y;
  }

  public double getRotation() {
    return rotation;
  }

  public void setRotation(double rotation) {
    this.rotation = rotation;
  }

  public double getScaleX() {
    return scaleX;
  }

  public void setScaleX(double scaleX) {
    this.scaleX = scaleX;
  }

  public double getScaleY() {
    return scaleY;
  }

  public void setScaleY(double scaleY) {
    this.scaleY = scaleY;
  }
}
