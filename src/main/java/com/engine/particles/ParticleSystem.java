package com.engine.particles;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.engine.core.CameraSystem;
import com.engine.events.EventSystem;
import com.engine.events.EventTypes;
import com.engine.graph.CustomRenderer;
import com.engine.graph.RenderSystem;
import com.engine.particles.emitters.PhysicalParticleEmitter;
import com.engine.physics.CollisionSystem;
import com.engine.util.ParticleQuadTree;
import com.engine.util.SpatialHashGrid;

import dev.dominion.ecs.api.Entity;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.contacts.Contact;

/**
 * Main system for managing particle emitters and integrating with other engine
 * systems.
 */
@Singleton
public class ParticleSystem implements CustomRenderer {

  private static final Logger LOGGER = Logger.getLogger(ParticleSystem.class.getName());

  // Thread pool for parallel particle processing
  private final ExecutorService particleThreadPool;
  private final int numThreads;

  // Unified particle storage for better memory access patterns
  private final ParticleDataStore particleStore;

  private final List<ParticleEmitter> emitters = new ArrayList<>();
  private final Map<Entity, List<ParticleEmitter>> entityEmitters = new HashMap<>();
  private final ParticleEmitterFactory emitterFactory;
  private final EventSystem eventSystem;
  private final RenderSystem renderSystem;
  private final CameraSystem cameraSystem;

  // Advanced spatial partitioning
  private final ParticleQuadTree quadTree;
  private final SpatialHashGrid spatialGrid; // Keep both for different use cases

  // Particle pooling
  private final ParticlePool particlePool;

  // View frustum for culling
  private Rectangle viewBounds = new Rectangle(-1000, -1000, 2000, 2000);

  // Batch rendering support with instanced rendering capability
  private final Map<String, BatchRenderer> batchRenderers = new HashMap<>();

  // Performance monitoring
  private long lastUpdateTime = 0;
  private long updateDuration = 0;
  private long renderDuration = 0;

  private int totalParticleCount = 0;
  private boolean paused = false;
  private int maxParticlesTotal = 10000; // Global limit across all emitters

  // Performance optimization settings
  private boolean enableCulling = true;
  private boolean enableBatchRendering = true;
  private boolean enableMultiThreading = true;
  private boolean enableInstancedRendering = true;
  private boolean enableLOD = true;
  private int distanceThreshold = 2000; // Distance beyond which to reduce update frequency
  private boolean useAdaptiveUpdates = true;

  @Inject
  public ParticleSystem(ParticleEmitterFactory emitterFactory,
      EventSystem eventSystem,
      RenderSystem renderSystem,
      CameraSystem cameraSystem,
      CollisionSystem collisionSystem) {
    this.emitterFactory = emitterFactory;
    this.eventSystem = eventSystem;
    this.renderSystem = renderSystem;
    this.cameraSystem = cameraSystem;

    // Set up thread pool for parallel processing (use available processors - 1 to
    // avoid starving the main thread)
    numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    particleThreadPool = Executors.newFixedThreadPool(numThreads);

    // Initialize unified particle data store
    this.particleStore = new ParticleDataStore(20000);

    // Initialize spatial data structures
    this.spatialGrid = new SpatialHashGrid(200, 200, 64); // Larger world, optimized cell size
    this.quadTree = new ParticleQuadTree(-10000, -10000, 20000, 20000, 8, 10); // 10k by 10k world

    // Initialize particle pool with larger capacity for better reuse
    this.particlePool = new ParticlePool(5000);

    // Register as a custom renderer with the render system
    renderSystem.addCustomRenderer(this);

    // Set up collision handling for physical particles
    setupCollisionHandling(collisionSystem);

    // Create optimized batch renderers
    batchRenderers.put("color", new ColorParticleBatchRenderer());
    batchRenderers.put("sprite", new SpriteParticleBatchRenderer());
    batchRenderers.put("instanced", new InstancedSpriteRenderer());

    LOGGER.info("Optimized particle system initialized with " + numThreads + " worker threads");
  }

