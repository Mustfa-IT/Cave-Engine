package com.engine.editor;

import java.awt.Graphics2D;

public interface EditorElement {
  void render(Graphics2D g);

  void update(double deltaTime);

  boolean contains(int x, int y);

  void setPosition(int x, int y);

  int getX();

  int getY();

  int getWidth();

  int getHeight();

  String getName();

  void setName(String name);

  boolean isDraggable();

  void setDraggable(boolean draggable);

  // Add parent reference support
  EditorElement getParent();

  void setParent(EditorElement parent);
}
