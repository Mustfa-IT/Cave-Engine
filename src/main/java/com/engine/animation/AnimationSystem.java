package com.engine.animation;

import com.engine.components.SpriteComponent;
import com.engine.components.SpriteAnimationComponent;
import dev.dominion.ecs.api.Dominion;

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

  @Inject
  public AnimationSystem(Dominion ecs) {
    this.ecs = ecs;
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
      SpriteComponent sprite = result.comp1();
      SpriteAnimationComponent animation = result.comp2();

      // Skip if sprite is not visible
      if (!sprite.isVisible()) {
        continue;
      }

      // Update animation state
      animation.update(deltaTime);

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
  public boolean playAnimation(dev.dominion.ecs.api.Entity entity, String animationName) {
    if (entity == null || !entity.has(SpriteAnimationComponent.class)) {
      return false;
    }

    SpriteAnimationComponent animComp = entity.get(SpriteAnimationComponent.class);
    return animComp.play(animationName);
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
  public boolean playAnimation(dev.dominion.ecs.api.Entity entity, String animationName, Runnable onFinish) {
    if (entity == null || !entity.has(SpriteAnimationComponent.class)) {
      return false;
    }

    SpriteAnimationComponent animComp = entity.get(SpriteAnimationComponent.class);
    return animComp.play(animationName, onFinish);
  }
}
