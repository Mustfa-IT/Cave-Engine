package com.engine.audio;

/**
 * Component that makes an entity the audio listener (typically the camera)
 */
public class AudioListenerComponent {
  private boolean active;
  private float masterVolume = 1.0f;

  public AudioListenerComponent() {
    this.active = true;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public float getMasterVolume() {
    return masterVolume;
  }

  public void setMasterVolume(float volume) {
    this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
  }
}
