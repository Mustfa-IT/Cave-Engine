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
   * This converts from world coordinates to screen coordinates
   */
  public Graphics2D applyActiveCamera(Graphics2D g) {
    if (activeCamera == null)
      return g;

    CameraComponent cam = activeCamera.get(CameraComponent.class);
    Transform transform = activeCamera.get(Transform.class);

    if (cam == null || transform == null)
      return g;

    Graphics2D g2d = (Graphics2D) g.create();

    // Translate to the viewport center
    g2d.translate(cam.getViewportX() + cam.getViewportWidth() / 2.0,
        cam.getViewportY() + cam.getViewportHeight() / 2.0);

    // Scale with Y flipped to make positive Y point up
    g2d.scale(cam.getZoom(), -cam.getZoom());

    // Translate to camera position
    g2d.translate(-transform.getX(), -transform.getY());

    return g2d;
  }

  /**
   * Convert a screen coordinate to world coordinate
   *
   * @param screenX Screen X coordinate
   * @param screenY Screen Y coordinate
   * @return float[] containing worldX at index 0 and worldY at index 1
   */
  public float[] screenToWorld(float screenX, float screenY) {
    if (activeCamera == null)
      return new float[] { screenX, screenY };

    CameraComponent cam = activeCamera.get(CameraComponent.class);
    Transform transform = activeCamera.get(Transform.class);

    if (cam == null || transform == null)
      return new float[] { screenX, screenY };

    // Adjust for viewport position
    screenX -= (cam.getViewportX() + cam.getViewportWidth() / 2.0);
    // Flip Y coordinate - viewport center is (0,0), up is negative in screen space
    screenY =(float) (cam.getViewportY() + cam.getViewportHeight() / 2.0) - screenY;

    // Apply inverse zoom
    screenX /= cam.getZoom();
    screenY /= cam.getZoom();

    // Apply camera position offset
    screenX += transform.getX();
    screenY += transform.getY();

    return new float[] { screenX, screenY };
  }

  /**
   * Convert a world coordinate to screen coordinate
   *
   * @param worldX World X coordinate
   * @param worldY World Y coordinate
   * @return float[] containing screenX at index 0 and screenY at index 1
   */
  public float[] worldToScreen(float worldX, float worldY) {
    if (activeCamera == null)
      return new float[] { worldX, worldY };

    CameraComponent cam = activeCamera.get(CameraComponent.class);
    Transform transform = activeCamera.get(Transform.class);

    if (cam == null || transform == null)
      return new float[] { worldX, worldY };

    // Apply camera position offset
    worldX -= transform.getX();
    worldY -= transform.getY();

    // Apply zoom
    worldX *= cam.getZoom();
    worldY *= cam.getZoom();

    // Adjust for viewport position and flip Y
    worldX += (cam.getViewportX() + cam.getViewportWidth() / 2.0);
    worldY = (float) (cam.getViewportY() + cam.getViewportHeight() / 2.0) - worldY;

    return new float[] { worldX, worldY };
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
  }
}
