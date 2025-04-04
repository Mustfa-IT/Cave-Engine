package com.engine.graph;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyType;

import com.engine.components.CameraComponent;
import com.engine.components.PhysicsBodyComponent;
import com.engine.components.RenderableComponent;
import com.engine.components.SpriteComponent;
import com.engine.components.Transform;
import com.engine.components.UIComponent;
import com.engine.components.GameObjectComponent;
import com.engine.core.CameraSystem;
import com.engine.core.GameWindow;
import com.engine.physics.BoxCollider;
import com.engine.physics.CircleCollider;
import com.engine.physics.PolygonCollider;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

@Singleton
public class RenderSystem implements RenderingSystem {
  private final GameWindow window;
  private final Dominion world;
  private final CameraSystem cameraSystem;
  private static final Logger LOGGER = Logger.getLogger(RenderSystem.class.getName());

  // Debug visualization flags
  private boolean debugPhysics = false;
  private boolean debugColliders = false;
  private boolean showGrid = true;
  private OverlayRenderer overlayRenderer;

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
   * Set the overlay renderer (used for console, etc)
   */
  public void setOverlayRenderer(OverlayRenderer overlayRenderer) {
    this.overlayRenderer = overlayRenderer;
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

    // Render all entity types
    renderEntities(cameraTranformedG);
    renderGameObjects(cameraTranformedG);
    renderSprites(cameraTranformedG);

    // Draw debug visualizations if enabled
    if (debugPhysics || debugColliders) {
      renderDebugOverlays(cameraTranformedG);
    }

    cameraTranformedG.dispose();

    // Create a separate graphics context for UI (not affected by camera)
    Graphics2D uiG = (Graphics2D) g.create();
    renderUI(uiG);
    uiG.dispose();

    // Render console and other overlays via the interface
    if (overlayRenderer != null) {
      overlayRenderer.renderOverlays(g);
    }

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
    if (camera == null) {
      LOGGER.warning("No active camera found for grid drawing");
      return;
    }

    CameraComponent camComponent = camera.get(CameraComponent.class);
    if (camComponent == null) {
      LOGGER.warning("Camera entity has no CameraComponent");
      return;
    }

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

    try {
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
    } catch (Exception e) {
      LOGGER.warning("Error drawing grid: " + e.getMessage());
    } finally {
      g2d.dispose();
    }
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
   * Renders custom GameObjects
   */
  private void renderGameObjects(Graphics2D g) {
    world.findEntitiesWith(Transform.class, GameObjectComponent.class).forEach(result -> {
      Transform transform = result.comp1();
      GameObjectComponent gameObjectComp = result.comp2();

      // Skip if the GameObject has been destroyed
      if (gameObjectComp.isDestroyed()) {
        return;
      }

      // Create a copy of the camera-transformed graphics for this entity
      Graphics2D entityG = (Graphics2D) g.create();

      // Apply entity's local transformations
      entityG.translate(transform.getX(), transform.getY());
      entityG.rotate(transform.getRotation());
      entityG.scale(transform.getScaleX(), transform.getScaleY());

      // Let the GameObject render itself
      gameObjectComp.getGameObject().render(entityG);

      entityG.dispose();
    });
  }

  /**
   * Render sprite components
   */
  private void renderSprites(Graphics2D g) {
    int renderedCount = 0;

    for (var result : world.findEntitiesWith(Transform.class, SpriteComponent.class)) {
      Transform transform = result.comp1();
      SpriteComponent sprite = result.comp2();

      // Skip if sprite is not visible or has no image
      if (!sprite.isVisible() || sprite.getImage() == null) {
        continue;
      }

      // Create a copy of the camera-transformed graphics for this entity
      Graphics2D entityG = (Graphics2D) g.create();

      // Apply entity's local transformations
      entityG.translate(transform.getX(), transform.getY());
      entityG.rotate(transform.getRotation());

      // Fix: Apply scale with Y-component negated to correct the sprite orientation
      entityG.scale(transform.getScaleX(), -transform.getScaleY());

      // Render the sprite
      sprite.render(entityG);
      entityG.dispose();

      renderedCount++;
    }

    if (renderedCount > 0) {
      LOGGER.fine("Rendered " + renderedCount + " sprites");
    }
  }

  /**
   * Render physics and collider debug visualizations
   */
  private void renderDebugOverlays(Graphics2D g) {
    // Physics debug rendering
    if (debugPhysics) {
      // Render physics bodies, velocities, etc.
      world.findEntitiesWith(Transform.class, PhysicsBodyComponent.class).forEach(result -> {
        Transform transform = result.comp1();
        PhysicsBodyComponent physics = result.comp2();

        // Skip if no physics body
        if (physics.getBody() == null)
          return;

        Graphics2D debugG = (Graphics2D) g.create();
        debugG.translate(transform.getX(), transform.getY());
        debugG.rotate(transform.getRotation());

        // Draw velocity vector
        Vec2 vel = physics.getBody().getLinearVelocity();
        float speed = vel.length();
        if (speed > 0.1f) {
          // Scale velocity for visibility and draw the vector
          float velocityScale = 0.2f;
          float velX = vel.x * velocityScale * physics.getWorld().fromPhysicsWorld(1.0f);
          float velY = vel.y * velocityScale * physics.getWorld().fromPhysicsWorld(1.0f);

          // Draw velocity vector
          debugG.setColor(new Color(50, 200, 50, 180));
          debugG.setStroke(new BasicStroke(2.0f));
          debugG.drawLine(0, 0, (int) velX, (int) velY);

          // Draw arrowhead
          double angle = Math.atan2(velY, velX);
          debugG.drawLine((int) velX, (int) velY,
              (int) (velX - 12 * Math.cos(angle - Math.PI / 6)),
              (int) (velY - 12 * Math.sin(angle - Math.PI / 6)));
          debugG.drawLine((int) velX, (int) velY,
              (int) (velX - 12 * Math.cos(angle + Math.PI / 6)),
              (int) (velY - 12 * Math.sin(angle + Math.PI / 6)));

          // Show speed value
          debugG.setFont(new Font("SansSerif", Font.PLAIN, 10));
          debugG.drawString(String.format("%.1f", speed), (int) velX + 5, (int) velY);
        }
        // THIS ADDS TOO MUCH LOAD
        // // Display body type and sensor status
        // String bodyTypeText = getBodyTypeDescription(physics.getBody().getType());
        // String sensorText = physics.isTrigger() ? " [SENSOR]" : "";
        // debugG.setFont(new Font("SansSerif", Font.PLAIN, 10));
        // debugG.setColor(Color.WHITE);
        // debugG.drawString(bodyTypeText + sensorText, -15, -10);

        debugG.dispose();
      });
    }

    // Collider debug rendering
    if (debugColliders) {
      // Outline all colliders
      world.findEntitiesWith(Transform.class, PhysicsBodyComponent.class).forEach(result -> {
        Transform transform = result.comp1();
        PhysicsBodyComponent physics = result.comp2();
        if(transform == null || physics ==null || physics.getBody() == null) return;
        // Get a separate graphics context
        Graphics2D debugG = (Graphics2D) g.create();

        // Choose color based on body properties
        Color outlineColor;
        if (physics.isTrigger()) {
          // Sensors/triggers use a semi-transparent yellow
          outlineColor = new Color(220, 220, 0, 150);
        } else {
          // Choose color based on body type
          switch (physics.getBody().getType()) {
            case DYNAMIC:
              outlineColor = new Color(255, 100, 100, 150); // Red for dynamic
              break;
            case KINEMATIC:
              outlineColor = new Color(100, 100, 255, 150); // Blue for kinematic
              break;
            default:
              outlineColor = new Color(100, 255, 100, 150); // Green for static
          }
        }
        debugG.setColor(outlineColor);

        // Set stroke style - dashed for sensors
        if (physics.isTrigger()) {
          float[] dashPattern = { 5.0f, 5.0f };
          debugG
              .setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));
        } else {
          debugG.setStroke(new BasicStroke(1.0f));
        }

        debugG.translate(transform.getX(), transform.getY());
        debugG.rotate(transform.getRotation());

        // Draw collider outline based on shape
        if (physics.getBody() != null) {
          Entity entity = result.entity();

          // Draw different shapes based on collider type
          if (entity.has(CircleCollider.class)) {
            CircleCollider circle = entity.get(CircleCollider.class);
            float radius = circle.getRadius();
            debugG.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));

            // Draw radius line to show rotation
            debugG.drawLine(0, 0, (int) radius, 0);
          } else if (entity.has(BoxCollider.class)) {
            BoxCollider box = entity.get(BoxCollider.class);
            float width = box.getWidth();
            float height = box.getHeight();
            debugG.drawRect((int) (-width / 2), (int) (-height / 2), (int) width, (int) height);

            // Draw diagonal to show orientation
            debugG.drawLine((int) (-width / 2), (int) (-height / 2), (int) (width / 2), (int) (height / 2));
          } else if (entity.has(PolygonCollider.class)) {
            PolygonCollider polygon = entity.get(PolygonCollider.class);
            Vec2[] vertices = polygon.getVertices();
            if (vertices != null && vertices.length > 0) {
              int[] xPoints = new int[vertices.length];
              int[] yPoints = new int[vertices.length];

              for (int i = 0; i < vertices.length; i++) {
                // Convert physics world coordinates to screen coordinates
                xPoints[i] = (int) (vertices[i].x * physics.getWorld().fromPhysicsWorld(1.0f));
                yPoints[i] = (int) (vertices[i].y * physics.getWorld().fromPhysicsWorld(1.0f));
              }

              debugG.drawPolygon(xPoints, yPoints, vertices.length);
            }
          } else {
            // Simple rectangle representation for debugging
            if (physics.getShape() != null) {
              float width = physics.getWidth();
              float height = physics.getHeight();
              debugG.drawRect((int) (-width / 2), (int) (-height / 2), (int) width, (int) height);
            }
          }

          // Draw center point
          debugG.fillOval(-3, -3, 6, 6);

          // Draw collision category/mask info if in extended debug mode
          if (debugPhysics) {
            // Display collision filtering info
            // TOO MUCH LOAD
            // String filterInfo = String.format("C:%04X M:%04X",
            //     physics.getCollisionCategory(),
            //     physics.getCollisionMask());
            // debugG.setFont(new Font("Monospaced", Font.PLAIN, 10));
            // debugG.setColor(Color.BLACK);
            // debugG.drawString(filterInfo, -20, 20);
          }
        }

        debugG.dispose();
      });
    }
  }

  /**
   * Get a human-readable description of a Box2D body type
   *
   * @param bodyType The Box2D body type
   * @return A readable string description
   */
  private String getBodyTypeDescription(BodyType bodyType) {
    switch (bodyType) {
      case DYNAMIC:
        return "Dynamic";
      case STATIC:
        return "Static";
      case KINEMATIC:
        return "Kinematic";
      default:
        return "Unknown";
    }
  }
}
