package com.engine.components;

import java.awt.Graphics2D;

import com.engine.graph.Renderable;

public class RenderableComponent {
  private Renderable r;
  private boolean visible = true;

  public Renderable getR() {
    return r;
  }

  public void setR(Renderable r) {
    this.r = r;
  }

  public RenderableComponent(Renderable r) {
    this.r = r;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public void render(Graphics2D g) {
    if (visible && r != null) {
      r.render(g);
    }
  }
}
