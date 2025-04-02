package com.engine.components;

import java.awt.Graphics2D;

import com.engine.graph.Renderable;

public class RenderableComponent {
  private Renderable r;

  public Renderable getR() {
    return r;
  }

  public void setR(Renderable r) {
    this.r = r;
  }

  public RenderableComponent(Renderable r) {
    this.r = r;
  }

  public void render(Graphics2D g) {
    r.render(g);
  }
}
