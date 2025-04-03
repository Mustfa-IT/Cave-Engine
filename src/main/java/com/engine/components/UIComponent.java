package com.engine.components;

import java.awt.Graphics2D;
import com.engine.ui.UIElement;

public class UIComponent {
  private UIElement ui;
  private boolean visible;

  public UIComponent(UIElement ui) {
    this.ui = ui;
    this.visible = true;
  }

  public UIElement getUi() {
    return ui;
  }

  public void setUi(UIElement ui) {
    this.ui = ui;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public void render(Graphics2D g) {
    if (visible && ui != null) {
      ui.render(g);
    }
  }

  public void update(double deltaTime) {
    if (ui != null) {
      ui.update(deltaTime);
    }
  }

  public boolean contains(float x, float y) {
    return ui != null && ui.contains(x, y);
  }
}
