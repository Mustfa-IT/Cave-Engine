package com.engine.assets;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;

import java.awt.image.BufferedImage;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import com.engine.animation.Animation;
import com.engine.audio.AudioClip;
// Removed redundant import to resolve collision
import com.engine.events.EventSystem;
import com.engine.events.EventTypes;

/**
 * Manages loading and caching of game assets like images, fonts, and sounds
 */
@Singleton
public class AssetManager {
  private static final Logger LOGGER = Logger.getLogger(AssetManager.class.getName());

  private final Map<String, Object> assets = new HashMap<>();
  private final Map<String, AssetInfo> assetInfo = new HashMap<>();
  private final ExecutorService asyncLoader = Executors.newSingleThreadExecutor();
  private Path basePath = Paths.get("assets");

  // Audio system reference
  private com.engine.audio.AudioSystem audioSystem;
  private EventSystem eventSystem;

  @Inject
  public AssetManager() {
    // Default constructor
  }

  @Inject
  public void setAudioSystem(com.engine.audio.AudioSystem audioSystem, EventSystem eventSystem) {
    this.audioSystem = audioSystem;
    this.eventSystem = eventSystem;
  }

  /**
   * Set the base directory for loading assets
   *
   * @param path The path to the assets directory
   */
  public void setBasePath(String path) {
    this.basePath = Paths.get(path);
    LOGGER.info("Asset base path set to: " + basePath.toAbsolutePath());
  }

  /**
   * Load an image asset
   *
   * @param id   The identifier for the asset
   * @param path The path to the image file (relative to base path)
   * @return The loaded image or null if loading failed
   */
  public BufferedImage loadImage(String id, String path) {
    if (assets.containsKey(id)) {
      return (BufferedImage) assets.get(id);
    }

    try {
      Path fullPath = basePath.resolve(path);
      BufferedImage image = ImageIO.read(fullPath.toFile());
      assets.put(id, image);
      assetInfo.put(id, new AssetInfo(id, AssetType.IMAGE, path));
      LOGGER.fine("Loaded image: " + id + " from " + path);
      return image;
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to load image: " + path, e);
      return null;
    }
  }

  /**
   * Load an image asynchronously
   *
   * @param id   The identifier for the asset
   * @param path The path to the image file (relative to base path)
   * @return A CompletableFuture that will provide the loaded image
   */
  public CompletableFuture<BufferedImage> loadImageAsync(String id, String path) {
    return CompletableFuture.supplyAsync(() -> loadImage(id, path), asyncLoader);
  }

  /**
   * Load a font asset
   *
   * @param id   The identifier for the asset
   * @param path The path to the font file (relative to base path)
   * @param size The point size of the font
   * @return The loaded font or a default font if loading failed
   */
  public Font loadFont(String id, String path, float size) {
    String cacheId = id + "_" + size;

    if (assets.containsKey(cacheId)) {
      return (Font) assets.get(cacheId);
    }

    try {
      Path fullPath = basePath.resolve(path);
      Font font = Font.createFont(Font.TRUETYPE_FONT, fullPath.toFile());
      font = font.deriveFont(size);

      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      ge.registerFont(font);

      assets.put(cacheId, font);
      assetInfo.put(cacheId, new AssetInfo(cacheId, AssetType.FONT, path));
      LOGGER.fine("Loaded font: " + id + " size " + size + " from " + path);
      return font;
    } catch (FontFormatException | IOException e) {
      LOGGER.log(Level.WARNING, "Failed to load font: " + path, e);
      return new Font(Font.SANS_SERIF, Font.PLAIN, (int) size);
    }
  }

  /**
   * Load an audio file (WAV format)
   *
   * @param id   The identifier for the asset
   * @param path The path to the audio file (relative to base path)
   * @return The loaded audio clip or null if loading failed
   */
  public AudioClip loadAudio(String id, String path) {
    if (assets.containsKey(id)) {
      return (AudioClip) assets.get(id);
    }

    if (audioSystem == null || !audioSystem.isInitialized()) {
      LOGGER.warning("Audio system not initialized when loading audio: " + id);
      return null;
    }

    try {
      Path fullPath = basePath.resolve(path);
      AudioInputStream audioStream = AudioSystem.getAudioInputStream(fullPath.toFile());
      AudioFormat format = audioStream.getFormat();

      // Check format is supported (OpenAL requires mono or stereo, 8 or 16 bit PCM)
      if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED &&
          format.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
        // Convert to PCM signed 16-bit
        format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            format.getSampleRate(),
            16,
            format.getChannels(),
            format.getChannels() * 2,
            format.getSampleRate(),
            false);
        audioStream = AudioSystem.getAudioInputStream(format, audioStream);
      }

