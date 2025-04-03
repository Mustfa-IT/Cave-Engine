package com.engine.components;

public class CameraComponent {
  private float baseWidth;
  private float baseHeight;
  private float zoom = 1.0f;
  private int viewportX, viewportY, viewportWidth, viewportHeight;
  private boolean isActive = false;

  public CameraComponent(float baseWidth, float baseHeight) {
    this.baseWidth = baseWidth;
    this.baseHeight = baseHeight;
  }

  public float getBaseWidth() {
    return baseWidth;
  }

  public void setBaseWidth(float baseWidth) {
    this.baseWidth = baseWidth;
  }

  public float getBaseHeight() {
    return baseHeight;
  }

  public void setBaseHeight(float baseHeight) {
    this.baseHeight = baseHeight;
  }

  public float getZoom() {
    return zoom;
  }

  public void setZoom(float zoom) {
    this.zoom = zoom;
  }

  public int getViewportX() {
    return viewportX;
  }

  public int getViewportY() {
    return viewportY;
  }

  public int getViewportWidth() {
    return viewportWidth;
  }

  public int getViewportHeight() {
    return viewportHeight;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public void updateViewport(int screenWidth, int screenHeight) {
    float targetAspect = baseWidth / baseHeight;
    float screenAspect = (float) screenWidth / screenHeight;

    if (screenAspect > targetAspect) {
      zoom = (float) screenHeight / baseHeight;
      viewportWidth = (int) (baseWidth * zoom);
      viewportHeight = screenHeight;
    } else {
      zoom = (float) screenWidth / baseWidth;
      viewportWidth = screenWidth;
      viewportHeight = (int) (baseHeight * zoom);
    }

    viewportX = (screenWidth - viewportWidth) / 2;
    viewportY = (screenHeight - viewportHeight) / 2;
  }
}
