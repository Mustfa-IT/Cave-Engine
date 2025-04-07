package com.engine.audio;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;

import com.engine.assets.AssetManager;
import com.engine.components.Transform;
import com.engine.events.EventSystem;
import com.engine.events.EventTypes;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Results.With2;

/**
 * System for 3D spatial audio using OpenAL through LWJGL
 */
@Singleton
public class AudioSystem {
  private static final Logger LOGGER = Logger.getLogger(AudioSystem.class.getName());

  private final EventSystem eventSystem;
  private final AssetManager assetManager;
  private final Dominion ecs;

  // OpenAL state
  private long device;
  private long context;
  private boolean initialized = false;

  // Audio state
  private final Map<Integer, AudioClip> loadedClips = new HashMap<>();
  private final List<Integer> availableSources = new ArrayList<>();
  private final int MAX_SOURCES = 32;

  // Track sources playing when muted
  private final List<Entity> pausedSources = new ArrayList<>();

  // Settings
  private boolean muted = false;
  private float masterVolume = 1.0f;

  @Inject
  public AudioSystem(EventSystem eventSystem, AssetManager assetManager, Dominion ecs) {
    this.eventSystem = eventSystem;
    this.assetManager = assetManager;
    this.ecs = ecs;
    LOGGER.info("Audio system created");
  }

  /**
   * Initialize the OpenAL system
   */
  public boolean initialize() {
    try {
      // Initialize OpenAL
      device = ALC10.alcOpenDevice((ByteBuffer) null);
      if (device == 0) {
        LOGGER.severe("Failed to open default OpenAL device");
        return false;
      }

      ALCCapabilities deviceCaps = ALC.createCapabilities(device);
      context = ALC10.alcCreateContext(device, (IntBuffer) null);
      if (context == 0) {
        LOGGER.severe("Failed to create OpenAL context");
        return false;
      }

      ALC10.alcMakeContextCurrent(context);
      AL.createCapabilities(deviceCaps);

      // Create sound sources
      for (int i = 0; i < MAX_SOURCES; i++) {
        int source = AL10.alGenSources();
        availableSources.add(source);
      }

      // Configure default listener
      AL10.alListener3f(AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
      AL10.alListener3f(AL10.AL_VELOCITY, 0.0f, 0.0f, 0.0f);

      // Default orientation (along -Z axis with Y up)
      FloatBuffer orientation = BufferUtils.createFloatBuffer(6)
          .put(new float[] { 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f });
      orientation.flip();
      AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);

      initialized = true;
      LOGGER.info("Audio system initialized with " + MAX_SOURCES + " sources");
      return true;

    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error initializing audio system", e);
      shutdown();
      return false;
    }
  }

  /**
   * Shutdown the audio system
   */
  public void shutdown() {
    if (!initialized) {
      return;
    }

    // Stop all sources
    for (int sourceId : availableSources) {
      AL10.alSourceStop(sourceId);
      AL10.alDeleteSources(sourceId);
    }
    availableSources.clear();

    // Delete all buffers
    for (AudioClip clip : loadedClips.values()) {
      AL10.alDeleteBuffers(clip.getBufferId());
    }
    loadedClips.clear();

    // Destroy OpenAL context
    if (context != 0) {
      ALC10.alcDestroyContext(context);
      context = 0;
    }

    // Close device
    if (device != 0) {
      ALC10.alcCloseDevice(device);
      device = 0;
    }

    initialized = false;
    LOGGER.info("Audio system shutdown");
  }

  /**
   * Update audio system - call once per frame
   */
  public void update(double deltaTime) {
    if (!initialized || muted) {
      return;
    }

    // Update listener position from entity with AudioListenerComponent
    updateListener();

    // Update all audio sources positions from their transforms
    updateSourcePositions();

    // Check for finished playing sources
    checkCompletedSources();
  }

  /**
   * Update the audio listener position and orientation
   */
  private void updateListener() {
    Entity listenerEntity = findAudioListener();
    if (listenerEntity == null) {
      return;
    }

    Transform transform = listenerEntity.get(Transform.class);
    if (transform == null) {
      return;
    }

    // Update position
    AL10.alListener3f(AL10.AL_POSITION, (float) transform.getX(), (float) transform.getY(), (float) transform.getZ());

    // Calculate forward vector based on yaw rotation (rotation around Y axis)
    // Forward vector points in the direction the listener is facing in XZ plane
    float rotation = (float) transform.getRotation();
    float forwardX = -(float) Math.sin(rotation);
    float forwardZ = -(float) Math.cos(rotation);

    // Create orientation buffer (forward vector, then up vector)
    FloatBuffer orientation = BufferUtils.createFloatBuffer(6)
        .put(forwardX).put(0.0f).put(forwardZ) // Forward vector
        .put(0.0f).put(1.0f).put(0.0f); // Up vector
    orientation.flip();

    AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
  }

