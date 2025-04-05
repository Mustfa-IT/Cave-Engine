package com.engine.ui;

import com.engine.graph.Renderable;

public interface UIElement extends Renderable {
  void update(double deltaTime);

  boolean contains(float x, float y);

  void setPosition(float x, float y);

  void setSize(float width, float height);

  float getX();

  float getY();

  float getWidth();

  float getHeight();
}
