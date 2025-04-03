package com.engine.graph;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.util.logging.Logger;

import com.engine.components.CameraComponent;
import com.engine.components.RenderableComponent;
import com.engine.components.Transform;
import com.engine.components.UIComponent;
import com.engine.core.CameraSystem;
import com.engine.core.GameEngine;
import com.engine.core.GameWindow;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public class RenderSystem {
  private final GameWindow window;
  private final Dominion world;
  private final CameraSystem cameraSystem;
  private static final Logger LOGGER = Logger.getLogger(GameEngine.class.getName());

  public RenderSystem(GameWindow window, Dominion world, CameraSystem cameraSystem) {
    this.cameraSystem = cameraSystem;
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

    // Get base graphics context for the frame
    Graphics2D g = (Graphics2D) bs.getDrawGraphics();
    // Clear the screen
    g.clearRect(0, 0, window.getWidth(), window.getHeight());

    // Draw world grid for reference (optional)
    // drawWorldGrid(g);

    // Get the camera-transformed graphics context
    Entity camera = cameraSystem.getActiveCamera();
    if (camera != null) {
      Transform camTransform = camera.get(Transform.class);
      CameraComponent camComponent = camera.get(CameraComponent.class);

      if (camTransform != null && camComponent != null) {
        // Log camera position for debugging
        LOGGER.fine("Rendering with camera at: " + camTransform.getX() + ", " + camTransform.getY() +
            " zoom: " + camComponent.getZoom());
      }
    }

    // Apply camera transformation and render entities
    Graphics2D cameraTranformedG = cameraSystem.applyActiveCamera((Graphics2D) g.create());
    renderEntities(cameraTranformedG);
    cameraTranformedG.dispose();

    // Create a separate graphics context for UI (not affected by camera)
    Graphics2D uiG = (Graphics2D) g.create();
    renderUI(uiG);
    uiG.dispose();

    // Clean up and display
    g.dispose();
    bs.show();
    Toolkit.getDefaultToolkit().sync();
  }

  /**
   * Draw a reference grid in world space for debugging
   */
  private void drawWorldGrid(Graphics2D g) {
    Entity camera = cameraSystem.getActiveCamera();
    if (camera == null)
      return;

    CameraComponent camComponent = camera.get(CameraComponent.class);
    if (camComponent == null)
      return;

    // Get screen dimensions
    int screenWidth = window.getWidth();
    int screenHeight = window.getHeight();

    // Convert screen edges to world coordinates
    float[] topLeft = cameraSystem.screenToWorld(0, 0);
    float[] bottomRight = cameraSystem.screenToWorld(screenWidth, screenHeight);

    // Draw coordinate axes with transformed graphics
    Graphics2D g2d = cameraSystem.applyActiveCamera((Graphics2D) g.create());

    // Set up line style
    g2d.setColor(new Color(0, 0, 255, 128)); // Semi-transparent blue
    g2d.setStroke(new BasicStroke(0.5f));

    // Draw grid lines
    float gridSize = 50; // Size of each grid cell

    // Calculate grid bounds based on screen edges in world space
    float startX = (float) (Math.floor(topLeft[0] / gridSize) * gridSize);
    float endX = (float) (Math.ceil(bottomRight[0] / gridSize) * gridSize);
    float startY = (float) (Math.floor(topLeft[1] / gridSize) * gridSize);
    float endY = (float) (Math.ceil(bottomRight[1] / gridSize) * gridSize);

    // Draw vertical grid lines
    for (float x = startX; x <= endX; x += gridSize) {
      g2d.drawLine((int) x, (int) startY, (int) x, (int) endY);
    }

    // Draw horizontal grid lines
    for (float y = startY; y <= endY; y += gridSize) {
      g2d.drawLine((int) startX, (int) y, (int) endX, (int) y);
    }

    // Draw the main axes with stronger lines
    g2d.setStroke(new BasicStroke(2.0f));
    g2d.setColor(Color.RED);
    g2d.drawLine(-1000, 0, 1000, 0); // X-axis

    g2d.setColor(Color.GREEN);
    g2d.drawLine(0, -1000, 0, 1000); // Y-axis

    // Draw origin marker
    g2d.setColor(Color.WHITE);
    g2d.fillOval(-5, -5, 10, 10);

    g2d.dispose();
  }

  /**
   * Render UI Elements
   */
  private void renderUI(Graphics2D g) {
    world.findEntitiesWith(UIComponent.class).forEach((c) -> {
      UIComponent com = c.comp();
      com.render(g);
    });
  }

  /**
   * Renders entities using the camera-transformed graphics context
   */
  private void renderEntities(Graphics2D g) {
    world.findEntitiesWith(Transform.class, RenderableComponent.class).forEach(result -> {
      Transform transform = result.comp1();
      RenderableComponent renderable = result.comp2();

      // Create a copy of the camera-transformed graphics for this entity
      Graphics2D entityG = (Graphics2D) g.create();

      // Apply entity's local transformations
      entityG.translate(transform.getX(), transform.getY());
      entityG.rotate(transform.getRotation());
      entityG.scale(transform.getScaleX(), transform.getScaleY());

      // Render the entity with the combined transformations (camera + entity)
      renderable.render(entityG);

      entityG.dispose();
    });
  }
}
