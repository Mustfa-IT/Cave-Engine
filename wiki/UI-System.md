# UI System Documentation

The UI System provides functionality for creating and managing user interface elements in the game engine.

## Main Components

### UISystem Class

The `UISystem` class is the central manager for all UI elements. It handles:
- Creation of UI elements (factory methods)
- Event handling (mouse clicks, movement, dragging)
- Managing element states (hover, focus, drag)
- Rendering delegation

```java
// Create a UISystem instance
UISystem uiSystem = new UISystem(gameWindow, ecsInstance);

// Register with a scene
uiSystem.setCurrentRegistrar(sceneRegistrar);
```

## Available UI Elements

### Button

Buttons can be clicked to trigger actions.

```java
// Create a button
Entity button = uiSystem.createButton("Click Me", 100, 100, 150, 40);

// Add click handler
Button buttonComponent = button.get(UIComponent.class).getUi();
buttonComponent.setOnClick(() -> System.out.println("Button clicked!"));
```

### Label

Labels display static text.

```java
// Create a label
Entity label = uiSystem.createLabel("Hello World", 100, 100);
```

### Panel

Panels are container elements that can hold other UI elements.

```java
// Create a panel
Entity panel = uiSystem.createPanel(50, 50, 300, 200);

// Add elements to the panel
Entity button = uiSystem.createButton("Panel Button", 10, 10, 100, 30);
uiSystem.addToPanel(panel, button);
```

### Slider

Sliders allow the user to select a value from a range.

```java
// Create a slider
Entity slider = uiSystem.createSlider("Volume", 100, 150, 200, 30, 0.0f, 1.0f, 0.5f);

// Add value change callback
uiSystem.setSliderCallback(slider, value -> System.out.println("New value: " + value));
```

### Debug Overlay

Debug overlays display performance metrics and debug information.

```java
// Create a debug overlay
Entity debugOverlay = uiSystem.createDebugOverlay(10, 10);
```

## Event Handling

The UISystem automatically handles mouse events for all UI elements. This includes:
- Hover effects for buttons
- Focus management
- Slider dragging
- Click events

## Advanced Usage

### Custom UI Elements

You can create custom UI elements by extending `AbstractUIElement` and implementing the required methods.

```java
public class CustomElement extends AbstractUIElement {
    public CustomElement(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    @Override
    public void render(Graphics2D g) {
        // Custom rendering code
    }

    @Override
    public void update(double deltaTime) {
        // Custom update logic
    }
}
```