  /**
   * Create a particle emitter of the specified type
   *
   * @param type   The type of emitter to create
   * @param x      X position
   * @param y      Y position
   * @param params Configuration parameters
   * @return The created emitter, already registered with the system
   */
  public ParticleEmitter createEmitter(String type, float x, float y, Map<String, Object> params) {
    ParticleEmitter emitter = emitterFactory.createEmitter(type, x, y, params);

    // Configure emitter to use particle pool if it supports it
    if (emitter instanceof PooledParticleEmitter) {
      ((PooledParticleEmitter) emitter).setParticlePool(particlePool);
    }

    addEmitter(emitter);
    return emitter;
  }

  /**
   * Add an existing emitter to the system
   *
   * @param emitter The emitter to add
   */
  public void addEmitter(ParticleEmitter emitter) {
    synchronized (emitters) {
      emitters.add(emitter);
    }
    LOGGER.fine("Added particle emitter at " + emitter.getX() + ", " + emitter.getY());

    // Fire event for emitter added
    eventSystem.fireEvent(EventTypes.PARTICLE_EMITTER_ADDED,
        "emitter", emitter);
  }

  /**
   * Remove an emitter from the system
   *
   * @param emitter The emitter to remove
   */
  public void removeEmitter(ParticleEmitter emitter) {
    synchronized (emitters) {
      emitters.remove(emitter);
    }

    // Also remove from entity associations
    Entity attachedEntity = emitter.getAttachedEntity();
    if (attachedEntity != null) {
      synchronized (entityEmitters) {
        List<ParticleEmitter> entityEmitterList = entityEmitters.get(attachedEntity);
        if (entityEmitterList != null) {
          entityEmitterList.remove(emitter);
          if (entityEmitterList.isEmpty()) {
            entityEmitters.remove(attachedEntity);
          }
        }
      }
    }

    // Fire event for emitter removed
    eventSystem.fireEvent(EventTypes.PARTICLE_EMITTER_REMOVED,
        "emitter", emitter);
  }

  /**
   * Attach an emitter to an entity
   *
   * @param emitter The emitter to attach
   * @param entity  The entity to attach to
   */
  public void attachEmitterToEntity(ParticleEmitter emitter, Entity entity) {
    // Set the entity in the emitter
    emitter.setAttachedEntity(entity);

    // Add to entity association map
    entityEmitters.computeIfAbsent(entity, k -> new ArrayList<>()).add(emitter);

    // Add emitter to system if not already added
    if (!emitters.contains(emitter)) {
      addEmitter(emitter);
    }
  }

  /**
   * Get all emitters attached to an entity
   *
   * @param entity The entity
   * @return List of attached emitters
   */
  public List<ParticleEmitter> getEntityEmitters(Entity entity) {
    return entityEmitters.getOrDefault(entity, new ArrayList<>());
  }

