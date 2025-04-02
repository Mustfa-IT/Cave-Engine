# Java 2D Physics Engine

This project is a simple 2D physics engine built in Java. It uses a component-based architecture and integrates with the Dominion ECS (Entity Component System) library for managing entities and components.

## Features

- **Entity-Component System (ECS):** Powered by Dominion ECS for efficient entity management.
- **Rendering System:** Supports rendering of basic shapes like squares and circles.
- **Transformations:** Handles position, rotation, and scaling of objects.
- **Customizable Game Engine:** Includes a `GameEngine` class to manage the game loop and rendering.

## Requirements

- Java 17 or higher
- Maven for dependency management and building the project

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
  - `components/`: Defines components like `Transform` and `RenderableComponent`.
  - `graph/`: Includes rendering-related classes like `Shape`, `Square`, and `Circle`.

- **`pom.xml`**: Maven configuration file for dependencies and build settings.

## License

This project is licensed under the MIT License. See the LICENSE file for details.
