package com.engine.ui;

public abstract class AbstractUIElement implements UIElement {
  protected float x, y;
  protected float width, height;

  public AbstractUIElement(float x, float y, float width, float height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  @Override
  public boolean contains(float x, float y) {
    return x >= this.x && x <= this.x + width &&
        y >= this.y && y <= this.y + height;
  }

  @Override
  public void setPosition(float x, float y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public void setSize(float width, float height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public float getX() {
    return x;
  }

  @Override
  public float getY() {
    return y;
  }

  @Override
  public float getWidth() {
    return width;
  }

  @Override
  public float getHeight() {
    return height;
  }

  @Override
  public void update(double deltaTime) {
    // Default implementation does nothing
    // Subclasses can override this method if they need to update their state
  }
}
