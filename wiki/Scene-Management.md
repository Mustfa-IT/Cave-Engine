# Scene Management

The Scene Management system in Java 2D Physics Engine provides functionality for organizing entities into separate scenes and managing scene transitions.

## Overview

Scene management allows you to:
- Group entities into logical scenes (e.g., main menu, game level, pause screen)
- Switch between scenes during runtime
- Load and unload scene resources efficiently
- Implement level transitions and state changes

## Core Components

### SceneManager

The `SceneManager` class is the central manager for all scenes:

```java
import com.engine.entity.SceneManager;
import dev.dominion.ecs.api.Dominion;

// Create a scene manager
Dominion ecs = Dominion.create();
SceneManager sceneManager = new SceneManager(ecs);
```

### EntityRegistrar

The `EntityRegistrar` interface represents a scene that can register and manage entities:

```java
import com.engine.entity.EntityRegistrar;

// Create a new scene
EntityRegistrar mainMenuScene = sceneManager.createScene("mainMenu");
EntityRegistrar gamePlayScene = sceneManager.createScene("gamePlay");
EntityRegistrar pauseScene = sceneManager.createScene("pause");
```

## Working with Scenes

### Creating and Managing Scenes

```java
// Create scenes
EntityRegistrar mainMenuScene = sceneManager.createScene("mainMenu");
EntityRegistrar gamePlayScene = sceneManager.createScene("gamePlay");

// Set active scene
sceneManager.setActiveScene("mainMenu");

// Check if a scene exists
boolean hasScene = sceneManager.hasScene("mainMenu");

// Get a scene by name
EntityRegistrar scene = sceneManager.getScene("mainMenu");

// Remove a scene
sceneManager.removeScene("mainMenu");
```

### Switching Between Scenes

```java
// Switch from main menu to gameplay
sceneManager.setActiveScene("gamePlay");

// Switch back to main menu
sceneManager.setActiveScene("mainMenu");
```

### Scene Transitions

For smoother transitions between scenes, you can implement fade effects:

```java
// Create a scene transition
public void transitionToScene(String targetSceneName) {
    // Create a black overlay for fade effect
    Entity fadeOverlay = ecs.createEntity(
        "fadeOverlay",
        new Transform(0, 0, 0, window.getWidth(), window.getHeight()),
        new RenderableComponent(new ColorRect(0, 0, 0, 0)) // Start transparent
    );

    // Fade out current scene
    Animator.animate(0, 1, 0.5, alpha -> {
        ColorRect rect = (ColorRect) fadeOverlay.get(RenderableComponent.class).getShape();
        rect.setAlpha(alpha);
    }, () -> {
        // When fade out is complete, switch scene
        sceneManager.setActiveScene(targetSceneName);

        // Fade in new scene
        Animator.animate(1, 0, 0.5, alpha -> {
            ColorRect rect = (ColorRect) fadeOverlay.get(RenderableComponent.class).getShape();
            rect.setAlpha(alpha);
        }, () -> {
            // When fade in is complete, remove the overlay
            fadeOverlay.destroy();
        });
    });
}
```

## Scene Lifecycle

Each scene can have its own lifecycle methods:

```java
public class GameScene implements EntityRegistrar {
    private final Dominion ecs;
    private final List<Entity> entities = new ArrayList<>();

    public GameScene(Dominion ecs) {
        this.ecs = ecs;
    }

    public void initialize() {
        // Create entities and set up scene
        createPlayer();
        createEnemies();
        createEnvironment();
    }

    public void activate() {
        // Called when scene becomes active
        playBackgroundMusic();
        setUpInputHandlers();
    }

    public void deactivate() {
        // Called when scene becomes inactive
        pauseBackgroundMusic();
        removeInputHandlers();
    }

    public void cleanup() {
        // Called when scene is removed
        for (Entity entity : entities) {
            entity.destroy();
        }
        entities.clear();
    }

    @Override
    public Entity registerEntity(Entity entity) {
        entities.add(entity);
        return entity;
    }
}
```

## Integrating UI with Scenes

When working with both scenes and UI elements, you'll need to connect the two systems:

```java
// Create a UI system
UISystem uiSystem = new UISystem(window, ecs);

// Create scenes
EntityRegistrar mainMenuScene = sceneManager.createScene("mainMenu");
EntityRegistrar gameScene = sceneManager.createScene("game");

// Setup main menu UI
sceneManager.setActiveScene("mainMenu");
uiSystem.setCurrentRegistrar(mainMenuScene);

// Create UI elements for main menu
Entity titleLabel = uiSystem.createLabel("My Game", 400, 100);
Entity startButton = uiSystem.createButton("Start Game", 400, 250, 200, 50);
Entity exitButton = uiSystem.createButton("Exit", 400, 350, 200, 50);

// Add click handler to switch scenes
Button startButtonComponent = (Button) startButton.get(UIComponent.class).getUi();
startButtonComponent.setOnClick(() -> {
    // Switch to game scene
    sceneManager.setActiveScene("game");
    uiSystem.setCurrentRegistrar(gameScene);

    // Setup game UI
    setupGameUI();
});
```

## Best Practices

1. **Organize scenes by function**: Create separate scenes for different parts of your game (menus, levels, etc.)
2. **Clean up resources**: When removing a scene, make sure to destroy all associated entities and resources
3. **Pause inactive systems**: When switching scenes, consider pausing systems that don't need to run
4. **Use scene transitions**: Add visual transitions when switching between scenes for a more polished experience
5. **Preload scenes**: For complex scenes, consider preloading assets to avoid hitches during transitions
