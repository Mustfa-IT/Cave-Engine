package com;

import com.engine.core.GameEngine;
import com.engine.scene.TestScene;
import com.engine.scene.TestScene2;

public class Main {
  public static void main(String[] args) {
    // Create and initialize the game engine
    GameEngine game = new GameEngine();

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
