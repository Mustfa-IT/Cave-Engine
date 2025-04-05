package com.engine.physics;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

import com.engine.particles.emitters.PhysicalParticleEmitter.PhysicalParticle;

/**
 * Optimized batch processor for physics calculations of particles.
 * Uses parallel processing for better performance with many physics particles.
 */
public class BatchPhysicsProcessor {
  private static final int BATCH_SIZE = 128;
  private final ExecutorService executor;

  public BatchPhysicsProcessor() {
    // Use available processors - 1 to avoid starving the main thread
    int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
    executor = Executors.newFixedThreadPool(threads);
  }

  /**
   * Process a batch of physics particles in parallel
   *
   * @param particles List of physical particles to process
   * @param deltaTime Time step
   */
  public void processBatch(List<PhysicalParticle> particles, float deltaTime) {
    if (particles.size() <= BATCH_SIZE) {
      // Small batch - process directly
      updateParticleBatch(particles, 0, particles.size(), deltaTime);
      return;
    }

    // Split into batches for parallel processing
    int numBatches = (particles.size() + BATCH_SIZE - 1) / BATCH_SIZE;
    CompletableFuture<?>[] futures = new CompletableFuture[numBatches];

    for (int i = 0; i < numBatches; i++) {
      final int startIdx = i * BATCH_SIZE;
      final int endIdx = Math.min(startIdx + BATCH_SIZE, particles.size());

      futures[i] = CompletableFuture.runAsync(() -> {
        updateParticleBatch(particles, startIdx, endIdx, deltaTime);
      }, executor);
    }

    // Wait for all batches to complete
    CompletableFuture.allOf(futures).join();
  }

  /**
   * Update a batch of particles
   */
  private void updateParticleBatch(List<PhysicalParticle> particles, int start, int end, float deltaTime) {
    for (int i = start; i < end; i++) {
      PhysicalParticle particle = particles.get(i);
      if (!particle.isActive())
        continue;

      Body body = particle.getPhysicsBody();
      if (body != null) {
        // Apply any custom physics logic here

        // Example: Apply drag force
        Vec2 velocity = body.getLinearVelocity();
        float speed = velocity.length();

        if (speed > 0.1f) {
          float dragFactor = 0.98f; // Small drag
          body.setLinearVelocity(velocity.mul(dragFactor));
        }
      }
    }
  }

  /**
   * Shutdown the executor service
   */
  public void shutdown() {
    executor.shutdown();
  }
}