  /**
   * Update all particle emitters
   *
   * @param deltaTime Time since last update in seconds
   */
  public void update(float deltaTime) {
    if (paused) {
      return;
    }

    long startTime = System.nanoTime();

    try {
      // Update camera view bounds for culling
      updateViewBounds();

      // Clear spatial data structures before updating positions
      spatialGrid.clear();
      quadTree.clear();

      // Create a safe copy to iterate over
      List<ParticleEmitter> emittersCopy;
      List<ParticleEmitter> emittersToRemove = Collections.synchronizedList(new ArrayList<>());

      synchronized (emitters) {
        emittersCopy = new ArrayList<>(emitters);
      }

      totalParticleCount = 0;
      AtomicInteger activeParticles = new AtomicInteger(0);

      // Only use multi-threading if we have enough emitters to make it worthwhile
      if (enableMultiThreading && emittersCopy.size() > 10) {
        // Group emitters for parallel processing
        List<List<ParticleEmitter>> emitterGroups = partitionForThreads(emittersCopy, numThreads);
        List<Future<?>> futures = new ArrayList<>();

        // Process each group in parallel
        for (List<ParticleEmitter> group : emitterGroups) {
          if (group.isEmpty())
            continue;

          futures.add(particleThreadPool.submit(() -> {
            try {
              int localCount = processEmitterGroup(group, deltaTime, emittersToRemove);
              activeParticles.addAndGet(localCount);
            } catch (Exception e) {
              LOGGER.warning("Error in particle thread: " + e);
            }
          }));
        }

        // Wait for all threads to complete with a timeout
        for (Future<?> future : futures) {
          try {
            // Add timeout to prevent deadlocks
            future.get(100, TimeUnit.MILLISECONDS);
          } catch (Exception e) {
            LOGGER.warning("Error or timeout waiting for particle thread: " + e);
          }
        }

        totalParticleCount = activeParticles.get();
      } else {
        // Single-threaded update
        for (ParticleEmitter emitter : emittersCopy) {
          try {
            int count = updateEmitter(emitter, deltaTime, emittersToRemove);
            totalParticleCount += count;
          } catch (Exception e) {
            LOGGER.warning("Error updating emitter: " + e);
          }
        }
      }

      // Now safely remove the inactive emitters
      if (!emittersToRemove.isEmpty()) {
        synchronized (emitters) {
          emitters.removeAll(emittersToRemove);
        }
      }

      // Fire particle count event
      eventSystem.fireEvent(EventTypes.PARTICLE_COUNT_UPDATED,
          "count", totalParticleCount);

      // Return unused particles to pool
      particlePool.cleanup();
    } catch (Exception e) {
      LOGGER.severe("Critical error in particle system update: " + e);
    }

    // Record update time for performance monitoring
    updateDuration = System.nanoTime() - startTime;
  }

  // Helper methods for multi-threaded processing

  private List<List<ParticleEmitter>> partitionForThreads(List<ParticleEmitter> allEmitters, int threadCount) {
    // Handle empty list case
    if (allEmitters.isEmpty()) {
      return Collections.emptyList();
    }

    // Calculate number of emitters per thread, at least 1
    int emittersPerThread = Math.max(1, (int) Math.ceil((double) allEmitters.size() / threadCount));
    List<List<ParticleEmitter>> groups = new ArrayList<>();

    for (int i = 0; i < allEmitters.size(); i += emittersPerThread) {
      int end = Math.min(i + emittersPerThread, allEmitters.size());
      if (end > i) {
        groups.add(new ArrayList<>(allEmitters.subList(i, end)));
      }
    }

    return groups;
  }

  private int processEmitterGroup(List<ParticleEmitter> groupEmitters, float deltaTime,
      List<ParticleEmitter> toRemove) {
    int particleCount = 0;

    for (ParticleEmitter emitter : groupEmitters) {
      if (emitter == null)
        continue;

      try {
        particleCount += updateEmitter(emitter, deltaTime, toRemove);
      } catch (Exception e) {
        LOGGER.warning("Error processing emitter in thread: " + e);
        // Continue processing other emitters despite error
      }
    }

    return particleCount;
  }

  private int updateEmitter(ParticleEmitter emitter, float deltaTime, List<ParticleEmitter> toRemove) {
    if (emitter == null)
      return 0;

    // Skip inactive emitters with no particles
    if (!emitter.isActive() && emitter.getParticles().isEmpty()) {
      synchronized (toRemove) {
        toRemove.add(emitter);
      }
      return 0;
    }

    try {
      // Regular update
      emitter.update(deltaTime);
    } catch (Exception e) {
      LOGGER.warning("Error updating emitter: " + e);
      return 0;
    }

    int particleCount = 0;

    // Create a defensive copy to avoid concurrent modification during spatial
    // registration
    List<Particle> particlesCopy = new ArrayList<>();
    try {
      List<Particle> originalParticles = emitter.getParticles();
      if (originalParticles != null) {
        synchronized (originalParticles) {
          particlesCopy.addAll(originalParticles);
        }
        particleCount = originalParticles.size();
      }
    } catch (Exception e) {
      LOGGER.warning("Error copying particles: " + e);
      return 0;
    }

    // Register particles in spatial structures
    try {
      if (emitter instanceof SpatialAware) {
        try {
          ((SpatialAware) emitter).registerInSpatialStructure(spatialGrid);
        } catch (Exception e) {
          LOGGER.warning("Error registering in spatial grid: " + e);
        }

        try {
          ((SpatialAware) emitter).registerInQuadTree(quadTree);
        } catch (Exception e) {
          LOGGER.warning("Error registering in quad tree: " + e);
        }
      } else if (!particlesCopy.isEmpty()) {
        // Register each particle manually - use batched insertion for better
        // performance
        try {
          // Filter to only active particles before adding to quadtree
          List<Particle> activeParticles = particlesCopy.stream()
              .filter(p -> p != null && p.isActive())
              .collect(Collectors.toList());

          if (!activeParticles.isEmpty()) {
            quadTree.insertParticles(activeParticles);
          }
        } catch (Exception e) {
          LOGGER.warning("Error inserting into quad tree: " + e);
        }

        // Register in spatial grid
        for (Particle p : particlesCopy) {
          if (p == null || !p.isActive())
            continue;

          try {
            float size = p.getSize();
            spatialGrid.insertObject(p, p.getX() - size / 2, p.getY() - size / 2, size, size);
          } catch (Exception e) {
            // Skip this particle but continue with others
          }
        }
      }
    } catch (Exception e) {
      LOGGER.warning("Error registering particles in spatial structures: " + e);
    }

    return particleCount;
  }

