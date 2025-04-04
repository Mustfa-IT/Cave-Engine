package com.engine.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * In-game console for displaying logs and executing commands
 */
public class Console {
  private static final Logger LOGGER = Logger.getLogger(Console.class.getName());

  private boolean visible = false;
  private final int maxLines = 15;
  private final List<String> logLines = new ArrayList<>();
  private final List<String> commandHistory = new ArrayList<>();
  private int historyIndex = -1;
  private StringBuilder currentInput = new StringBuilder();
  private int cursorPosition = 0;
  private final int cursorBlinkRate = 500; // ms
  private long lastBlinkTime = 0;
  private boolean cursorVisible = true;
  private int consoleHeight = 300;
  private float opacity = 0.85f;
  private final Map<String, ConsoleCommand> commands = new HashMap<>();
  private final GameEngine engine;

  public Console(GameEngine engine) {
    this.engine = engine;
    setupLogHandler();
    registerDefaultCommands();
    LOGGER.info("Console system initialized");
  }

  private void setupLogHandler() {
    Logger rootLogger = Logger.getLogger("");
    rootLogger.addHandler(new Handler() {
      @Override
      public void publish(LogRecord record) {
        addLogLine(record.getLevel() + ": " + record.getMessage());
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() throws SecurityException {
      }
    });
  }

  private void registerDefaultCommands() {
    registerCommand("help", args -> {
      addLogLine("=== Available Commands ===");
      for (String cmd : commands.keySet()) {
        addLogLine(cmd + ": " + commands.get(cmd).getDescription());
      }
    }, "List all available commands");

    registerCommand("clear", args -> {
      logLines.clear();
      addLogLine("Console cleared");
    }, "Clear the console");

    registerCommand("fps", args -> {
      addLogLine("Current FPS: " + String.format("%.2f", engine.getFps()));
    }, "Display current FPS");

    registerCommand("debug", args -> {
      if (args.length > 0) {
        boolean enable = args[0].equalsIgnoreCase("on") ||
            args[0].equalsIgnoreCase("true") ||
            args[0].equals("1");
        engine.setDebugDisplay(enable, enable, enable);
        addLogLine("Debug display: " + (enable ? "enabled" : "disabled"));
      } else {
        addLogLine("Usage: debug [on|off]");
      }
    }, "Toggle debug rendering");
  }

  public void registerCommand(String name, ConsoleCommand command, String description) {
    command.setDescription(description);
    commands.put(name.toLowerCase(), command);
  }

  public void registerCommand(String name, Consumer<String[]> action, String description) {
    registerCommand(name, new ConsoleCommand() {
      private String desc = description;

      @Override
      public void execute(String[] args) {
        action.accept(args);
      }

      @Override
      public String getDescription() {
        return desc;
      }

      @Override
      public void setDescription(String description) {
        this.desc = description;
      }
    }, description);
  }

  public void toggleVisibility() {
    visible = !visible;
    if (visible) {
      // Set up input capture when console is visible
    }
  }

  public boolean isVisible() {
    return visible;
  }

  public void render(Graphics2D g, int screenWidth) {
    if (!visible)
      return;

    // Save original composite for restoration
    var originalComposite = g.getComposite();

    // Draw console background with transparency
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
    g.setColor(new Color(0, 0, 0));
    g.fillRect(0, 0, screenWidth, consoleHeight);

    // Restore original composite for text
    g.setComposite(originalComposite);

    // Draw console content
    g.setFont(new Font("Monospaced", Font.PLAIN, 12));
    g.setColor(Color.WHITE);

    // Draw separator line
    g.drawLine(0, consoleHeight - 25, screenWidth, consoleHeight - 25);

    // Draw log lines
    int y = consoleHeight - 40;
    int startIdx = Math.max(0, logLines.size() - maxLines);
    for (int i = logLines.size() - 1; i >= startIdx; i--) {
      if (i < 0 || y < 15)
        break;
      g.drawString(logLines.get(i), 10, y);
      y -= 18;
    }

    // Draw input field
    g.setColor(new Color(200, 200, 200));
    g.drawString("> " + currentInput.toString(), 10, consoleHeight - 10);

    // Draw cursor
    if (cursorVisible) {
      // Calculate cursor position based on current input
      String textBeforeCursor = currentInput.substring(0, cursorPosition);
      int cursorX = 10 + g.getFontMetrics().stringWidth("> " + textBeforeCursor);
      g.drawLine(cursorX, consoleHeight - 22, cursorX, consoleHeight - 8);
    }

    // Handle cursor blinking
    long now = System.currentTimeMillis();
    if (now - lastBlinkTime > cursorBlinkRate) {
      cursorVisible = !cursorVisible;
      lastBlinkTime = now;
    }
  }

  public void addLogLine(String line) {
    logLines.add(line);
    if (logLines.size() > 100) { // Prevent unbounded growth
      logLines.remove(0);
    }
  }

  public void handleKeyInput(KeyEvent e) {
    if (!visible)
      return;

    int keyCode = e.getKeyCode();

    switch (keyCode) {
      case KeyEvent.VK_ENTER:
        executeCommand();
        break;

      case KeyEvent.VK_BACK_SPACE:
        if (cursorPosition > 0) {
          currentInput.deleteCharAt(--cursorPosition);
        }
        break;

      case KeyEvent.VK_DELETE:
        if (cursorPosition < currentInput.length()) {
          currentInput.deleteCharAt(cursorPosition);
        }
        break;

      case KeyEvent.VK_LEFT:
        if (cursorPosition > 0) {
          cursorPosition--;
        }
        break;

      case KeyEvent.VK_RIGHT:
        if (cursorPosition < currentInput.length()) {
          cursorPosition++;
        }
        break;

      case KeyEvent.VK_UP:
        navigateHistory(-1);
        break;

      case KeyEvent.VK_DOWN:
        navigateHistory(1);
        break;

      case KeyEvent.VK_HOME:
        cursorPosition = 0;
        break;

      case KeyEvent.VK_END:
        cursorPosition = currentInput.length();
        break;

      case KeyEvent.VK_ESCAPE:
        toggleVisibility();
        break;
    }
  }

  public void handleTypedKey(char c) {
    if (!visible || c < 32 || c == 127)
      return; // Ignore control chars

    currentInput.insert(cursorPosition++, c);
    cursorVisible = true;
    lastBlinkTime = System.currentTimeMillis();
  }

  // Add this method to handle KeyEvents and determine if they should be consumed
  public boolean handleKeyEvent(KeyEvent e) {
    if (!visible)
      return false;

    if (e.getID() == KeyEvent.KEY_PRESSED) {
      handleKeyInput(e);
      return true; // Consume the event
    } else if (e.getID() == KeyEvent.KEY_TYPED) {
      handleTypedKey(e.getKeyChar());
      return true; // Consume the event
    }

    return false;
  }

  private void executeCommand() {
    String commandLine = currentInput.toString().trim();
    if (!commandLine.isEmpty()) {
      // Add to history
      commandHistory.add(commandLine);
      if (commandHistory.size() > 20) {
        commandHistory.remove(0);
      }
      historyIndex = -1;

      // Log the command
      addLogLine("> " + commandLine);

      // Parse and execute
      String[] parts = commandLine.split("\\s+");
      String cmd = parts[0].toLowerCase();
      String[] args = new String[parts.length - 1];
      System.arraycopy(parts, 1, args, 0, args.length);

      if (commands.containsKey(cmd)) {
        try {
          commands.get(cmd).execute(args);
        } catch (Exception e) {
          addLogLine("Error executing command: " + e.getMessage());
        }
      } else {
        addLogLine("Unknown command: " + cmd + ". Type 'help' for commands.");
      }
    }

    // Clear input for next command
    currentInput.setLength(0);
    cursorPosition = 0;
  }

  private void navigateHistory(int direction) {
    if (commandHistory.isEmpty())
      return;

    historyIndex += direction;

    if (historyIndex >= commandHistory.size()) {
      historyIndex = commandHistory.size();
      currentInput.setLength(0);
    } else if (historyIndex >= 0) {
      currentInput.setLength(0);
      currentInput.append(commandHistory.get(historyIndex));
    } else if (historyIndex < 0) {
      historyIndex = -1;
      currentInput.setLength(0);
    }

    cursorPosition = currentInput.length();
  }

  public interface ConsoleCommand {
    void execute(String[] args);

    String getDescription();

    void setDescription(String description);
  }
}
