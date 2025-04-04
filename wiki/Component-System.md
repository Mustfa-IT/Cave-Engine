# Component System

The Component System is the foundation of the Java 2D Physics Engine, based on the Entity Component System (ECS) architectural pattern.

## Overview

Our engine uses the Dominion ECS library which provides a fast and efficient implementation of the ECS pattern. This architecture separates:

- **Entities**: Unique identifiers representing game objects
- **Components**: Raw data containers that define entity properties
- **Systems**: Logic that processes entities with specific components

## Core Components

### Transform Component

The Transform component defines the position, rotation, and scale of an entity.

```java
import com.engine.components.Transform;

// Create a transform at position (100, 200) with 0 rotation and scale 1
Transform transform = new Transform(100f, 200f, 0f, 1f, 1f);

// Add to an entity
Entity entity = ecs.createEntity("player", transform);

// Access properties
float x = transform.getX();
float y = transform.getY();
transform.setRotation(45f);
transform.setScale(2f, 2f);
```

### RenderableComponent

The RenderableComponent allows an entity to be rendered to the screen.

```java
import com.engine.components.RenderableComponent;
import com.engine.graph.Square;

// Create a renderable component with a square shape
RenderableComponent renderable = new RenderableComponent(new Square(50, 50));

// Add to an entity
entity.add(renderable);
```

### UIComponent

The UIComponent associates a UI element with an entity.

```java
import com.engine.components.UIComponent;
import com.engine.ui.Button;

// Create a UI component with a button
Button button = new Button("Click Me", 100, 100, 200, 50);
UIComponent uiComponent = new UIComponent(button);

// Add to an entity
entity.add(uiComponent);
```

### RigidBody

The RigidBody component gives an entity physical properties.

```java
import com.engine.components.RigidBody;

// Create a dynamic rigid body with mass 1.0
RigidBody rigidBody = new RigidBody(RigidBody.Type.DYNAMIC, 1.0f);

// Add to an entity
entity.add(rigidBody);

// Configure physics properties
rigidBody.setRestitution(0.7f);  // Bounciness
rigidBody.setFriction(0.3f);     // Surface friction
```

## Creating Custom Components

You can create your own components by implementing the Component interface:

```java
import com.engine.components.Component;

public class HealthComponent implements Component {
    private float currentHealth;
    private float maxHealth;

    public HealthComponent(float maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void damage(float amount) {
        currentHealth = Math.max(0, currentHealth - amount);
    }

    public void heal(float amount) {
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    public boolean isAlive() {
        return currentHealth > 0;
    }
}
```

## Working with Components

### Adding Components to Entities

```java
// Create an entity with multiple components
Entity entity = ecs.createEntity(
    "player",
    new Transform(100, 100, 0, 1, 1),
    new RenderableComponent(new Square(50, 50)),
    new RigidBody(RigidBody.Type.DYNAMIC, 1.0f),
    new HealthComponent(100)
);

// Or add components later
entity.add(new AudioComponent("player_sounds.wav"));
```

### Querying for Entities with Specific Components

```java
// Find all entities with both Transform and RigidBody components
ecs.findEntitiesWith(Transform.class, RigidBody.class).forEach(result -> {
    Transform transform = result.comp1();
    RigidBody rigidBody = result.comp2();
    Entity entity = result.entity();

    // Process physics for this entity
    updatePhysics(entity, transform, rigidBody);
});
```

## Best Practices

1. **Keep components focused**: Each component should represent a single aspect of an entity.
2. **Avoid coupling between components**: Components should not directly reference other components.
3. **Use composition over inheritance**: Create complex entities by combining simple components.
4. **Minimize component state changes**: When possible, avoid frequently changing component data.
