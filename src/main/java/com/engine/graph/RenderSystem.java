package com.engine.graph;

import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.util.logging.Logger;

import com.engine.components.CameraComponent;
import com.engine.components.RenderableComponent;
import com.engine.components.Transform;

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

    // Clean up and display
    g.dispose();
    bs.show();
    Toolkit.getDefaultToolkit().sync();
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

      // Occasionally log entity position for debugging
      if (Math.random() < 0.001) { // Limit to avoid excessive logging
        String entityId = result.entity().toString();
        if (entityId.startsWith("ball") || entityId.startsWith("rect")) {
          LOGGER.fine("Entity " + entityId + " at " + transform.getX() + ", " + transform.getY());
        }
      }

      entityG.dispose();
    });
  }
}
