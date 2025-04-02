package com.engine.core;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.Callable;

import javax.swing.JFrame;

public class GameWindow extends JFrame {
  private int width, height;
  private String title;
  private Callable<Void> onClose;

  public GameWindow() {
    this.width = 400 * 2;
    this.height = 400 * 2;
    this.title = "Game Engine";
    this.onClose = nullCallble();
    init();
  }

  public GameWindow(String title) {
    this.width = 400 * 2;
    this.height = 400 * 2;
    this.title = title;
    this.onClose = nullCallble();
    init();
  }

  public GameWindow(String title, int width, int height) {
    this.width = width;
    this.height = height;
    this.title = title;
    this.onClose = nullCallble();
    init();
  }

  public void setOnClose(Callable<Void> onClose) {
    this.onClose = onClose;
  }

  public void init() {
    this.setSize(width, height);
    this.setTitle(title);
    this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        try {
          onClose.call();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    });
  }

  public void initialize() {
    setVisible(true);
  }

  private Callable<Void> nullCallble() {
    return new Callable<Void>() {
      public Void call() throws Exception {
        // do something
        return null; // only possible value for a Void type
      }
    };
  }
}
