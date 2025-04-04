package com.engine.components;

import com.engine.animation.Animation;
import java.util.HashMap;
import java.util.Map;

/**
 * Component for handling sprite animations
 */
public class SpriteAnimationComponent {
  private Map<String, Animation> animations = new HashMap<>();
  private String currentAnimationName;
  private float animationTime = 0;
  private int currentFrameIndex = 0;
  private boolean playing = true;
  private boolean finished = false;
  private Runnable onFinishCallback;
  private float playbackSpeed = 1.0f;
  private boolean onLastFrame;


  /**
   * Create a new animation component
   */
  public SpriteAnimationComponent() {
  }

  /**
   * Create an animation component with a default animation
   *
   * @param animation Default animation
   */
  public SpriteAnimationComponent(Animation animation) {
    addAnimation(animation);
    play(animation.getName());
  }

  /**
   * Add an animation to this component
   *
   * @param animation The animation to add
   */
  public void addAnimation(Animation animation) {
    animations.put(animation.getName(), animation);

    // If this is the first animation, set it as current
    if (currentAnimationName == null) {
      currentAnimationName = animation.getName();
    }
  }

  /**
   * Play the specified animation from the beginning
   *
   * @param animationName Name of the animation to play
   * @return true if successful, false if animation not found
   */
  public boolean play(String animationName) {
    if (!animations.containsKey(animationName)) {
      return false;
    }

    currentAnimationName = animationName;
    animationTime = 0;
    currentFrameIndex = 0;
    playing = true;
    finished = false;
    return true;
  }

  /**
   * Play the specified animation from the beginning
   *
   * @param animationName Name of the animation to play
   * @param onFinish      Callback to run when animation finishes (non-looping
   *                      animations only)
   * @return true if successful, false if animation not found
   */
  public boolean play(String animationName, Runnable onFinish) {
    boolean result = play(animationName);
    this.onFinishCallback = onFinish;
    return result;
  }

  /**
   * Update the animation state
   *
   * @param deltaTime Time elapsed since last update
   */
  public void update(double deltaTime) {
    if (!playing || currentAnimationName == null) {
      return;
    }

    Animation currentAnimation = animations.get(currentAnimationName);
    if (currentAnimation == null || finished) {
      return;
    }

    // Update animation time
    animationTime += deltaTime * playbackSpeed;

    // Calculate the current frame based on time
    float frameDuration = 1.0f / currentAnimation.getFrameRate();
    int frameIndex = (int) (animationTime / frameDuration);

    // Check if animation is finished
    if (frameIndex >= currentAnimation.getFrameCount()) {
      if (currentAnimation.isLooping()) {
        // Loop animation
        animationTime %= currentAnimation.getDuration();
        frameIndex %= currentAnimation.getFrameCount();
      } else {
        // Non-looping animation is done
        frameIndex = currentAnimation.getFrameCount() - 1;
        finished = true;

        // Call finish callback if one is set
        if (onFinishCallback != null) {
          onFinishCallback.run();
        }
      }
    }

    currentFrameIndex = frameIndex;
  }

  /**
   * Get the current frame to display
   *
   * @return The current frame image or null if no animation is active
   */
  public java.awt.image.BufferedImage getCurrentFrame() {
    if (currentAnimationName == null) {
      return null;
    }

    Animation currentAnimation = animations.get(currentAnimationName);
    if (currentAnimation == null) {
      return null;
    }

    return currentAnimation.getFrame(currentFrameIndex);
  }

  // Getters and setters
  public boolean isPlaying() {
    return playing;
  }

  public void setPlaying(boolean playing) {
    this.playing = playing;
  }

  public void pause() {
    playing = false;
  }

  public void resume() {
    playing = true;
  }

  public void stop() {
    playing = false;
    animationTime = 0;
    currentFrameIndex = 0;
    finished = true;
  }

  public boolean isFinished() {
    return finished;
  }

  public Animation getCurrentAnimation() {
    if (currentAnimationName == null) {
      return null;
    }
    return animations.get(currentAnimationName);
  }

  public String getCurrentAnimationName() {
    return currentAnimationName;
  }

  public float getPlaybackSpeed() {
    return playbackSpeed;
  }

  public void setPlaybackSpeed(float playbackSpeed) {
    this.playbackSpeed = playbackSpeed;
  }

  public int getCurrentFrameIndex() {
    return currentFrameIndex;
  }
  public boolean isOnLastFrame(){
    return this.onLastFrame;
  }

  public boolean isComplete() {
    return finished;
  }
}
