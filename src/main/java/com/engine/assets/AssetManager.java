package com.engine.assets;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
import javax.inject.Singleton;

import com.engine.animation.Animation;

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
    private final String id;
    private final AssetType type;
    private final String path;

    public AssetInfo(String id, AssetType type, String path) {
      this.id = id;
      this.type = type;
      this.path = path;
    }
  }
}
