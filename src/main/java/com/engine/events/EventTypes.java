package com.engine.events;

/**
 * Standardized event types used across the application
 */
public interface EventTypes {
  // Game lifecycle events
  String GAME_START = "game:start";
  String GAME_PAUSE = "game:pause";
  String GAME_RESUME = "game:resume";
  String GAME_STOP = "game:stop";

  // Scene events
  String SCENE_CHANGE = "scene:change";
  String SCENE_LOADED = "scene:loaded";
  String SCENE_UNLOADED = "scene:unloaded";

  // Physics events
  String PHYSICS_STEP = "physics:step";
  String PHYSICS_BODY_CREATED = "physics:body:created";
  String PHYSICS_BODY_DESTROYED = "physics:body:destroyed";
  String PHYSICS_GRAVITY_CHANGED = "physics:gravity:changed";

  // Collision events
  String COLLISION_BEGIN = "collision:begin";
  String COLLISION_END = "collision:end";

  // Animation events
  String ANIMATION_START = "animation:start";
  String ANIMATION_COMPLETE = "animation:complete";
  String ANIMATION_FRAME_CHANGED = "animation:frame:changed";

  // UI events
  String UI_CLICK = "ui:click";
  String UI_HOVER_BEGIN = "ui:hover:begin";
  String UI_HOVER_END = "ui:hover:end";
  String UI_VALUE_CHANGED = "ui:value:changed";

  // Entity events
  String ENTITY_CREATED = "entity:created";
  String ENTITY_DESTROYED = "entity:destroyed";
  String ENTITY_COMPONENT_ADDED = "entity:component:added";
  String ENTITY_COMPONENT_REMOVED = "entity:component:removed";
}