  private void updateViewBounds() {
    if (enableCulling && cameraSystem != null && cameraSystem.getActiveCamera() != null) {
      float[] worldBounds = cameraSystem.getWorldViewBounds();
      viewBounds.x = (int) worldBounds[0];
      viewBounds.y = (int) worldBounds[1];
      viewBounds.width = (int) worldBounds[2];
      viewBounds.height = (int) worldBounds[3];

      // Expand view bounds by a margin to prevent popping
      viewBounds.x -= viewBounds.width / 2;
      viewBounds.y -= viewBounds.height / 2;
      viewBounds.width += viewBounds.width;
      viewBounds.height += viewBounds.height;
    }
  }

  /**
   * Render callback - called by the RenderSystem
   */
  @Override
  public void render(Graphics2D g) {
    long startTime = System.nanoTime();

    // Reset all batch renderers
    if (enableBatchRendering) {
      for (BatchRenderer renderer : batchRenderers.values()) {
        renderer.reset();
      }
    }

    // Create a safe copy of the emitters list
    List<ParticleEmitter> emittersCopy;
    synchronized (emitters) {
      emittersCopy = new ArrayList<>(emitters);
    }

    // Sort emitters by type for better batching
    if (enableBatchRendering) {
      emittersCopy.sort((a, b) -> {
        String typeA = (a instanceof BatchableEmitter) ? ((BatchableEmitter) a).getBatchType() : "";
        String typeB = (b instanceof BatchableEmitter) ? ((BatchableEmitter) b).getBatchType() : "";
        return typeA.compareTo(typeB);
      });
    }

    // First pass - collect particles for batch rendering and render non-batchable
    // particles
    for (ParticleEmitter emitter : emittersCopy) {
      // Skip out-of-view emitters using quad tree for faster culling
      if (enableCulling && !quadTreeContains(emitter.getX(), emitter.getY())) {
        continue;
      }

      // Handle emitter rendering based on distance (LOD)
      // float distance = distanceToCamera(emitter.getX(), emitter.getY());

      // Choose rendering method based on distance and capability
      if (enableBatchRendering) {
        if (emitter instanceof BatchableEmitter) {
          BatchableEmitter batchable = (BatchableEmitter) emitter;
          String batchType = batchable.getBatchType();

          // Use instanced rendering for sprites when far away
          // if (enableInstancedRendering && "sprite".equals(batchType) && distance >
          // distanceThreshold) {
          if (enableInstancedRendering && "sprite".equals(batchType)) {
            batchable.addToBatch(batchRenderers.get("instanced"));
          } else {
            BatchRenderer renderer = batchRenderers.get(batchType);
            if (renderer != null) {
              batchable.addToBatch(renderer);
            } else {
              emitter.render(g);
            }
          }
        } else {
          emitter.render(g);
        }
      } else {
        emitter.render(g);
      }
    }

    // Second pass - render all batches
    if (enableBatchRendering) {
      for (BatchRenderer renderer : batchRenderers.values()) {
        renderer.render(g);
      }
    }

    renderDuration = System.nanoTime() - startTime;
  }

