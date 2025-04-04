# Java 2D Physics Engine Wiki

Welcome to the Java 2D Physics Engine Wiki! This documentation will help you understand and use the engine effectively.

## Overview

This is a 2D physics engine built in Java that provides:
- Entity Component System (ECS) architecture using Dominion ECS
- UI System with various components (buttons, sliders, panels, etc.)
- Physics simulation capabilities
- Game window management

## Quick Navigation

- [Getting Started](./GettingStarted.md) - Setup instructions and basic usage
- [UI System](./UI-System.md) - Documentation for UI components and functionality
- [Component System](./Component-System.md) - Overview of available components
- [Physics System](./Physics-System.md) - Physics simulation documentation
- [Examples](./Examples.md) - Code examples and patterns

## Installation

```java
// Add repository to your pom.xml or build.gradle
// Maven example:
<dependency>
  <groupId>com.engine</groupId>
  <artifactId>java2dphysics</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Minimal Example

```java
import com.engine.core.GameWindow;
import com.engine.ui.UISystem;
import dev.dominion.ecs.api.Dominion;

public class MinimalExample {
    public static void main(String[] args) {
        GameWindow window = new GameWindow("Physics Example", 800, 600);
        Dominion ecs = Dominion.create();

        UISystem uiSystem = new UISystem(window, ecs);
        uiSystem.createButton("Hello World", 100, 100, 200, 50);

        // Game loop would be implemented here
    }
}
```

## Contributing

Contributions are welcome! Please see our [Contributing Guide](./Contributing.md) for details.
