package com.engine.graph;

import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;

import com.engine.components.RenderableComponent;
import com.engine.core.GameWindow;

import dev.dominion.ecs.api.Dominion;;

public class Renderer {
  private final GameWindow window;
  private final Dominion world;

  public Renderer(GameWindow window, Dominion world) {
    this.window = window;
    this.world = world;
    // Ensure the window is visible before creating the BufferStrategy
    window.initialize();
    window.createBufferStrategy(2);
  }

  /**
   * Renders all registered renderable objects.
   */
  public void render() {
    BufferStrategy bs = window.getBufferStrategy();
    if (bs == null) {
      return;
    }
    Graphics2D g = (Graphics2D) bs.getDrawGraphics();
    // Clear the screen
    g.clearRect(0, 0, window.getWidth(), window.getHeight());
    // Render each object of the world
    world.findEntitiesWith( RenderableComponent.class).stream().forEach(result -> {
      RenderableComponent r = result.comp();
      r.render(g);
    });
    g.dispose();
    bs.show();
    Toolkit.getDefaultToolkit().sync();
  }
}
