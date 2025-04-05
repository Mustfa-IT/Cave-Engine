package com.engine.util;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.engine.particles.Particle;

/**
 * A quadtree implementation optimized for particles.
 * Provides much faster spatial queries than a regular grid.
 */
public class ParticleQuadTree {
  private static final int DEFAULT_MAX_OBJECTS = 10;
  private static final int DEFAULT_MAX_LEVELS = 5;

  private final int maxObjects;
  private final int maxLevels;
  private final int level;
  private final Rectangle bounds;
  private final ParticleQuadTree[] nodes;
  private final List<Particle> particles;

  // Add concurrency control for thread safety
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  // Boundary values for faster access
  private final float minX, minY, maxX, maxY, midX, midY;

  /**
   * Create a new quad tree
   *
   * @param x          X position of the bounds
   * @param y          Y position of the bounds
   * @param width      Width of the bounds
   * @param height     Height of the bounds
   * @param maxObjects Maximum objects per node before splitting
   * @param maxLevels  Maximum depth of the tree
   */
  public ParticleQuadTree(float x, float y, float width, float height, int maxObjects, int maxLevels) {
    this.maxObjects = maxObjects;
    this.maxLevels = maxLevels;
    this.level = 0;
    this.bounds = new Rectangle((int) x, (int) y, (int) width, (int) height);
    this.nodes = new ParticleQuadTree[4];
    this.particles = new ArrayList<>(Math.min(1000, maxObjects)); // Cap initial capacity

    // Cache boundary values for faster access
    this.minX = x;
    this.minY = y;
    this.maxX = x + width;
    this.maxY = y + height;
    this.midX = x + width / 2;
    this.midY = y + height / 2;
  }

  // Private constructor for child nodes
  private ParticleQuadTree(float x, float y, float width, float height, int level, int maxObjects, int maxLevels) {
    this.maxObjects = maxObjects;
    this.maxLevels = maxLevels;
    this.level = level;
    this.bounds = new Rectangle((int) x, (int) y, (int) width, (int) height);
    this.nodes = new ParticleQuadTree[4];
    this.particles = new ArrayList<>(Math.min(500, maxObjects)); // Smaller capacity for child nodes

    // Cache boundary values for faster access
    this.minX = x;
    this.minY = y;
    this.maxX = x + width;
    this.maxY = y + height;
    this.midX = x + width / 2;
    this.midY = y + height / 2;
  }

