# Example Use Cases

This page contains examples of common use cases and patterns for the Java 2D Physics Engine.

## Basic UI Layout

```java
import com.engine.core.GameWindow;
import com.engine.ui.UISystem;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public class UILayoutExample {
    public static void main(String[] args) {
        GameWindow window = new GameWindow("UI Layout Example", 800, 600);
        Dominion ecs = Dominion.create();
        UISystem uiSystem = new UISystem(window, ecs);

        // Create a panel as container
        Entity panel = uiSystem.createPanel(50, 50, 400, 300);

        // Add label
        Entity titleLabel = uiSystem.createLabel("Settings", 10, 20);
        uiSystem.addToPanel(panel, titleLabel);

        // Add volume slider
        Entity volumeSlider = uiSystem.createSlider("Volume", 20, 60, 350, 30, 0.0f, 1.0f, 0.75f);
        uiSystem.setSliderCallback(volumeSlider, value -> System.out.println("Volume: " + value));
        uiSystem.addToPanel(panel, volumeSlider);

        // Add difficulty slider
        Entity difficultySlider = uiSystem.createSlider("Difficulty", 20, 120, 350, 30, 1.0f, 5.0f, 2.0f);
        uiSystem.setSliderCallback(difficultySlider, value -> System.out.println("Difficulty: " + (int)value));
        uiSystem.addToPanel(panel, difficultySlider);

        // Add buttons
        Entity saveButton = uiSystem.createButton("Save", 80, 220, 100, 40);
        uiSystem.addToPanel(panel, saveButton);

        Entity cancelButton = uiSystem.createButton("Cancel", 220, 220, 100, 40);
        uiSystem.addToPanel(panel, cancelButton);
    }
}
```

## Physics Simulation

```java
import com.engine.core.GameWindow;
import com.engine.physics.PhysicsSystem;
import com.engine.components.RigidBody;
import com.engine.components.Transform;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public class PhysicsExample {
    public static void main(String[] args) {
        GameWindow window = new GameWindow("Physics Example", 800, 600);
        Dominion ecs = Dominion.create();
        PhysicsSystem physicsSystem = new PhysicsSystem(ecs);

        // Create ground
        Entity ground = ecs.createEntity(
            "ground",
            new Transform(400, 550, 0, 800, 100),
            new RigidBody(RigidBody.Type.STATIC, 0)
        );

        // Create falling box
        Entity box = ecs.createEntity(
            "box",
            new Transform(400, 100, 0, 50, 50),
            new RigidBody(RigidBody.Type.DYNAMIC, 1.0f)
        );

        // Game loop
        double deltaTime = 0.016; // ~60 FPS
        while (true) {
            // Update physics
            physicsSystem.update(deltaTime);

            // Render frame
            window.clear();
            window.render();

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```

## Debug Overlay

```java
import com.engine.core.GameWindow;
import com.engine.ui.UISystem;
import com.engine.entity.EntityRegistrar;
import com.engine.entity.SceneManager;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public class DebugExample {
    public static void main(String[] args) {
        GameWindow window = new GameWindow("Debug Example", 800, 600);
        Dominion ecs = Dominion.create();
        UISystem uiSystem = new UISystem(window, ecs);

        // Create a debug overlay at the top-right corner
        Entity debugOverlay = uiSystem.createDebugOverlay(10, 10);

        // Now the debug overlay will automatically track:
        // - FPS
        // - Entity count
        // - Memory usage
        // - System performance metrics

        // Game loop
        while (true) {
            uiSystem.update(0.016);
            window.clear();
            window.render();

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```

## Component System Pattern

```java
import com.engine.components.Component;
import com.engine.core.GameWindow;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;

public class ComponentExample {
    public static void main(String[] args) {
        Dominion ecs = Dominion.create();

        // Define a custom component
        class HealthComponent implements Component {
            private float health;
            private float maxHealth;

            public HealthComponent(float maxHealth) {
                this.maxHealth = maxHealth;
                this.health = maxHealth;
            }

            public float getHealth() { return health; }
            public float getMaxHealth() { return maxHealth; }
            public void damage(float amount) { health = Math.max(0, health - amount); }
            public void heal(float amount) { health = Math.min(maxHealth, health + amount); }
        }

        // Create an entity with the custom component
        Entity player = ecs.createEntity(
            "player",
            new HealthComponent(100.0f)
        );

        // Process entities with the component
        ecs.findEntitiesWith(HealthComponent.class).forEach(result -> {
            HealthComponent health = result.comp();
            Entity entity = result.entity();

            System.out.println("Entity: " + entity.getName() +
                              ", Health: " + health.getHealth() +
                              "/" + health.getMaxHealth());
        });
    }
}
```
