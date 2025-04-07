package com.engine.audio;

/**
 * Represents a loaded audio clip that can be played by audio sources
 */
public class AudioClip {
  private final int bufferId;
  private final String id;
  private final AudioType type;
  private float duration;

  public enum AudioType {
    SOUND_EFFECT,
    MUSIC,
    AMBIENT,
    VOICE
  }

  public AudioClip(String id, int bufferId, AudioType type, float duration) {
    this.id = id;
    this.bufferId = bufferId;
    this.type = type;
    this.duration = duration;
  }

  public int getBufferId() {
    return bufferId;
  }

  public String getId() {
    return id;
  }

  public AudioType getType() {
    return type;
  }

  public float getDuration() {
    return duration;
  }

  @Override
  public String toString() {
    return "AudioClip[id=" + id + ", type=" + type + ", duration=" + duration + "s]";
  }
}
