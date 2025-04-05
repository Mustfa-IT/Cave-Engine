package com.engine.particles;

import java.util.HashMap;
import java.util.Map;

/**
 * Performance profiling tool for the particle system.
 * Helps identify bottlenecks and optimization opportunities.
 */
public class ParticleSystemProfiler {
  private final Map<String, Long> startTimes = new HashMap<>();
  private final Map<String, Long> durations = new HashMap<>();
  private final Map<String, Long> counts = new HashMap<>();
  private boolean enabled = false;

  /**
   * Start timing a section
   *
   * @param section Name of the section to time
   */
  public void start(String section) {
    if (!enabled)
      return;
    startTimes.put(section, System.nanoTime());
  }

  /**
   * End timing a section
   *
   * @param section Name of the section to end
   */
  public void end(String section) {
    if (!enabled)
      return;

    long startTime = startTimes.getOrDefault(section, System.nanoTime());
    long duration = System.nanoTime() - startTime;

    durations.put(section, durations.getOrDefault(section, 0L) + duration);
    counts.put(section, counts.getOrDefault(section, 0L) + 1);
  }

  /**
   * Reset all timings
   */
  public void reset() {
    startTimes.clear();
    durations.clear();
    counts.clear();
  }

  /**
   * Enable or disable profiling
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Get a report of all timings
   */
  public Map<String, String> getReport() {
    Map<String, String> report = new HashMap<>();

    for (Map.Entry<String, Long> entry : durations.entrySet()) {
      String section = entry.getKey();
      long totalNanos = entry.getValue();
      long count = counts.getOrDefault(section, 1L);

      double avgMs = (totalNanos / count) / 1_000_000.0;
      double totalMs = totalNanos / 1_000_000.0;

      report.put(section, String.format("%.2f ms avg (%.2f ms total, %d calls)",
          avgMs, totalMs, count));
    }

    return report;
  }

  /**
   * Get the total time for a section in milliseconds
   */
  public double getTotalTimeMs(String section) {
    return durations.getOrDefault(section, 0L) / 1_000_000.0;
  }

  /**
   * Get the average time for a section in milliseconds
   */
  public double getAverageTimeMs(String section) {
    long total = durations.getOrDefault(section, 0L);
    long count = counts.getOrDefault(section, 1L);
    return (total / count) / 1_000_000.0;
  }

  /**
   * Log the current report to the console
   */
  public void logReport() {
    if (!enabled)
      return;

    System.out.println("===== Particle System Profiler Report =====");
    for (Map.Entry<String, String> entry : getReport().entrySet()) {
      System.out.println(entry.getKey() + ": " + entry.getValue());
    }
    System.out.println("===========================================");
  }
}
