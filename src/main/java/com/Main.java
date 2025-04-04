package com;

import com.engine.core.GameEngine;
import com.engine.di.DaggerEngineComponent;
import com.engine.di.EngineComponent;
import com.engine.di.EngineModule;
import com.engine.scene.TestScene;
import com.engine.scene.TestScene2;

public class Main {
  public static void main(String[] args) {
    // Use Dagger to create the engine with simplified initialization
    GameEngine game = createEngine();

    // More readable, chainable scene configuration
    game.createScene("test", () -> new TestScene(game.getEntityFactory()))
        .createScene("test2", () -> new TestScene2(game.getEntityFactory()))
        .setActiveScene("test2")
        .start();
  }

  /**
   * Creates and initializes the game engine
   * 
   * @return Configured GameEngine instance
   */
  private static GameEngine createEngine() {
    EngineComponent engineComponent = DaggerEngineComponent.builder()
        .concreteModule(new EngineModule.ConcreteModule())
        .build();
    return engineComponent.engine();
  }
}
