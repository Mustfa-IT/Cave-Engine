package com.engine.core;

import java.awt.Canvas;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;

import java.awt.image.BufferedImage;

import java.util.logging.Level;
import java.util.logging.Logger;


public class GameFrame extends Canvas {
  private int width, height;
  private ResizeHandler resizeHandler;

  public GameFrame() {
    this.width = 400 * 2;
    this.height = 400 * 2;
    init();
  }

  public GameFrame(String title) {
    this.width = 400 * 2;
    this.height = 400 * 2;
    init();
  }

  public GameFrame(String title, int width, int height) {
    this.width = width;
    this.height = height;
    init();
  }

  public void setOnResize(ResizeHandler handler) {
    this.resizeHandler = handler;
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        if (resizeHandler != null) {
          resizeHandler.onResize(e.getComponent().getWidth(), e.getComponent().getHeight());
        }
      }
    });
  }

  // Add simplified methods to add mouse listeners
  public void addMouseListener(MouseAdapter mouseAdapter) {
    super.addMouseListener(mouseAdapter);
  }

  public void addMouseMotionListener(MouseAdapter mouseAdapter) {
    super.addMouseMotionListener(mouseAdapter);
  }

  public void init() {
    this.setSize(width, height);
  }

  public interface ResizeHandler {
    void onResize(int width, int height);
  }

  public void initialize() {
    setVisible(true);
  }

  /**
   * Captures the current window content as an image
   *
   * @return BufferedImage of the window content or null if capture fails
   */
  public BufferedImage captureScreen() {
    try {
      Rectangle rect = this.getBounds();
      BufferedImage image = new Robot().createScreenCapture(rect);
      return image;
    } catch (Exception e) {
      Logger.getLogger(GameFrame.class.getName()).log(Level.WARNING,
          "Failed to capture screenshot", e);
      return null;
    }
  }

}
