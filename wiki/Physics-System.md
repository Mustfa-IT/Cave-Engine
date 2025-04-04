# Physics System

The Physics System in the Java 2D Physics Engine provides functionality for simulating physical interactions between entities.

## Overview

The Physics System handles:
- Gravity and force application
- Collision detection
- Collision resolution
- Object movement and rotation

## Core Components

### RigidBody

The `RigidBody` component defines the physical properties of an entity:

```java
import com.engine.components.RigidBody;

// Create different types of rigid bodies
RigidBody dynamicBody = new RigidBody(RigidBody.Type.DYNAMIC, 1.0f); // Mass = 1.0
RigidBody staticBody = new RigidBody(RigidBody.Type.STATIC, 0.0f);   // Immovable
RigidBody kinematicBody = new RigidBody(RigidBody.Type.KINEMATIC, 0.0f); // Moves but ignores forces
```

#### RigidBody Types

- **DYNAMIC**: Affected by forces and collisions
- **STATIC**: Fixed in place, cannot move (e.g., ground, walls)
- **KINEMATIC**: Can be moved programmatically but not affected by physics forces

### Collider

The `Collider` component defines the shape used for collision detection:

```java
import com.engine.physics.BoxCollider;
import com.engine.physics.CircleCollider;

// Create a box collider
BoxCollider boxCollider = new BoxCollider(width, height);

// Create a circle collider
CircleCollider circleCollider = new CircleCollider(radius);
```

## Using the Physics System

### Setting Up Physics

```java
import com.engine.physics.PhysicsSystem;
import dev.dominion.ecs.api.Dominion;

// Create the physics system
Dominion ecs = Dominion.create();
PhysicsSystem physicsSystem = new PhysicsSystem(ecs);

// Configure gravity (optional)
physicsSystem.setGravity(0, 9.8f); // Default Earth gravity
```

### Creating Physical Objects

```java
// Create a dynamic bouncing ball
Entity ball = ecs.createEntity(
    "ball",
    new Transform(100, 100, 0, 1, 1),
    new RenderableComponent(new Circle(25)),
    new RigidBody(RigidBody.Type.DYNAMIC, 1.0f),
    new CircleCollider(25)
);

// Create static ground
Entity ground = ecs.createEntity(
    "ground",
    new Transform(400, 550, 0, 800, 20),
    new RenderableComponent(new Square(800, 20)),
    new RigidBody(RigidBody.Type.STATIC, 0),
    new BoxCollider(800, 20)
);
```

### Applying Forces

```java
// Get the rigid body component
RigidBody ballRigidBody = ball.get(RigidBody.class);

// Apply a force (e.g., when jumping)
ballRigidBody.applyForce(0, -500);

// Apply an impulse (immediate change in velocity)
ballRigidBody.applyImpulse(100, 0);

// Set velocity directly
ballRigidBody.setVelocity(5, -10);
```

### Updating Physics

The physics system should be updated every frame:

```java
// In your game loop
double deltaTime = 0.016; // 60 fps
physicsSystem.update(deltaTime);
```

## Collision Detection and Response

### Collision Events

You can register listeners to receive collision events:

```java
physicsSystem.addCollisionListener((entityA, entityB, collisionInfo) -> {
    System.out.println("Collision between " + entityA.getName() + " and " + entityB.getName());

    // Example: Damage player when hitting an enemy
    if (entityA.getName().equals("player") && entityB.getName().equals("enemy")) {
        HealthComponent health = entityA.get(HealthComponent.class);
        if (health != null) {
            health.damage(10);
        }
    }
});
```

### Collision Filtering

You can control which entities can collide with each other using collision layers and masks:

```java
// Define collision layers
final int PLAYER_LAYER = 1;
final int ENEMY_LAYER = 2;
final int WALL_LAYER = 4;
final int ITEM_LAYER = 8;

// Configure player to collide with enemies and walls, but not items
RigidBody playerRigidBody = player.get(RigidBody.class);
playerRigidBody.setCollisionLayer(PLAYER_LAYER);
playerRigidBody.setCollisionMask(ENEMY_LAYER | WALL_LAYER);

// Configure items to only collide with the player
RigidBody itemRigidBody = item.get(RigidBody.class);
itemRigidBody.setCollisionLayer(ITEM_LAYER);
itemRigidBody.setCollisionMask(PLAYER_LAYER);
```

## Advanced Physics Concepts

### Friction and Restitution

```java
RigidBody rigidBody = entity.get(RigidBody.class);

// Configure friction (0 = no friction, 1 = max friction)
rigidBody.setFriction(0.3f);

// Configure bounciness (0 = no bounce, 1 = perfect bounce)
rigidBody.setRestitution(0.7f);
```

### Continuous Collision Detection

For fast-moving objects, you can enable continuous collision detection to prevent tunneling:

```java
RigidBody bullet = entity.get(RigidBody.class);
bullet.setContinuousCollisionDetection(true);
```

## Performance Tips

1. **Use simple colliders**: Box and circle colliders are faster than complex polygons
2. **Limit dynamic objects**: Too many dynamic bodies can slow down the simulation
3. **Use appropriate broadphase**: For large worlds, consider using spatial partitioning
4. **Sleep inactive objects**: Bodies that haven't moved for a while can be temporarily removed from simulation
5. **Adjust time step**: Smaller time steps are more accurate but more CPU intensive
