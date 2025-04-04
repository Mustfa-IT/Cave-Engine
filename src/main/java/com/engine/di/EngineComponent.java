package com.engine.di;

import javax.inject.Singleton;

import com.engine.core.GameEngine;
import com.engine.graph.RenderingSystem;
import com.engine.physics.PhysicsSystem;

import dagger.Component;

@Singleton
@Component(modules = { EngineModule.class, EngineModule.ConcreteModule.class })
public interface EngineComponent {
  GameEngine engine();

  // Expose these systems for direct access if needed
  RenderingSystem renderingSystem();

  PhysicsSystem physicsSystem();
}
