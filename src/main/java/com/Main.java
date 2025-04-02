package com;

import com.engine.core.GameEngine;
import com.engine.scene.TestScene;

public class Main {
  public static void main(String[] args) {
    // Create and initialize the game engine
    GameEngine game = new GameEngine();

    // Create a test scene using the entity factory
    TestScene testScene = new TestScene(game.getEntityFactory());

    // Register the scene with the engine
    game.registerScene("test", testScene);

    // Initialize all scenes
    game.initializeScenes();

    // Set the active scene
    game.setActiveScene("test");

    // Start the game
    game.start();
  }
}
