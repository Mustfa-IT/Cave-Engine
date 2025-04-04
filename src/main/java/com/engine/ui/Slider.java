package com.engine.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Slider extends AbstractUIElement {
  private static final Logger LOGGER = Logger.getLogger(Slider.class.getName());
  private float minValue;
  private float maxValue;
  private float value;
  private float knobSize = 10;
  private boolean dragging = false;
  private String label;
  private List<Consumer<Float>> onValueChanged = new ArrayList<>();
  private Color trackColor = new Color(200, 200, 200);
  private Color knobColor = new Color(100, 100, 240);
  private Color labelColor = new Color(50, 50, 50);

  public Slider(String label, float x, float y, float width, float height, float minValue, float maxValue,
      float initialValue) {
    super(x, y, width, height);
    this.label = label;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.value = clamp(initialValue, minValue, maxValue);
    LOGGER.fine("Created slider: " + label);
  }

  @Override
  public void render(Graphics2D g) {
    // Draw label
    g.setColor(labelColor);
    g.setFont(new Font("Arial", Font.PLAIN, 12));
    FontMetrics metrics = g.getFontMetrics();
    g.drawString(label, x, y - 5);

    // Draw value
    String valueText = String.format("%.1f", value);
    g.drawString(valueText, x + width + 5, y + height / 2 + metrics.getAscent() / 2);

    // Draw track
    g.setColor(trackColor);
    g.setStroke(new BasicStroke(2));
    g.drawLine((int) x, (int) (y + height / 2), (int) (x + width), (int) (y + height / 2));

    // Draw knob
    float knobX = x + (value - minValue) / (maxValue - minValue) * width;
    g.setColor(knobColor);
    g.fillOval((int) (knobX - knobSize / 2), (int) (y + height / 2 - knobSize / 2),
        (int) knobSize, (int) knobSize);
  }

  public void handleMouseDrag(float mouseX, float mouseY) {
    if (dragging) {
      // Calculate new value based on mouse position
      float normalizedPosition = clamp((mouseX - x) / width, 0, 1);
      float newValue = minValue + normalizedPosition * (maxValue - minValue);
      setValue(newValue);
    }
  }

  public boolean startDrag(float mouseX, float mouseY) {
    float knobX = x + (value - minValue) / (maxValue - minValue) * width;
    if (Math.abs(mouseX - knobX) <= knobSize &&
        mouseY >= y && mouseY <= y + height) {
      dragging = true;
      return true;
    }
    return false;
  }

  public void stopDrag() {
    dragging = false;
  }

  public float getValue() {
    return value;
  }

  public void setValue(float newValue) {
    float oldValue = this.value;
    this.value = clamp(newValue, minValue, maxValue);

    if (oldValue != this.value && onValueChanged != null) {
      for (Consumer<Float> consumer : onValueChanged) {
        consumer.accept(this.value);
      }
    }
  }

  public void setOnValueChanged(Consumer<Float> callback) {
    this.onValueChanged.add(callback);
  }

  private float clamp(float value, float min, float max) {
    return Math.max(min, Math.min(max, value));
  }

  public void removeSliderCallBacks() {
    this.onValueChanged.clear();
  }
  public void removeSliderCallBack(Consumer<Float> callback){
    this.onValueChanged.remove(callback);
  }
}