      // Read audio data
      byte[] data = new byte[audioStream.available()];
      int bytesRead = audioStream.read(data);
      audioStream.close();

      // Create OpenAL buffer
      IntBuffer buffer = BufferUtils.createIntBuffer(1);
      AL10.alGenBuffers(buffer);
      int bufferId = buffer.get(0);

      // Determine OpenAL format based on channels and bit depth
      int openALFormat = getOpenALFormat(format);
      if (openALFormat == -1) {
        LOGGER.warning("Unsupported audio format: " + format);
        AL10.alDeleteBuffers(bufferId);
        return null;
      }

      // Create ByteBuffer from audio data
      ByteBuffer byteBuffer = BufferUtils.createByteBuffer(data.length);
      byteBuffer.put(data);
      byteBuffer.flip();

      // Fill OpenAL buffer
      AL10.alBufferData(bufferId, openALFormat, byteBuffer, (int) format.getSampleRate());

      // Calculate duration
      float durationSeconds = bytesRead
          / (format.getSampleRate() * format.getChannels() * (format.getSampleSizeInBits() / 8));

      // Create AudioClip
      AudioClip clip = new AudioClip(id, bufferId, AudioClip.AudioType.SOUND_EFFECT, durationSeconds);

      // Store in cache
      assets.put(id, clip);
      assetInfo.put(id, new AssetInfo(id, AssetType.SOUND, path));

      // Register with audio system
      if (audioSystem != null) {
        audioSystem.registerAudioClip(clip);
      }

      // Fire event
      if (eventSystem != null) {
        eventSystem.fireEvent(EventTypes.AUDIO_LOAD_COMPLETE,
            "id", id,
            "clip", clip,
            "path", path);
      }

