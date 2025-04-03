package com.engine.core;

import com.engine.components.CameraComponent;
import com.engine.components.Transform;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import java.awt.Graphics2D;
import java.util.logging.Logger;

public class CameraSystem {
  private final Dominion ecs;
  private Entity activeCamera;
  private static final Logger LOGGER = Logger.getLogger(GameEngine.class.getName());

  public CameraSystem(Dominion ecs) {
    this.ecs = ecs;
  }

  /**
   * Creates a camera entity with the specified parameters
   */
  public Entity createCamera(float baseWidth, float baseHeight, float x, float y) {
    Entity camera = ecs.createEntity(
        "camera",
        new Transform(x, y, 0, 1, 1),
        new CameraComponent(baseWidth, baseHeight));

    if (activeCamera == null) {
      setActiveCamera(camera);
    }

    LOGGER.info("Created camera at position: " + x + ", " + y);
    return camera;
  }

  /**
   * Sets the active camera entity
   */
  public void setActiveCamera(Entity camera) {
    if (camera != null && camera.has(CameraComponent.class) && camera.has(Transform.class)) {
      // Deactivate current camera if exists
      if (activeCamera != null) {
        CameraComponent oldCam = activeCamera.get(CameraComponent.class);
        if (oldCam != null) {
          oldCam.setActive(false);
        }
      }

      // Set new camera as active
      activeCamera = camera;
      CameraComponent newCam = camera.get(CameraComponent.class);
      if (newCam != null) {
        newCam.setActive(true);
      }

      // Log the new active camera position
      Transform transform = camera.get(Transform.class);
      LOGGER.info("Set active camera at position: " + transform.getX() + ", " + transform.getY());
    }
  }

  /**
   * Updates viewport dimensions for all cameras
   */
  public void updateAllViewports(int screenWidth, int screenHeight) {
    ecs.findEntitiesWith(CameraComponent.class).forEach(result -> {
      result.comp().updateViewport(screenWidth, screenHeight);
    });
  }

  /**
   * Returns the current active camera entity
   */
  public Entity getActiveCamera() {
    return activeCamera;
  }

  /**
   * Applies the active camera's viewport transform to the graphics context
   */
  public Graphics2D applyActiveCamera(Graphics2D g) {
    if (activeCamera == null)
      return g;

    CameraComponent cam = activeCamera.get(CameraComponent.class);
    Transform transform = activeCamera.get(Transform.class);

    if (cam == null || transform == null)
      return g;

    // Debug output to track camera transform
    LOGGER.fine("Applying camera transform: " + transform.getX() + ", " + transform.getY() +
        " with zoom: " + cam.getZoom());

    Graphics2D g2d = (Graphics2D) g.create();

    // First, translate to the viewport center
    g2d.translate(cam.getViewportX() + cam.getViewportWidth() / 2.0,
        cam.getViewportY() + cam.getViewportHeight() / 2.0);

    // Apply zoom
    g2d.scale(cam.getZoom(), cam.getZoom());

    // Finally, translate by negative camera position to move the world
    // This is the key transformation that makes camera movement work
    g2d.translate(-transform.getX(), -transform.getY());

    return g2d;
  }

  /**
   * Moves the active camera by the specified delta
   */
  public void moveCamera(float dx, float dy) {
    if (activeCamera == null)
      return;

    Transform transform = activeCamera.get(Transform.class);
    if (transform == null)
      return;

    float newX = (float) transform.getX() + dx;
    float newY = (float) transform.getY() + dy;

    transform.setX(newX);
    transform.setY(newY);

    LOGGER.fine("Camera moved to: " + newX + ", " + newY);
  }

  /**
   * Sets the camera position directly
   */
  public void setCameraPosition(float x, float y) {
    if (activeCamera == null)
      return;

    Transform transform = activeCamera.get(Transform.class);
    if (transform == null)
      return;

    transform.setX(x);
    transform.setY(y);

    LOGGER.info("Camera position set to: " + x + ", " + y);
  }

  /**
   * Updates camera logic (e.g., following targets)
   */
  public void update(float deltaTime) {
    if (activeCamera == null)
      return;

    // Example of how to implement camera movement:
    // This could be replaced with input handling or target following

    // Log camera position periodically for debugging
    if (Math.random() < 0.01) { // Only log occasionally to avoid spam
      Transform transform = activeCamera.get(Transform.class);
      if (transform != null) {
        LOGGER.fine("Current camera position: " + transform.getX() + ", " + transform.getY());
      }
    }
  }
}
