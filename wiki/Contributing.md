# Contributing to Java 2D Physics Engine

Thank you for your interest in contributing to the Java 2D Physics Engine! This guide will help you get started with the development process.

## Getting Started

### Prerequisites

- Java 17 or higher
- Git
- Maven
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code, etc.)

### Setting Up the Development Environment

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/your-username/java2dphysics.git
   cd java2dphysics
   ```

3. Set up the upstream remote:
   ```bash
   git remote add upstream https://github.com/Mustfa-IT/java2dphysics.git
   ```

4. Import the project into your IDE
   - For IntelliJ IDEA: File > Open > Select the pom.xml file
   - For Eclipse: File > Import > Existing Maven Projects
   - For VS Code: Open folder and install Java extensions

5. Build the project:
   ```bash
   mvn clean install
   ```

## Development Workflow

### Branching Strategy

We use a simplified Git flow for development:

- `main`: The stable branch containing released code
- `develop`: The development branch where features are integrated
- `feature/*`: Feature branches for new functionality
- `bugfix/*`: Branches for bug fixes
- `release/*`: Branches for preparing releases

### Creating a Feature Branch

```bash
# Update your develop branch
git checkout develop
git pull upstream develop

# Create a feature branch
git checkout -b feature/your-feature-name
```

### Coding Standards

- Follow Java naming conventions
- Use 2-space indentation (no tabs)
- Write meaningful comments and JavaDoc
- Keep methods small and focused
- Write unit tests for new functionality

### Commit Guidelines

- Use descriptive commit messages
- Keep commits focused on a single task
- Reference issue numbers in commit messages

Example:
```
Feature: Add slider UI component

- Implement Slider class with draggable handle
- Add value change callback support
- Integrate with UISystem factory methods
- Add unit tests for slider functionality

Fixes #42
```

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=UISystemTest
```

### Writing Tests

- Write unit tests using JUnit 5
- Place test files in `src/test/java` following the same package structure
- Aim for high test coverage, especially for core functionality
- Mock external dependencies when appropriate

## Submitting Changes

### Creating a Pull Request

1. Push your changes to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

2. Go to the [original repository](https://github.com/Mustfa-IT/java2dphysics) and create a pull request:
   - Base branch: `develop`
   - Compare branch: `feature/your-feature-name` from your fork
   - Provide a clear description of your changes
   - Reference any related issues

3. Wait for a code review and address any feedback

### Code Review Process

- All code changes require at least one review
- Reviewers will check for:
  - Code quality and adherence to coding standards
  - Appropriate test coverage
  - Documentation
  - Functionality and design

## Documentation

When adding new features, please update the documentation:

- Add JavaDoc comments to all public classes and methods
- Update the wiki if necessary
- Consider adding examples to demonstrate the feature

## Building and Running

### Development Build

```bash
mvn clean compile
```

### Running Examples

```bash
mvn exec:java -Dexec.mainClass="com.engine.examples.ExampleClassName"
```

### Creating a Release Build

```bash
mvn clean package
```

## Community

- **Issues**: Use the [issue tracker](https://github.com/Mustfa-IT/java2dphysics/issues) to report bugs or suggest features
- **Discussions**: Join discussions on GitHub or in our community chat

## License

By contributing, you agree that your contributions will be licensed under the project's MIT License.
