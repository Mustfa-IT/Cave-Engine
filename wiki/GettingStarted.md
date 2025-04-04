# Getting Started

This guide will help you set up and start using the Java 2D Physics Engine.

## Prerequisites

- Java 11 or higher
- Maven for dependency management
- Basic understanding of Entity Component Systems (ECS) (Just Now How they work)

## Installation

First, clone or download the repository. Then, create a new folder for your game within the project structure, referencing the engine’s code to access its functionality.

## Basic Structure

A typical application using this engine has the following components:

1. **Game Window** - Handles rendering and input
2. **ECS (Entity Component System)** - Manages game entities and components
3. **Systems** - Process entities with specific components
4. **Game Loop** - Updates all systems and handles rendering

## Creating Your First Application

Here's a minimal example to get started:

```java
import com.engine.core.GameEngine;
import com.engine.scene.SimpleScene;

public class MyGame {
  private GameEngine engine;

  public MyGame() {
    // Create the engine using DI
    engine = GameEngine.createEngine();

    // Configure the engine with fluent API
    engine.configure(config -> config
        .targetFps(60)
        .showPerformanceStats(true)
        .debugMode(false, true, true)
        .gravity(0, -9.8f)
        .windowTitle("My CaveEngine Game"))
        .createCamera(0, 0, 1.0f)
        .createScene("main", () -> new SimpleScene(engine.getEntityFactory()))
        .createDebugOverlay();

    // Set active scene
    engine.setActiveScene("main");

    // Create UI elements
    setupUI();

    // Start the game loop
    engine.start();
  }

  private void setupUI() {
    var uiSystem = engine.getUiSystem();

    // Create a button
    uiSystem.createButton("Click Me", 350, 200, 100, 40);

    // Create a label
    uiSystem.createLabel("Hello World", 350, 100);
  }

  public static void main(String[] args) {
    new MyGame();
  }
}

```

## Next Steps

Once you have your basic application running, you might want to explore:

- [Adding physics bodies](./Physics-System.md)
- [Creating custom UI components](./UI-System.md#custom-ui-elements)
- [Working with multiple scenes](./Scene-Management.md)
- [Implementing game-specific systems](./Custom-Systems.md)
