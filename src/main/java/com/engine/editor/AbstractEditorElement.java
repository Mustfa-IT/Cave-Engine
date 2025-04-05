package com.engine.editor;

import java.awt.Graphics2D;

public abstract class AbstractEditorElement implements EditorElement {
  protected int x, y, width, height;
  protected String name;
  protected boolean draggable = true;
  protected EditorElement parent = null;

  public AbstractEditorElement(int x, int y, int width, int height, String name) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.name = name;
  }

  @Override
  public boolean contains(int x, int y) {
    return x >= this.x && x <= this.x + width &&
        y >= this.y && y <= this.y + height;
  }

  @Override
  public void setPosition(int x, int y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public int getX() {
    return x;
  }

  @Override
  public int getY() {
    return y;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean isDraggable() {
    return draggable;
  }

  @Override
  public void setDraggable(boolean draggable) {
    this.draggable = draggable;
  }

  @Override
  public void update(double deltaTime) {
    // Default implementation does nothing
  }

  @Override
  public EditorElement getParent() {
    return parent;
  }

  @Override
  public void setParent(EditorElement parent) {
    this.parent = parent;
  }
}