  /**
   * Find the entity with an active AudioListenerComponent
   */
  private Entity findAudioListener() {
    var results = ecs.findEntitiesWith(AudioListenerComponent.class);
    for (var result : results) {
      AudioListenerComponent listener = result.comp();
      if (listener != null && listener.isActive()) {
        return result.entity();
      }
    }
    return null;
  }

  /**
   * Update positions of all audio sources based on their entity Transform
   */
  private void updateSourcePositions() {
    var results = ecs.findEntitiesWith(AudioSourceComponent.class, Transform.class);
    for (With2<AudioSourceComponent, Transform> result : results) {
      AudioSourceComponent source = result.comp1();
      Transform transform = result.comp2();

      if (source != null && transform != null) {
        AL10.alSource3f(source.getSourceId(), AL10.AL_POSITION,
            (float) transform.getX(), (float) transform.getY(), (float) transform.getZ());
      }
    }
  }

  /**
   * Check for sources that have finished playing
   */
  private void checkCompletedSources() {
    var results = ecs.findEntitiesWith(AudioSourceComponent.class);
    for (var result : results) {
      AudioSourceComponent source = result.comp();
      Entity entity = result.entity();

      if (source != null && source.isPlaying()) {
        int state = AL10.alGetSourcei(source.getSourceId(), AL10.AL_SOURCE_STATE);
        if (state == AL10.AL_STOPPED) {
          source.setPlaying(false);
          source.notifyCompletion();

          // Fire completion event
          eventSystem.fireEvent(EventTypes.AUDIO_PLAY_COMPLETE,
              "entity", entity,
              "source", source,
              "clip", source.getCurrentClip());
        }
      }
    }
  }

