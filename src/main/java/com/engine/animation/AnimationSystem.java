package com.engine.animation;

import com.engine.components.SpriteComponent;
import com.engine.components.SpriteAnimationComponent;
import com.engine.events.EventSystem;
import com.engine.events.EventTypes;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Logger;

/**
 * System for updating sprite animations
 */
@Singleton
public class AnimationSystem {
  private static final Logger LOGGER = Logger.getLogger(AnimationSystem.class.getName());
  private final Dominion ecs;
  private final EventSystem eventSystem;

  @Inject
  public AnimationSystem(Dominion ecs, EventSystem eventSystem) {
    this.ecs = ecs;
    this.eventSystem = eventSystem;
    LOGGER.info("Animation system initialized");
  }

  /**
   * Update all animated sprites in the game
   *
   * @param deltaTime Time elapsed since last update in seconds
   */
  public void update(double deltaTime) {
    // Process all entities with both sprite and animation components
    int count = 0;
    var entities = ecs.findEntitiesWith(SpriteComponent.class, SpriteAnimationComponent.class);

    for (var result : entities) {
      Entity entity = result.entity();
      SpriteComponent sprite = result.comp1();
      SpriteAnimationComponent animation = result.comp2();

      // Skip if sprite is not visible
      if (!sprite.isVisible()) {
        continue;
      }

      // Check if animation frame changed
      int previousFrame = animation.getCurrentFrameIndex();

      // Update animation state
      animation.update(deltaTime);

      // Fire event if frame changed
      if (previousFrame != animation.getCurrentFrameIndex()) {
        eventSystem.fireEvent(EventTypes.ANIMATION_FRAME_CHANGED,
            "entity", entity,
            "animationName", animation.getCurrentAnimationName(),
            "frame", animation.getCurrentFrameIndex(),
            "isLastFrame", animation.isOnLastFrame());

        // Fire completion event if animation finished
        if (animation.isOnLastFrame() && animation.isComplete()) {
          eventSystem.fireEvent(EventTypes.ANIMATION_COMPLETE,
              "entity", entity,
              "animationName", animation.getCurrentAnimationName());
        }
      }

      // Apply current animation frame to sprite
      var frame = animation.getCurrentFrame();
      sprite.setImage(frame);

      if (frame != null) {
        count++;
      }
    }

    if (count > 0) {
      LOGGER.fine("Updated " + count + " animations");
    }
  }

  /**
   * Play an animation on a specific entity
   *
   * @param entity        Entity to animate
   * @param animationName Name of animation to play
   * @return true if successful, false if entity doesn't have required components
   *         or animation not found
   */
  public boolean playAnimation(Entity entity, String animationName) {
    if (entity == null || !entity.has(SpriteAnimationComponent.class)) {
      return false;
    }

    SpriteAnimationComponent animComp = entity.get(SpriteAnimationComponent.class);
    boolean result = animComp.play(animationName);

    if (result) {
      // Fire animation start event
      eventSystem.fireEvent(EventTypes.ANIMATION_START,
          "entity", entity,
          "animationName", animationName);
    }

    return result;
  }

  /**
   * Play an animation on a specific entity with a callback when finished
   *
   * @param entity        Entity to animate
   * @param animationName Name of animation to play
   * @param onFinish      Callback to run when animation finishes
   * @return true if successful, false if entity doesn't have required components
   *         or animation not found
   */
  public boolean playAnimation(Entity entity, String animationName, Runnable onFinish) {
    if (entity == null || !entity.has(SpriteAnimationComponent.class)) {
      return false;
    }

    SpriteAnimationComponent animComp = entity.get(SpriteAnimationComponent.class);
    return animComp.play(animationName, onFinish);
  }
}