  private boolean quadTreeContains(float x, float y) {
    // More efficient check than rectangle.contains for just a point
    return quadTree.containsPoint(x, y) || viewBounds.contains(x, y);
  }

  // Getters for performance metrics

  public long getUpdateTimeMs() {
    return updateDuration / 1_000_000;
  }

  public long getRenderTimeMs() {
    return renderDuration / 1_000_000;
  }

  /**
   * Set advanced optimization settings
   */
  public void setAdvancedOptimizationSettings(boolean enableCulling, boolean enableBatchRendering,
      boolean enableMultiThreading, boolean enableInstancedRendering,
      boolean enableLOD, int distanceThreshold) {
    this.enableCulling = enableCulling;
    this.enableBatchRendering = enableBatchRendering;
    this.enableMultiThreading = enableMultiThreading;
    this.enableInstancedRendering = enableInstancedRendering;
    this.enableLOD = enableLOD;
    this.distanceThreshold = distanceThreshold;

    LOGGER.info("Advanced particle optimization settings updated");
  }

  /**
   * Create an explosion effect at the specified position
   *
   * @param x             X position
   * @param y             Y position
   * @param particleCount Number of particles
   * @return The explosion emitter
   */
  public ParticleEmitter createExplosion(float x, float y, int particleCount) {
    ParticleEmitter emitter = emitterFactory.createExplosionEmitter(x, y);
    addEmitter(emitter);
    emitter.burst(particleCount);
    return emitter;
  }

  /**
   * Create a continuous fire effect at the specified position
   *
   * @param x X position
   * @param y Y position
   * @return The fire emitter
   */
  public ParticleEmitter createFire(float x, float y) {
    ParticleEmitter emitter = emitterFactory.createFireEmitter(x, y);
    addEmitter(emitter);
    emitter.start();
    return emitter;
  }

  /**
   * Create a smoke effect at the specified position
   *
   * @param x X position
   * @param y Y position
   * @return The smoke emitter
   */
  public ParticleEmitter createSmoke(float x, float y) {
    ParticleEmitter emitter = emitterFactory.createSmokeEmitter(x, y);
    addEmitter(emitter);
    emitter.start();
    return emitter;
  }

  /**
   * Get the total particle count across all emitters
   *
   * @return Total particle count
   */
  public int getParticleCount() {
    return totalParticleCount;
  }

  /**
   * Get the emitter factory
   *
   * @return The emitter factory
   */
  public ParticleEmitterFactory getFactory() {
    return emitterFactory;
  }

  /**
   * Pause or resume the particle system
   *
   * @param paused Whether the system should be paused
   */
  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  /**
   * Clear all particles and emitters
   */
  public void clearAll() {
    emitters.clear();
    entityEmitters.clear();
    totalParticleCount = 0;
    LOGGER.info("All particle emitters cleared");
  }

  /**
   * Set up collision handling for physical particles
   */
  private void setupCollisionHandling(CollisionSystem collisionSystem) {
    // Listen for collision events
    eventSystem.addEventListener(EventTypes.COLLISION_BEGIN, event -> {
      Contact contact = (Contact) event.getData("collision");
      if (contact != null) {
        Body bodyA = contact.getFixtureA().getBody();
        Body bodyB = contact.getFixtureB().getBody();

        // Check if either body is a particle
        handleParticleCollision(bodyA);
        handleParticleCollision(bodyB);
      }
      return;
    });
  }

  /**
   * Handle a collision involving a particle
   *
   * @param body The physics body that collided
   */
  private void handleParticleCollision(Body body) {
    if (body.getUserData() instanceof PhysicalParticleEmitter.PhysicalParticle) {
      // Find the emitter for this particle
      for (ParticleEmitter emitter : emitters) {
        if (emitter instanceof PhysicalParticleEmitter) {
          ((PhysicalParticleEmitter) emitter).handleCollision(body);
          break;
        }
      }
    }
  }

  @Override
  public int getPriority() {
    return 0;
  }
}
