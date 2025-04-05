package com.engine.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyType;

import com.engine.components.PhysicsBodyComponent;
import com.engine.components.Transform;
import com.engine.physics.BoxCollider;
import com.engine.physics.CircleCollider;
import com.engine.physics.PolygonCollider;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

/**
 * Dedicated class for debug rendering with optimizations
 */
@Singleton
public class DebugRenderer {
  private final Dominion world;


  // Debug rendering flags
  private boolean showPhysics = false;
  private boolean showColliders = false;


  // Shape caching for colliders - maps entity ID to pre-calculated shape data
  private final Map<String, Object> shapeCache = new HashMap<>();
  private boolean cacheInvalidated = true;

  @Inject
  public DebugRenderer(Dominion world) {
    this.world = world;
  }

  /**
   * Set debug rendering options
   */
  public void setDebugOptions(boolean showPhysics, boolean showColliders) {
    this.showPhysics = showPhysics;
    this.showColliders = showColliders;
    invalidateCache();
  }

  /**
   * Mark the shape cache as needing to be rebuilt
   */
  public void invalidateCache() {
    cacheInvalidated = true;
  }

  /**
   * Render debug visualizations
   */
  public void render(Graphics2D g) {
    if (!showPhysics && !showColliders) {
      return; // Early exit if debugging is disabled
    }

    // Rebuild cache if needed
    if (cacheInvalidated) {
      rebuildCache();
    }

    // Render physics debug info
    if (showPhysics) {
      renderPhysicsDebug(g);
    }

    // Render collider outlines
    if (showColliders) {
      renderColliderDebug(g);
    }
  }

  /**
   * Rebuild the shape cache
   */
  private void rebuildCache() {
    // Clear existing cache
    shapeCache.clear();

    // We'll rebuild the cache on demand when rendering
    cacheInvalidated = false;
  }

  /**
   * Render physics debug info (velocities, etc)
   */
  private void renderPhysicsDebug(Graphics2D g) {
    world.findEntitiesWith(Transform.class, PhysicsBodyComponent.class).forEach(result -> {
      Transform transform = result.comp1();
      PhysicsBodyComponent physics = result.comp2();

      // Skip if no physics body or not visible
      if (physics.getBody() == null) {
        return;
      }

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
      }

      debugG.dispose();
    });
  }

  /**
   * Render collider outlines
   */
  private void renderColliderDebug(Graphics2D g) {
    world.findEntitiesWith(Transform.class, PhysicsBodyComponent.class).forEach(result -> {
      Transform transform = result.comp1();
      PhysicsBodyComponent physics = result.comp2();

      // Skip if no physics body or not visible
      if (physics.getBody() == null) {
        return;
      }

      Entity entity = result.entity();

      // Choose color based on body properties
      Color outlineColor;
      if (physics.isTrigger()) {
        outlineColor = new Color(220, 220, 0, 150); // Yellow for triggers
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

      Graphics2D debugG = (Graphics2D) g.create();
      debugG.setColor(outlineColor);

      // Set stroke style - dashed for sensors
      if (physics.isTrigger()) {
        float[] dashPattern = { 5.0f, 5.0f };
        debugG.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));
      } else {
        debugG.setStroke(new BasicStroke(1.0f));
      }

      debugG.translate(transform.getX(), transform.getY());
      debugG.rotate(transform.getRotation());

      // Draw different shapes based on collider type
      if (entity.has(CircleCollider.class)) {
        renderCircleCollider(debugG, entity);
      } else if (entity.has(BoxCollider.class)) {
        renderBoxCollider(debugG, entity);
      } else if (entity.has(PolygonCollider.class)) {
        renderPolygonCollider(debugG, entity, physics);
      } else if (physics.getShape() != null) {
        // Simple rectangle representation for debugging
        float width = physics.getWidth();
        float height = physics.getHeight();
        debugG.drawRect((int) (-width / 2), (int) (-height / 2), (int) width, (int) height);
      }

      // Draw center point
      debugG.fillOval(-3, -3, 6, 6);

      debugG.dispose();
    });
  }

  /**
   * Render circle collider
   */
  private void renderCircleCollider(Graphics2D g, Entity entity) {
    CircleCollider circle = entity.get(CircleCollider.class);
    float radius = circle.getRadius();
    g.drawOval((int) -radius, (int) -radius, (int) (radius * 2), (int) (radius * 2));

    // Draw radius line to show rotation
    g.drawLine(0, 0, (int) radius, 0);
  }

  /**
   * Render box collider
   */
  private void renderBoxCollider(Graphics2D g, Entity entity) {
    BoxCollider box = entity.get(BoxCollider.class);
    float width = box.getWidth();
    float height = box.getHeight();
    g.drawRect((int) (-width / 2), (int) (-height / 2), (int) width, (int) height);

    // Draw diagonal to show orientation
    g.drawLine((int) (-width / 2), (int) (-height / 2), (int) (width / 2), (int) (height / 2));
  }

  /**
   * Render polygon collider
   */
  private void renderPolygonCollider(Graphics2D g, Entity entity, PhysicsBodyComponent physics) {
    PolygonCollider polygon = entity.get(PolygonCollider.class);
    Vec2[] vertices = polygon.getVertices();

    // Get cached vertices or create new ones
    int[] xPoints;
    int[] yPoints;

    String entityId = entity.getName();
    if (shapeCache.containsKey(entityId)) {
      @SuppressWarnings("unchecked")
      Map<String, int[]> cachedPoints = (Map<String, int[]>) shapeCache.get(entityId);
      xPoints = cachedPoints.get("x");
      yPoints = cachedPoints.get("y");
    } else {
      // Calculate points
      if (vertices != null && vertices.length > 0) {
        xPoints = new int[vertices.length];
        yPoints = new int[vertices.length];

        for (int i = 0; i < vertices.length; i++) {
          // Convert physics world coordinates to screen coordinates
          xPoints[i] = (int) (vertices[i].x * physics.getWorld().fromPhysicsWorld(1.0f));
          yPoints[i] = (int) (vertices[i].y * physics.getWorld().fromPhysicsWorld(1.0f));
        }

        // Cache the calculated points
        Map<String, int[]> pointsMap = new HashMap<>();
        pointsMap.put("x", xPoints);
        pointsMap.put("y", yPoints);
        shapeCache.put(entityId, pointsMap);
      } else {
        return; // No vertices to render
      }
    }

    // Draw the polygon
    g.drawPolygon(xPoints, yPoints, vertices.length);
  }

  /**
   * Get a string describing a body type
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
