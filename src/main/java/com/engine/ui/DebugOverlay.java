package com.engine.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DebugOverlay extends AbstractUIElement {
  private List<DebugStat> stats = new ArrayList<>();
  private boolean visible = true;
  private float bgOpacity = 0.7f;
  private DecimalFormat df = new DecimalFormat("#0.00");

  public DebugOverlay(float x, float y, float width, float height) {
    super(x, y, width, height);
    // Add default stats
    addStat("FPS", "0.0");
    addStat("Objects", "0");
    addStat("Memory", "0 MB");
  }

  @Override
  public void render(Graphics2D g) {
    if (!visible)
      return;

    // Save the original composite
    var originalComposite = g.getComposite();

    // Draw semi-transparent background
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, bgOpacity));
    g.setColor(new Color(0, 0, 0, 150));
    g.fillRect((int) x, (int) y, (int) width, (int) height);

    // Restore original composite for text
    g.setComposite(originalComposite);

    // Draw title
    g.setColor(Color.WHITE);
    g.setFont(new Font("Monospaced", Font.BOLD, 12));
    g.drawString("DEBUG STATISTICS", (int) x + 10, (int) y + 20);

    // Draw horizontal line
    g.drawLine((int) x + 5, (int) y + 25, (int) x + (int) width - 5, (int) y + 25);

    // Draw stats
    g.setFont(new Font("Monospaced", Font.PLAIN, 12));
    int yOffset = 45;

    for (DebugStat stat : stats) {
      g.setColor(Color.WHITE);
      g.drawString(stat.name + ":", (int) x + 10, (int) y + yOffset);
      g.setColor(new Color(100, 255, 100));
      g.drawString(stat.value, (int) x + 100, (int) y + yOffset);
      yOffset += 20;
    }
  }

  public void addStat(String name, String value) {
    stats.add(new DebugStat(name, value));
  }

  public void updateStat(String name, String value) {
    for (DebugStat stat : stats) {
      if (stat.name.equals(name)) {
        stat.value = value;
        return;
      }
    }
    // If we get here, the stat doesn't exist yet, so add it
    addStat(name, value);
  }

  public void updateStat(String name, double value) {
    updateStat(name, df.format(value));
  }

  public void updateStat(String name, int value) {
    updateStat(name, String.valueOf(value));
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public boolean isVisible() {
    return visible;
  }

  public void toggleVisibility() {
    visible = !visible;
  }

  private static class DebugStat {
    String name;
    String value;

    DebugStat(String name, String value) {
      this.name = name;
      this.value = value;
    }
  }
}
