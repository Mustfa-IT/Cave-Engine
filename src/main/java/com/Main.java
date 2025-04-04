package com;

import com.engine.core.GameEngine;
import com.engine.di.DaggerEngineComponent;
import com.engine.di.EngineComponent;
import com.engine.di.EngineModule;
import com.engine.scene.TestScene;
import com.engine.scene.TestScene2;

public class Main {
  public static void main(String[] args) {
    // Use Dagger to create the engine
    // Create explicitly with builder to prevent potential issues
    EngineComponent engineComponent = DaggerEngineComponent.builder()
        .concreteModule(new EngineModule.ConcreteModule())
        .build();
    GameEngine game = engineComponent.engine();

    // Create a test scene using the entity factory
    TestScene testScene = new TestScene(game.getEntityFactory());
    TestScene2 testScene2 = new TestScene2(game.getEntityFactory());

    // Register the scene with the engine
    game.registerScene("test2", testScene2);
    game.registerScene("test", testScene);

    // Set the active scene
    game.setActiveScene("test2");

    // Start the game
    game.start();
  }
}