  /**
   * Clear the quad tree
   */
  public void clear() {
    lock.writeLock().lock();
    try {
      particles.clear();

      for (int i = 0; i < nodes.length; i++) {
        if (nodes[i] != null) {
          nodes[i].clear();
          nodes[i] = null;
        }
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Split the node into four sub-nodes
   */
  private void split() {
    // Don't split if we're at max depth
    if (level >= maxLevels - 1) {
      return;
    }

    float subWidth = (float) Math.max(1.0, (maxX - minX) / 2);
    float subHeight = (float) Math.max(1.0, (maxY - minY) / 2);
    float x = (float) minX;
    float y = (float) minY;

    try {
      // Create all nodes at once to ensure proper initialization
      nodes[0] = new ParticleQuadTree(x + subWidth, y, subWidth, subHeight, level + 1, maxObjects, maxLevels);
      nodes[1] = new ParticleQuadTree(x, y, subWidth, subHeight, level + 1, maxObjects, maxLevels);
      nodes[2] = new ParticleQuadTree(x, y + subHeight, subWidth, subHeight, level + 1, maxObjects, maxLevels);
      nodes[3] = new ParticleQuadTree(x + subWidth, y + subHeight, subWidth, subHeight, level + 1, maxObjects,
          maxLevels);
    } catch (Exception e) {
      // If there's any error during node creation, ensure no partially initialized
      // state
      for (int i = 0; i < 4; i++) {
        nodes[i] = null;
      }
      throw e; // Rethrow to be caught and handled by caller
    }
  }

  /**
   * Get the index of the node that would contain the object
   *
   * @param x X position
   * @param y Y position
   * @return Index of the node (0-3) or -1 if it can't be determined
   */
  private int getIndex(float x, float y) {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isInfinite(x) || Float.isInfinite(y)) {
      return -1;
    }

    try {
      // Using cached mid values for performance
      boolean isTop = y < midY;
      boolean isLeft = x < midX;

      // Simple binary decision tree
      if (isTop) {
        return isLeft ? 1 : 0;
      } else {
        return isLeft ? 2 : 3;
      }
    } catch (Exception e) {
      // Failsafe
      return -1;
    }
  }

  /**
   * Insert a particle into the quad tree
   *
   * @param particle The particle to insert
   */
  public void insert(Particle particle) {
    // Safety checks
    if (particle == null || !particle.isActive()) {
      return;
    }

    lock.writeLock().lock();
    try {
      insertInternal(particle);
    } catch (Exception e) {
      System.err.println("Error in ParticleQuadTree.insert: " + e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Internal implementation of particle insertion
   */
  private void insertInternal(Particle particle) {
    // Skip inactive particles
    if (particle == null || !particle.isActive()) {
      return;
    }

    float x = particle.getX();
    float y = particle.getY();

    // Skip particles with invalid coordinates
    if (Float.isNaN(x) || Float.isNaN(y)) {
      return;
    }

    // Fast check if the point is within bounds
    if (x < minX || x > maxX || y < minY || y > maxY) {
      return;
    }

    // If we have subnodes, insert into the appropriate one
    if (nodes[0] != null) {
      int index = getIndex(x, y);
      if (index >= 0 && index < nodes.length && nodes[index] != null) {
        nodes[index].insertInternal(particle);
        return;
      }
    }

    // Add to this node
    particles.add(particle);

    // Split if needed and not at max depth
    if (particles.size() > maxObjects && level < maxLevels - 1) {
      if (nodes[0] == null) {
        try {
          split();
        } catch (Exception e) {
          // If splitting failed, just keep particles at this level
          System.err.println("Failed to split quad tree node: " + e);
          return;
        }
      }

      // Only distribute if split was successful
      if (nodes[0] != null) {
        redistributeParticles();
      }
    }
  }

  /**
   * Move existing particles to child nodes after a split
   */
  private void redistributeParticles() {
    // Safety check - ensure all nodes exist
    for (int i = 0; i < 4; i++) {
      if (nodes[i] == null) {
        return; // Abort if any node is null
      }
    }

    // Iterate backwards to avoid issues when removing elements
    for (int i = particles.size() - 1; i >= 0; i--) {
      // Safety check for index
      if (i >= particles.size()) {
        continue;
      }

      Particle p = particles.get(i);
      if (p == null || !p.isActive()) {
        particles.remove(i);
        continue;
      }

      int index = getIndex(p.getX(), p.getY());
      if (index >= 0 && index < nodes.length && nodes[index] != null) {
        // Remove from this node and add to child
        particles.remove(i);
        nodes[index].insertInternal(p);
      }
    }
  }

  /**
   * Efficiently insert a batch of particles
   *
   * @param particleList List of particles to insert
   */
  public void insertParticles(List<Particle> particleList) {
    // Skip empty lists
    if (particleList == null || particleList.isEmpty()) {
      return;
    }

    lock.writeLock().lock();
    try {
      // Safely get a defensive copy of the particle list
      List<Particle> safeList = new ArrayList<>(particleList.size());
      for (Particle p : particleList) {
        if (p != null && p.isActive()) {
          safeList.add(p);
        }
      }

      // Skip if no active particles
      if (safeList.isEmpty()) {
        return;
      }

      // For small batches, just use regular insert
      if (safeList.size() < 20) {
        for (Particle p : safeList) {
          insertInternal(p);
        }
        return;
      }

      // Filter particles to only those in bounds
      List<Particle> inBoundsParticles = new ArrayList<>();
      for (Particle p : safeList) {
        float x = p.getX();
        float y = p.getY();

        // Skip invalid coordinates
        if (Float.isNaN(x) || Float.isNaN(y)) {
          continue;
        }

        if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
          inBoundsParticles.add(p);
        }
      }

      // If nothing is in bounds, skip
      if (inBoundsParticles.isEmpty()) {
        return;
      }

      // If we're at a leaf node or few enough particles, just add them all
      if (level >= maxLevels - 1 || inBoundsParticles.size() <= maxObjects) {
        particles.addAll(inBoundsParticles);
        return;
      }

      // Otherwise, ensure we have split and distribute
      if (nodes[0] == null) {
        try {
          split();
        } catch (Exception e) {
          // If splitting fails, just add particles to this node
          particles.addAll(inBoundsParticles);
          System.err.println("Failed to split quad tree node during batch insert: " + e);
          return;
        }
      }

      // Verify all nodes exist before proceeding
      for (int i = 0; i < nodes.length; i++) {
        if (nodes[i] == null) {
          // If any node is missing, just add all particles here
          particles.addAll(inBoundsParticles);
          return;
        }
      }

      // Group particles by quadrant for batch insertion
      @SuppressWarnings("unchecked")
      List<Particle>[] quadrantParticles = new List[4];
      for (int i = 0; i < 4; i++) {
        quadrantParticles[i] = new ArrayList<>();
      }

      // Sort particles into quadrants
      for (Particle p : inBoundsParticles) {
        int index = getIndex(p.getX(), p.getY());
        if (index >= 0 && index < 4) {
          quadrantParticles[index].add(p);
        } else {
          // If can't determine quadrant, keep it at this level
          particles.add(p);
        }
      }

      // Insert each quadrant's particles in batch
      for (int i = 0; i < 4; i++) {
        if (!quadrantParticles[i].isEmpty()) {
          nodes[i].insertParticles(quadrantParticles[i]);
        }
      }
    } catch (Exception e) {
      System.err.println("Error in ParticleQuadTree.insertParticles: " + e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Find all particles in a rectangle
   *
   * @param x      X position
   * @param y      Y position
   * @param width  Width of the rectangle
   * @param height Height of the rectangle
   * @return List of particles in the rectangle
   */
  public List<Particle> queryRange(float x, float y, float width, float height) {
    List<Particle> result = new ArrayList<>();

    lock.readLock().lock();
    try {
      // Early out if range is completely outside this node
      if (x > maxX || x + width < minX || y > maxY || y + height < minY) {
        return result;
      }

      // Add particles from this node that intersect the range
      for (Particle p : particles) {
        if (p != null && p.isActive()) {
          if (p.getX() >= x && p.getX() <= x + width &&
              p.getY() >= y && p.getY() <= y + height) {
            result.add(p);
          }
        }
      }

      // If we don't have subnodes, we're done
      if (nodes[0] == null) {
        return result;
      }

      // Otherwise, query each subnode
      for (int i = 0; i < 4; i++) {
        if (nodes[i] != null) {
          result.addAll(nodes[i].queryRange(x, y, width, height));
        }
      }
    } catch (Exception e) {
      System.err.println("Error in ParticleQuadTree.queryRange: " + e);
    } finally {
      lock.readLock().unlock();
    }

    return result;
  }

  /**
   * Check if the quad tree bounds contains a point
   *
   * @param x X position
   * @param y Y position
   * @return True if the point is within bounds
   */
  public boolean containsPoint(float x, float y) {
    // Handle NaN and infinite values
    if (Float.isNaN(x) || Float.isNaN(y) ||
        Float.isInfinite(x) || Float.isInfinite(y)) {
      return false;
    }

    return x >= minX && x <= maxX && y >= minY && y <= maxY;
  }

  /**
   * Get the number of particles stored in this node and all subnodes
   *
   * @return Total particle count
   */
  public int getTotalParticleCount() {
    int count = particles.size();
    for (int i = 0; i < 4; i++) {
      if (nodes[i] != null) {
        count += nodes[i].getTotalParticleCount();
      }
    }
    return count;
  }
}
