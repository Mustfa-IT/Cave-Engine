package com.engine.components;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Component that handles rendering of sprite images
 */
public class SpriteComponent {
  private BufferedImage image;
  private float width;
  private float height;
  private boolean visible = true;
  private boolean flipX = false;
  private boolean flipY = false;
  private float pivotX = 0.5f; // Default pivot at center (0-1 range)
  private float pivotY = 0.5f; // Default pivot at center (0-1 range)
  private float opacity = 1.0f; // 0 = transparent, 1 = opaque

  /**
   * Create a sprite with default size based on image dimensions
   *
   * @param image The image to render
   */
  public SpriteComponent(BufferedImage image) {
    this.image = image;
    if (image != null) {
      this.width = image.getWidth();
      this.height = image.getHeight();
    }
  }

  /**
   * Create a sprite with specified dimensions
   *
   * @param image  The image to render
   * @param width  Width for rendering
   * @param height Height for rendering
   */
  public SpriteComponent(BufferedImage image, float width, float height) {
    this.image = image;
    this.width = width;
    this.height = height;
  }

  /**
   * Render the sprite using the provided graphics context
   *
   * @param g Graphics context for rendering
   */
  public void render(Graphics2D g) {
    if (!visible || image == null) {
      return;
    }

    // Save original transform for restoration
    AffineTransform originalTransform = g.getTransform();

    // Apply opacity if needed
    if (opacity < 1.0f) {
      g.setComposite(java.awt.AlphaComposite.getInstance(
          java.awt.AlphaComposite.SRC_OVER, opacity));
    }

    // Apply flip transformations if needed
    if (flipX || flipY) {
      float pivotPointX = width * pivotX;
      float pivotPointY = height * pivotY;

      g.translate(pivotPointX, pivotPointY);
      g.scale(flipX ? -1 : 1, flipY ? -1 : 1);
      g.translate(-pivotPointX, -pivotPointY);
    }

    // Draw the image
    g.drawImage(image,
        -(int) (width * pivotX),
        -(int) (height * pivotY),
        (int) width,
        (int) height,
        null);

    // Restore original transform
    g.setTransform(originalTransform);
  }

  /**
   * Set a sub-region of the image to display (for sprite sheets)
   *
   * @param x      X position in the source image
   * @param y      Y position in the source image
   * @param width  Width of the region
   * @param height Height of the region
   */
  public void setSourceRect(int x, int y, int width, int height) {
    if (image == null) {
      return;
    }

    if (x < 0 || y < 0 || width <= 0 || height <= 0 ||
        x + width > image.getWidth() || y + height > image.getHeight()) {
      throw new IllegalArgumentException("Invalid source rectangle");
    }

    this.image = image.getSubimage(x, y, width, height);
  }

  // Getters and setters
  public BufferedImage getImage() {
    return image;
  }

  public void setImage(BufferedImage image) {
    this.image = image;
    if (image != null && width <= 0 && height <= 0) {
      this.width = image.getWidth();
      this.height = image.getHeight();
    }
  }

  public float getWidth() {
    return width;
  }

  public void setWidth(float width) {
    this.width = width;
  }

  public float getHeight() {
    return height;
  }

  public void setHeight(float height) {
    this.height = height;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public boolean isFlipX() {
    return flipX;
  }

  public void setFlipX(boolean flipX) {
    this.flipX = flipX;
  }

  public boolean isFlipY() {
    return flipY;
  }

  public void setFlipY(boolean flipY) {
    this.flipY = flipY;
  }

  public float getPivotX() {
    return pivotX;
  }

  public void setPivotX(float pivotX) {
    this.pivotX = pivotX;
  }

  public float getPivotY() {
    return pivotY;
  }

  public void setPivotY(float pivotY) {
    this.pivotY = pivotY;
  }

  public float getOpacity() {
    return opacity;
  }

  public void setOpacity(float opacity) {
    this.opacity = Math.max(0, Math.min(1, opacity));
  }
}
