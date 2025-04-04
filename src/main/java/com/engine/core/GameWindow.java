package com.engine.core;

import javax.swing.JFrame;
import java.util.logging.Logger;

/**
 * Main application window that contains the game frame
 */
public class GameWindow extends JFrame {
  private static final Logger LOGGER = Logger.getLogger(GameWindow.class.getName());

  public GameWindow() {
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setResizable(true);
    setIgnoreRepaint(true); // Let the Canvas handle all painting
  }

  /**
   * Show the window and make it visible
   */
  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      pack(); // Pack components before showing
      setLocationRelativeTo(null); // Center on screen
      LOGGER.info("Game window initialized");
    }
    super.setVisible(visible);
  }
}
