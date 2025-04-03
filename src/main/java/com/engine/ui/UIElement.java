package com.engine.ui;

import java.awt.Graphics2D;

public interface UIElement {
  void render(Graphics2D g);

  void update(double deltaTime);

  boolean contains(float x, float y);

  void setPosition(float x, float y);

  void setSize(float width, float height);

  float getX();

  float getY();

  float getWidth();

  float getHeight();
}
