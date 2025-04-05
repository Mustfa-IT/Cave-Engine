package com.engine.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A spatial hash grid for efficient spatial queries of particle positions.
 * Allows quick lookup of nearby particles without checking every particle.
 */
public class SpatialHashGrid {
  private final int cellSize;
  private final int worldWidth;
  private final int worldHeight;
  private final Map<Long, Set<Object>> cells;

  // Constants for limiting calculations
  private static final int MAX_GRID_DIMENSION = 10000;
  private static final int MAX_OBJECTS_PER_CELL = 1000;

  /**
   * Create a new spatial hash grid
   *
   * @param worldWidth  Width of the world space
   * @param worldHeight Height of the world space
   * @param cellSize    Size of each cell in the grid
   */
  public SpatialHashGrid(int worldWidth, int worldHeight, int cellSize) {
    this.worldWidth = worldWidth;
    this.worldHeight = worldHeight;
    this.cellSize = Math.max(1, cellSize); // Ensure positive cell size
    // Use ConcurrentHashMap for thread safety
    this.cells = new ConcurrentHashMap<>();
  }

  /**
   * Insert an object into the spatial grid
   *
   * @param obj    The object to insert
   * @param x      X position
   * @param y      Y position
   * @param width  Width of the object
   * @param height Height of the object
   */
  public void insertObject(Object obj, float x, float y, float width, float height) {
    if (obj == null)
      return;

    try {
      // Validate inputs to avoid massive memory allocation
      if (Float.isNaN(x) || Float.isNaN(y) ||
          Float.isInfinite(x) || Float.isInfinite(y) ||
          Float.isNaN(width) || Float.isNaN(height) ||
          Float.isInfinite(width) || Float.isInfinite(height)) {
        return;
      }

      // Skip insertion if object dimensions are unreasonable
      if (width <= 0 || height <= 0 || width > worldWidth * 2 || height > worldHeight * 2) {
        return;
      }

      // Calculate grid cells overlapped by the object
      int startCellX = (int) Math.floor(x / cellSize);
      int startCellY = (int) Math.floor(y / cellSize);
      int endCellX = (int) Math.floor((x + width) / cellSize);
      int endCellY = (int) Math.floor((y + height) / cellSize);

      // Limit to reasonable ranges to avoid excessive memory usage
      startCellX = Math.max(-MAX_GRID_DIMENSION, Math.min(MAX_GRID_DIMENSION, startCellX));
      startCellY = Math.max(-MAX_GRID_DIMENSION, Math.min(MAX_GRID_DIMENSION, startCellY));
      endCellX = Math.max(-MAX_GRID_DIMENSION, Math.min(MAX_GRID_DIMENSION, endCellX));
      endCellY = Math.max(-MAX_GRID_DIMENSION, Math.min(MAX_GRID_DIMENSION, endCellY));

      // Limit total cells to reasonable number
      int totalCells = (endCellX - startCellX + 1) * (endCellY - startCellY + 1);
      if (totalCells > 100) {
        // If object covers too many cells, just add to a few representative ones
        int stepX = Math.max(1, (endCellX - startCellX) / 10);
        int stepY = Math.max(1, (endCellY - startCellY) / 10);

        for (int cellX = startCellX; cellX <= endCellX; cellX += stepX) {
          for (int cellY = startCellY; cellY <= endCellY; cellY += stepY) {
            long cellKey = getCellKey(cellX, cellY);
            addToCell(cellKey, obj);
          }
        }
      } else {
        // Insert into all cells the object touches
        for (int cellX = startCellX; cellX <= endCellX; cellX++) {
          for (int cellY = startCellY; cellY <= endCellY; cellY++) {
            long cellKey = getCellKey(cellX, cellY);
            addToCell(cellKey, obj);
          }
        }
      }
    } catch (Exception e) {
      // Don't crash the entire particle system because of a spatial hashing error
      System.err.println("Error in SpatialHashGrid.insertObject: " + e);
    }
  }

  /**
   * Add an object to a cell, with limits on cell size
   */
  private void addToCell(long cellKey, Object obj) {
    Set<Object> cellObjects = cells.computeIfAbsent(cellKey, k -> {
      // Use HashSet with initial capacity to avoid resizing
      return new HashSet<>(32);
    });

    // Only add if the cell isn't too crowded
    if (cellObjects.size() < MAX_OBJECTS_PER_CELL) {
      cellObjects.add(obj);
    }
  }

  /**
   * Get all objects potentially intersecting a position
   *
   * @param x      X position to query
   * @param y      Y position to query
   * @param width  Width of query area
   * @param height Height of query area
   * @return List of objects in the area
   */
  public List<Object> getPotentialCollisions(float x, float y, float width, float height) {
    Set<Object> result = new HashSet<>();

    try {
      // Validate inputs
      if (Float.isNaN(x) || Float.isNaN(y) ||
          Float.isNaN(width) || Float.isNaN(height)) {
        return new ArrayList<>();
      }

      // Calculate grid cells overlapped by the query area
      int startCellX = (int) Math.floor(x / cellSize);
      int startCellY = (int) Math.floor(y / cellSize);
      int endCellX = (int) Math.floor((x + width) / cellSize);
      int endCellY = (int) Math.floor((y + height) / cellSize);

      // Limit to reasonable ranges
      startCellX = Math.max(-MAX_GRID_DIMENSION, Math.min(MAX_GRID_DIMENSION, startCellX));
      startCellY = Math.max(-MAX_GRID_DIMENSION, Math.min(MAX_GRID_DIMENSION, startCellY));
      endCellX = Math.max(-MAX_GRID_DIMENSION, Math.min(MAX_GRID_DIMENSION, endCellX));
      endCellY = Math.max(-MAX_GRID_DIMENSION, Math.min(MAX_GRID_DIMENSION, endCellY));

      // Get all objects from overlapping cells
      for (int cellX = startCellX; cellX <= endCellX; cellX++) {
        for (int cellY = startCellY; cellY <= endCellY; cellY++) {
          long cellKey = getCellKey(cellX, cellY);
          Set<Object> cellObjects = cells.get(cellKey);
          if (cellObjects != null) {
            result.addAll(cellObjects);
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Error in SpatialHashGrid.getPotentialCollisions: " + e);
    }

    return new ArrayList<>(result);
  }

  /**
   * Get objects near a specific position
   *
   * @param x      X position
   * @param y      Y position
   * @param radius Search radius
   * @return List of nearby objects
   */
  public List<Object> getNearbyObjects(float x, float y, float radius) {
    return getPotentialCollisions(x - radius, y - radius, radius * 2, radius * 2);
  }

  /**
   * Clear all objects from the grid
   */
  public void clear() {
    cells.clear();
  }

  /**
   * Generate a unique key for a cell position
   */
  private long getCellKey(int cellX, int cellY) {
    return ((long) cellX & 0xFFFFFFFFl) | (((long) cellY & 0xFFFFFFFFl) << 32);
  }

  /**
   * Get the number of cells currently in use
   *
   * @return Number of non-empty grid cells
   */
  public int getCellCount() {
    return cells.size();
  }

  /**
   * Get the current cell size
   *
   * @return Size of each grid cell
   */
  public int getCellSize() {
    return cellSize;
  }
}