      LOGGER.fine("Loaded audio: " + id + " from " + path + " (duration: " + durationSeconds + "s)");
      return clip;

    } catch (UnsupportedAudioFileException | IOException e) {
      LOGGER.log(Level.WARNING, "Failed to load audio: " + path, e);

      // Fire error event
      if (eventSystem != null) {
        eventSystem.fireEvent(EventTypes.AUDIO_LOAD_ERROR,
            "id", id,
            "path", path,
            "error", e.getMessage());
      }

      return null;
    }
  }

  /**
   * Load an OGG Vorbis audio file
   *
   * @param id   The identifier for the asset
   * @param path The path to the OGG file (relative to base path)
   * @return The loaded audio clip or null if loading failed
   */
  public AudioClip loadOgg(String id, String path) {
    if (assets.containsKey(id)) {
      return (AudioClip) assets.get(id);
    }

    if (audioSystem == null || !audioSystem.isInitialized()) {
      LOGGER.warning("Audio system not initialized when loading audio: " + id);
      return null;
    }

    try {
      Path fullPath = basePath.resolve(path);
      String filePath = fullPath.toAbsolutePath().toString();

      IntBuffer errorBuffer = BufferUtils.createIntBuffer(1);
      long decoder = STBVorbis.stb_vorbis_open_filename(filePath, errorBuffer, null);

      if (decoder == 0) {
        int errorCode = errorBuffer.get(0);
        LOGGER.warning("Failed to load OGG file: " + path + " (error code: " + errorCode + ")");
        if (eventSystem != null) {
          eventSystem.fireEvent(EventTypes.AUDIO_LOAD_ERROR,
              "id", id,
              "path", path,
              "error", "STBVorbis error code: " + errorCode);
        }
        return null;
      }

      try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
        STBVorbis.stb_vorbis_get_info(decoder, info);

        // Get file info
        int channels = info.channels();
        int sampleRate = info.sample_rate();

        // Get total samples and calculate duration
        int totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
        float durationSeconds = (float) totalSamples / sampleRate;

        // Create buffer for decoded data
        ShortBuffer pcm = BufferUtils.createShortBuffer(totalSamples * channels);

        // Decode the file
        int samplesPerChannel = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);

        // Close the decoder as we've read all the data
        STBVorbis.stb_vorbis_close(decoder);

        // Create OpenAL buffer
        IntBuffer buffer = BufferUtils.createIntBuffer(1);
        AL10.alGenBuffers(buffer);
        int bufferId = buffer.get(0);

        // Determine format
        int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;

        // Set buffer data
        AL10.alBufferData(bufferId, format, pcm, sampleRate);

        // Create AudioClip
        AudioClip clip = new AudioClip(id, bufferId, AudioClip.AudioType.MUSIC, durationSeconds);

        // Store in cache
        assets.put(id, clip);
        assetInfo.put(id, new AssetInfo(id, AssetType.SOUND, path));

        // Register with audio system
        if (audioSystem != null) {
          audioSystem.registerAudioClip(clip);
        }

        // Fire event
        if (eventSystem != null) {
          eventSystem.fireEvent(EventTypes.AUDIO_LOAD_COMPLETE,
              "id", id,
              "clip", clip,
              "path", path);
        }

        LOGGER.fine("Loaded OGG: " + id + " from " + path + " (duration: " + durationSeconds + "s)");
        return clip;
      }

    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to load OGG: " + path, e);

      // Fire error event
      if (eventSystem != null) {
        eventSystem.fireEvent(EventTypes.AUDIO_LOAD_ERROR,
            "id", id,
            "path", path,
            "error", e.getMessage());
      }

      return null;
    }
  }

  /**
   * Determine the OpenAL format based on the Java audio format
   */
  private int getOpenALFormat(AudioFormat format) {
    int channels = format.getChannels();
    int bits = format.getSampleSizeInBits();

    if (channels == 1) {
      if (bits == 8) {
        return AL10.AL_FORMAT_MONO8;
      } else if (bits == 16) {
        return AL10.AL_FORMAT_MONO16;
      }
    } else if (channels == 2) {
      if (bits == 8) {
        return AL10.AL_FORMAT_STEREO8;
      } else if (bits == 16) {
        return AL10.AL_FORMAT_STEREO16;
      }
    }

    return -1; // Unsupported format
  }

  /**
   * Get a previously loaded asset by ID
   *
   * @param <T> The type to cast the asset to
   * @param id  The asset identifier
   * @return The asset or null if not found
   */
  @SuppressWarnings("unchecked")
  public <T> T getAsset(String id) {
    if (!assets.containsKey(id)) {
      LOGGER.warning("Asset not found: " + id);
      return null;
    }

    try {
      return (T) assets.get(id);
    } catch (ClassCastException e) {
      LOGGER.warning("Asset type mismatch for: " + id);
      return null;
    }
  }

  /**
   * Check if an asset has been loaded
   *
   * @param id The asset identifier
   * @return true if the asset exists in the cache
   */
  public boolean hasAsset(String id) {
    return assets.containsKey(id);
  }

  /**
   * Unload an asset from memory
   *
   * @param id The asset identifier
   */
  public void unloadAsset(String id) {
    assets.remove(id);
    assetInfo.remove(id);
    LOGGER.fine("Unloaded asset: " + id);
  }

  /**
   * Clear all cached assets
   */
  public void clearAssets() {
    assets.clear();
    assetInfo.clear();
    LOGGER.info("Cleared all assets");
  }

  /**
   * Get a count of all loaded assets
   *
   * @return The number of loaded assets
   */
  public int getAssetCount() {
    return assets.size();
  }

  /**
   * Shutdown the asset manager and release resources
   */
  public void shutdown() {
    asyncLoader.shutdown();
    clearAssets();
  }

  /**
   * Load a sprite sheet and create animations from it
   *
   * @param id            Base identifier for the animations
   * @param path          Path to the sprite sheet image
   * @param frameWidth    Width of each frame
   * @param frameHeight   Height of each frame
   * @param animationData Map of animation name to [frameStart, frameCount, fps,
   *                      loop]
   * @param columns       Number of columns in the sprite sheet
   * @return Map of animation name to Animation object
   */
  public Map<String, Animation> loadSpriteSheet(String id, String path,
      int frameWidth, int frameHeight,
      Map<String, int[]> animationData,
      int columns) {
    BufferedImage sheet = loadImage(id + "_sheet", path);
    if (sheet == null) {
      return new HashMap<>();
    }

    Map<String, Animation> animations = new HashMap<>();

    for (Map.Entry<String, int[]> entry : animationData.entrySet()) {
      String animName = entry.getKey();
      int[] data = entry.getValue();

      if (data.length < 3) {
        LOGGER.warning("Invalid animation data for " + animName);
        continue;
      }

      int frameStart = data[0];
      int frameCount = data[1];
      float fps = data[2];
      boolean loop = data.length > 3 ? data[3] != 0 : true;

      Animation anim = Animation.fromSpriteSheet(
          animName, sheet, frameWidth, frameHeight, frameCount,
          (frameStart % columns) * frameWidth, (frameStart / columns) * frameHeight,
          columns, fps, loop);

      animations.put(animName, anim);
      assets.put(id + "_anim_" + animName, anim);
    }

    return animations;
  }

  /**
   * Asset type enumeration
   */
  public enum AssetType {
    IMAGE,
    SOUND,
    FONT,
    TEXT
  }

  /**
   * Asset metadata class
   */
  private class AssetInfo {
    @SuppressWarnings("unused")
    private final String id;
    @SuppressWarnings("unused")
    private final AssetType type;
    @SuppressWarnings("unused")
    private final String path;

    public AssetInfo(String id, AssetType type, String path) {
      this.id = id;
      this.type = type;
      this.path = path;
    }
  }
}