  /**
   * Create an audio source component for an entity
   */
  public AudioSourceComponent createAudioSource(Entity entity) {
    if (!initialized || entity == null) {
      return null;
    }

    // Reuse existing component if one exists
    if (entity.has(AudioSourceComponent.class)) {
      return entity.get(AudioSourceComponent.class);
    }

    // Check if we have available sources
    if (availableSources.isEmpty()) {
      LOGGER.warning("No available audio sources");
      return null;
    }

    // Get a source from the pool
    int sourceId = availableSources.remove(0);

    // Configure default source properties
    AL10.alSourcef(sourceId, AL10.AL_PITCH, 1.0f);
    AL10.alSourcef(sourceId, AL10.AL_GAIN, 1.0f);
    AL10.alSource3f(sourceId, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
    AL10.alSource3f(sourceId, AL10.AL_VELOCITY, 0.0f, 0.0f, 0.0f);
    AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);

    // Configure distance attenuation
    AL10.alSourcef(sourceId, AL10.AL_MAX_DISTANCE, 50.0f);
    AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, 5.0f);
    AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, 1.0f);

    // Create the component
    AudioSourceComponent component = new AudioSourceComponent(sourceId);
    entity.add(component);

    return component;
  }

  /**
   * Play a sound on an entity's audio source
   */
  public boolean playSound(Entity entity, String soundId, boolean loop) {
    if (!initialized || muted) {
      return false;
    }

    // Get or create audio source component
    AudioSourceComponent source = entity.get(AudioSourceComponent.class);
    if (source == null) {
      source = createAudioSource(entity);
      if (source == null) {
        return false;
      }
    }

    // Get the sound clip
    AudioClip clip = assetManager.getAsset(soundId);
    if (clip == null) {
      LOGGER.warning("Audio clip not found: " + soundId);
      return false;
    }

    // Configure the source
    int sourceId = source.getSourceId();
    AL10.alSourcei(sourceId, AL10.AL_BUFFER, clip.getBufferId());
    AL10.alSourcef(sourceId, AL10.AL_GAIN, source.getVolume() * masterVolume);
    AL10.alSourcef(sourceId, AL10.AL_PITCH, source.getPitch());
    AL10.alSourcei(sourceId, AL10.AL_LOOPING, loop ? AL10.AL_TRUE : AL10.AL_FALSE);

    // Update component state
    source.setCurrentClip(clip);
    source.setPlaying(true);
    source.setLooping(loop);

    // Start playing
    AL10.alSourcePlay(sourceId);

    // Fire event
    eventSystem.fireEvent(EventTypes.AUDIO_PLAY_START,
        "entity", entity,
        "source", source,
        "clip", clip,
        "loop", loop);

    return true;
  }

  /**
   * Stop playing sound on an entity
   */
  public void stopSound(Entity entity) {
    if (!initialized || entity == null) {
      return;
    }

    AudioSourceComponent source = entity.get(AudioSourceComponent.class);
    if (source != null && source.isPlaying()) {
      AL10.alSourceStop(source.getSourceId());
      source.setPlaying(false);

      // Fire event
      eventSystem.fireEvent(EventTypes.AUDIO_PLAY_STOP,
          "entity", entity,
          "source", source,
          "clip", source.getCurrentClip());
    }
  }

  /**
   * Create an audio listener component for an entity
   * Only one listener can be active at a time
   */
  public AudioListenerComponent createAudioListener(Entity entity) {
    if (!initialized || entity == null) {
      return null;
    }

    // Reuse existing component if one exists
    if (entity.has(AudioListenerComponent.class)) {
      return entity.get(AudioListenerComponent.class);
    }

    // Deactivate any existing listeners
    var existingListeners = ecs.findEntitiesWith(AudioListenerComponent.class);
    for (var result : existingListeners) {
      AudioListenerComponent listener = result.comp();
      listener.setActive(false);
    }

    // Create and add the new listener component
    AudioListenerComponent listener = new AudioListenerComponent();
    entity.add(listener);

    // Fire event
    eventSystem.fireEvent(EventTypes.AUDIO_LISTENER_CHANGED,
        "entity", entity,
        "listener", listener);

    return listener;
  }

  /**
   * Set whether audio is globally muted
   */
  public void setMuted(boolean muted) {
    // If transitioning from unmuted to muted
    if (!this.muted && muted && initialized) {
      pausedSources.clear();
      var sources = ecs.findEntitiesWith(AudioSourceComponent.class);
      for (var result : sources) {
        Entity entity = result.entity();
        AudioSourceComponent source = result.comp();
        if (source.isPlaying()) {
          pausedSources.add(entity);
          AL10.alSourcePause(source.getSourceId());
          source.setPlaying(false);
        }
      }
    }
    // If transitioning from muted to unmuted
    else if (this.muted && !muted && initialized) {
      for (Entity entity : pausedSources) {
        if (entity != null && entity.has(AudioSourceComponent.class)) {
          AudioSourceComponent source = entity.get(AudioSourceComponent.class);
          AudioClip clip = source.getCurrentClip();
          if (clip != null) {
            source.setPlaying(true);
            AL10.alSourcePlay(source.getSourceId());

            // Fire event
            eventSystem.fireEvent(EventTypes.AUDIO_PLAY_START,
                "entity", entity,
                "source", source,
                "clip", clip,
                "loop", source.isLooping());
          }
        }
      }
      pausedSources.clear();
    }

    this.muted = muted;
    eventSystem.fireEvent(EventTypes.AUDIO_MUTE_CHANGED, "muted", muted);
  }

  /**
   * Set the global master volume
   */
  public void setMasterVolume(float volume) {
    this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));

    // Update all playing sources with the new master volume
    if (initialized) {
      var sources = ecs.findEntitiesWith(AudioSourceComponent.class);
      for (var result : sources) {
        AudioSourceComponent source = result.comp();
        AL10.alSourcef(source.getSourceId(), AL10.AL_GAIN,
            source.getVolume() * masterVolume);
      }
    }

    eventSystem.fireEvent(EventTypes.AUDIO_VOLUME_CHANGED,
        "volume", masterVolume,
        "muted", muted);
  }

  /**
   * Register a loaded audio clip with the audio system
   * This is called by the AssetManager
   */
  public void registerAudioClip(AudioClip clip) {
    if (clip != null) {
      loadedClips.put(clip.getBufferId(), clip);
    }
  }

  /**
   * Get the current state of the audio system
   */
  public boolean isInitialized() {
    return initialized;
  }

  public boolean isMuted() {
    return muted;
  }

  public float getMasterVolume() {
    return masterVolume;
  }
}
