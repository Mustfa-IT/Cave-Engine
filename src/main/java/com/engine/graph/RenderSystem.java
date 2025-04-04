package com.engine.graph;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.engine.components.CameraComponent;
import com.engine.components.PhysicsBodyComponent;
import com.engine.components.RenderableComponent;
import com.engine.components.Transform;
import com.engine.components.UIComponent;
import com.engine.core.CameraSystem;
import com.engine.core.GameEngine;
import com.engine.core.GameWindow;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

@Singleton
public class RenderSystem implements RenderingSystem {
  private final GameWindow window;
  private final Dominion world;
  private final CameraSystem cameraSystem;
  private static final Logger LOGGER = Logger.getLogger(GameEngine.class.getName());

  // Debug visualization flags
  private boolean debugPhysics = false;
  private boolean debugColliders = false;
  private boolean showGrid = true;

  @Inject
  public RenderSystem(GameWindow window, Dominion world, CameraSystem cameraSystem) {
    this.cameraSystem = cameraSystem;
    this.window = window;
    this.world = world;
    // Ensure the window is visible before creating the BufferStrategy
    window.initialize();
    window.createBufferStrategy(2);
  }

  /**
   * Configure debug visualization options
   *
   * @param showPhysics   Show physics debug info
   * @param showColliders Show collider outlines
   * @param showGrid      Show world grid
   */
  public void setDebugOptions(boolean showPhysics, boolean showColliders, boolean showGrid) {
    this.debugPhysics = showPhysics;
    this.debugColliders = showColliders;
    this.showGrid = showGrid;
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

    // Only draw grid if flag is enabled
    if (showGrid) {
      drawWorldGrid(g);
    }

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

    // Draw debug visualizations if enabled
    if (debugPhysics || debugColliders) {
      renderDebugOverlays(cameraTranformedG);
    }

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
    g2d.drawLine(0, -1000, 0, 1000); // Y-axis (now points up)

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
      if (com.isVisible()) {
        com.render(g);
      }
    });
  }

  /**
   * Renders entities using the camera-transformed graphics context
   */
  private void renderEntities(Graphics2D g) {
    world.findEntitiesWith(Transform.class, RenderableComponent.class).forEach(result -> {
      Transform transform = result.comp1();
      RenderableComponent renderable = result.comp2();

      // Only render if the component is visible
      if (renderable.isVisible()) {
        // Create a copy of the camera-transformed graphics for this entity
        Graphics2D entityG = (Graphics2D) g.create();

        // Apply entity's local transformations
        entityG.translate(transform.getX(), transform.getY());
        entityG.rotate(transform.getRotation());
        entityG.scale(transform.getScaleX(), transform.getScaleY());

        // Render the entity with the combined transformations (camera + entity)
        renderable.render(entityG);

        entityG.dispose();
      }
    });
  }

  /**
   * Render physics and collider debug visualizations
   */
  private void renderDebugOverlays(Graphics2D g) {
    // Physics debug rendering
    if (debugPhysics) {
      // Render physics bodies, velocities, etc.
      // Implementation depends on your physics system
    }

    // Collider debug rendering
    if (debugColliders) {
      // Outline all colliders
      world.findEntitiesWith(Transform.class, PhysicsBodyComponent.class).forEach(result -> {
        Transform transform = result.comp1();
        PhysicsBodyComponent physics = result.comp2();

        // Get a separate graphics context
        Graphics2D debugG = (Graphics2D) g.create();
        debugG.setColor(new Color(255, 0, 0, 128));
        debugG.translate(transform.getX(), transform.getY());
        debugG.rotate(transform.getRotation());

        // Draw collider outline based on shape
        if (physics.getBody() != null) {
          // Simple rectangle representation for debugging
          if (physics.getShape() != null) {
            // This can be improved to properly render different shape types
            float width = physics.getWidth();
            float height = physics.getHeight();
            debugG.drawRect((int) (-width / 2), (int) (-height / 2), (int) width, (int) height);
          }
        }

        debugG.dispose();
      });
    }
  }
}
