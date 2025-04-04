# Getting Started

This guide will help you set up and start using the Java 2D Physics Engine.

## Prerequisites

- Java 11 or higher
- Maven or Gradle for dependency management
- Basic understanding of Entity Component Systems (ECS)

## Installation

### Maven

Add the following to your `pom.xml`:

```xml
<dependency>
  <groupId>com.engine</groupId>
  <artifactId>java2dphysics</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
implementation 'com.engine:java2dphysics:1.0.0'
```

## Basic Structure

A typical application using this engine has the following components:

1. **Game Window** - Handles rendering and input
2. **ECS (Entity Component System)** - Manages game entities and components
3. **Systems** - Process entities with specific components
4. **Game Loop** - Updates all systems and handles rendering

## Creating Your First Application

Here's a minimal example to get started:

```java
import com.engine.core.GameWindow;
import com.engine.ui.UISystem;
import com.engine.entity.EntityRegistrar;
import com.engine.entity.SceneManager;
import dev.dominion.ecs.api.Dominion;

public class MyGame {
    private GameWindow window;
    private Dominion ecs;
    private SceneManager sceneManager;
    private UISystem uiSystem;

    public MyGame() {
        // Create window
        window = new GameWindow("My Physics Game", 800, 600);

        // Create ECS
        ecs = Dominion.create();

        // Create scene manager
        sceneManager = new SceneManager(ecs);

        // Create UI system
        uiSystem = new UISystem(window, ecs);

        // Set up first scene
        setupMainScene();

        // Start game loop
        gameLoop();
    }

    private void setupMainScene() {
        EntityRegistrar mainScene = sceneManager.createScene("main");
        uiSystem.setCurrentRegistrar(mainScene);

        // Create UI elements
        uiSystem.createLabel("Hello World", 350, 100);
        uiSystem.createButton("Click Me", 350, 200, 100, 40);
    }

    private void gameLoop() {
        long lastTime = System.nanoTime();
        double delta = 0;

        while (true) {
            long now = System.nanoTime();
            delta = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            // Update systems
            uiSystem.update(delta);

            // Render
            window.clear();
            window.render();

            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
