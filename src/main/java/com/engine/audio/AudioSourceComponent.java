package com.engine.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Component that allows an entity to emit sound
 */
public class AudioSourceComponent {
  private static final Logger LOGGER = Logger.getLogger(AudioSourceComponent.class.getName());

  private int sourceId;
  private AudioClip currentClip;
  private boolean playing;
  private boolean looping;
  private float volume = 1.0f;
  private float pitch = 1.0f;
  private float maxDistance = 50.0f;
  private float referenceDistance = 5.0f;
  private float rolloffFactor = 1.0f;
  private List<Consumer<AudioSourceComponent>> completionListeners = new ArrayList<>();

  public AudioSourceComponent(int sourceId) {
    this.sourceId = sourceId;
  }

  public int getSourceId() {
    return sourceId;
  }

  public AudioClip getCurrentClip() {
    return currentClip;
  }

  public void setCurrentClip(AudioClip clip) {
    this.currentClip = clip;
  }

  public boolean isPlaying() {
    return playing;
  }

  public void setPlaying(boolean playing) {
    this.playing = playing;
  }

  public boolean isLooping() {
    return looping;
  }

  public void setLooping(boolean looping) {
    this.looping = looping;
  }

  public float getVolume() {
    return volume;
  }

  public void setVolume(float volume) {
    this.volume = Math.max(0.0f, Math.min(1.0f, volume));
  }

  public float getPitch() {
    return pitch;
  }

  public void setPitch(float pitch) {
    this.pitch = Math.max(0.5f, Math.min(2.0f, pitch));
  }

  public float getMaxDistance() {
    return maxDistance;
  }

  public void setMaxDistance(float maxDistance) {
    this.maxDistance = maxDistance;
  }

  public float getReferenceDistance() {
    return referenceDistance;
  }

  public void setReferenceDistance(float referenceDistance) {
    this.referenceDistance = referenceDistance;
  }

  public float getRolloffFactor() {
    return rolloffFactor;
  }

  public void setRolloffFactor(float rolloffFactor) {
    this.rolloffFactor = rolloffFactor;
  }

  public void addCompletionListener(Consumer<AudioSourceComponent> listener) {
    completionListeners.add(listener);
  }

  public void removeCompletionListener(Consumer<AudioSourceComponent> listener) {
    completionListeners.remove(listener);
  }

  public void notifyCompletion() {
    for (Consumer<AudioSourceComponent> listener : completionListeners) {
      try {
        listener.accept(this);
      } catch (Exception e) {
        LOGGER.warning("Error in audio completion listener: " + e.getMessage());
      }
    }
  }

  public void clearCompletionListeners() {
    completionListeners.clear();
  }
}
