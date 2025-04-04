# Java 2D Physics Engine

This project is a simple 2D physics engine built in Java. It uses a component-based architecture and integrates with the Dominion ECS (Entity Component System) library for managing entities and components.

## Features

- **Entity-Component System (ECS):** Powered by Dominion ECS for efficient entity management.
- **Rendering System:** Supports rendering of basic shapes like squares and circles.
- **UI System:** Comprehensive UI framework with buttons, labels, panels, sliders, and debug overlays.
- **Physics Simulation:** Basic physics capabilities including collision detection and response.
- **Transformations:** Handles position, rotation, and scaling of objects.
- **Customizable Game Engine:** Includes a `GameEngine` class to manage the game loop and rendering.
- **Scene Management:** Support for multiple scenes and entity management.

## Requirements

- Java 17 or higher
- Maven for dependency management and building the project

## Documentation

For detailed documentation, please visit our [Wiki](./wiki/Home.md) which includes:

- [Getting Started Guide](./wiki/GettingStarted.md)
- [UI System Documentation](./wiki/UI-System.md)
- [Component System Documentation](./wiki/Component-System.md)
- [Physics System Documentation](./wiki/Physics-System.md)
- [Example Use Cases](./wiki/Examples.md)

## Quick Start

```java
import com.engine.core.GameWindow;
import com.engine.ui.UISystem;
import dev.dominion.ecs.api.Dominion;

public class QuickStart {
    public static void main(String[] args) {
        // Create main game window
        GameWindow window = new GameWindow("My First Game", 800, 600);

        // Initialize ECS
        Dominion ecs = Dominion.create();

        // Create UI system
        UISystem uiSystem = new UISystem(window, ecs);

        // Create a button
        uiSystem.createButton("Click Me!", 300, 250, 200, 50);

        // Simple game loop
        while (true) {
            uiSystem.update(0.016); // ~60fps
            window.clear();
            window.render();
            try { Thread.sleep(16); } catch (InterruptedException e) { }
        }
    }
}
```

## Setup and Build

1. Clone the repository:
   ```bash
   git clone https://github.com/Mustfa-IT/java2dphysics.git
   cd java2dphysics
   ```

2. Build the project using Maven:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn exec:java -Dexec.mainClass="com.Main"
   ```

## Project Structure

- **`src/main/java/com`**
  - `Main.java`: Entry point of the application.
  - `core/`: Contains the core game engine and window management classes.
  - `components/`: Defines components like `Transform`, `RenderableComponent`, and `UIComponent`.
  - `graph/`: Includes rendering-related classes like `Shape`, `Square`, and `Circle`.
  - `ui/`: Contains UI system and elements like `Button`, `Label`, `Panel`, and `Slider`.
  - `physics/`: Physics simulation system and related components.
  - `entity/`: Entity management and scene system.

- **`pom.xml`**: Maven configuration file for dependencies and build settings.

## Contributing

Contributions are welcome! Please see our [Contributing Guide](./wiki/Contributing.md) for details on how to participate in this project.

## License

This project is licensed under the MIT License. See the LICENSE file for details.
