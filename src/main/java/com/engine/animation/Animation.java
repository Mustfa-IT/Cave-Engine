package com.engine.animation;

import java.awt.image.BufferedImage;

/**
 * Represents an animation sequence of frames
 */
public class Animation {
  private String name;
  private BufferedImage[] frames;
  private float frameRate; // frames per second
  private boolean looping = true;

  /**
   * Create a new animation
   *
   * @param name      Unique name for this animation
   * @param frames    Array of frames
   * @param frameRate Frame rate in frames per second
   * @param looping   Whether the animation should loop
   */
  public Animation(String name, BufferedImage[] frames, float frameRate, boolean looping) {
    this.name = name;
    this.frames = frames;
    this.frameRate = frameRate;
    this.looping = looping;
  }

  /**
   * Create a new looping animation
   *
   * @param name      Unique name for this animation
   * @param frames    Array of frames
   * @param frameRate Frame rate in frames per second
   */
  public Animation(String name, BufferedImage[] frames, float frameRate) {
    this(name, frames, frameRate, true);
  }

  /**
   * Create an animation from a sprite sheet
   *
   * @param name        Animation name
   * @param spriteSheet The sprite sheet image
   * @param frameWidth  Width of each frame
   * @param frameHeight Height of each frame
   * @param frameCount  Number of frames
   * @param startX      Starting X position in the sprite sheet
   * @param startY      Starting Y position in the sprite sheet
   * @param columns     Number of columns in the sprite sheet
   * @param frameRate   Frame rate in frames per second
   * @param looping     Whether the animation should loop
   * @return The created animation
   */
  public static Animation fromSpriteSheet(
      String name, BufferedImage spriteSheet,
      int frameWidth, int frameHeight, int frameCount,
      int startX, int startY, int columns,
      float frameRate, boolean looping) {

    BufferedImage[] frames = new BufferedImage[frameCount];

    for (int i = 0; i < frameCount; i++) {
      int col = i % columns;
      int row = i / columns;
      int x = startX + col * frameWidth;
      int y = startY + row * frameHeight;

      // Check boundaries
      if (x + frameWidth > spriteSheet.getWidth() ||
          y + frameHeight > spriteSheet.getHeight()) {
        throw new IllegalArgumentException("Frame exceeds sprite sheet bounds");
      }

      frames[i] = spriteSheet.getSubimage(x, y, frameWidth, frameHeight);
    }

    return new Animation(name, frames, frameRate, looping);
  }

  // Getters and setters
  public String getName() {
    return name;
  }

  public BufferedImage[] getFrames() {
    return frames;
  }

  public float getFrameRate() {
    return frameRate;
  }

  public void setFrameRate(float frameRate) {
    this.frameRate = frameRate;
  }

  public boolean isLooping() {
    return looping;
  }

  public void setLooping(boolean looping) {
    this.looping = looping;
  }

  public int getFrameCount() {
    return frames.length;
  }

  public BufferedImage getFrame(int index) {
    if (index < 0 || index >= frames.length) {
      return null;
    }
    return frames[index];
  }

  public float getDuration() {
    return frames.length / frameRate;
  }
}
