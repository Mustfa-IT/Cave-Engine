package com.engine.graph;

import java.awt.Graphics2D;

/**
 * Interface for custom renderers that can be added to the rendering pipeline.
 * This allows game systems to add their own rendering without modifying the core RenderSystem.
 */
public interface CustomRenderer {
    /**
     * Render custom graphics
     *
     * @param g The graphics context to render with
     */
    void render(Graphics2D g);

    /**
     * Get the priority of this renderer (higher numbers render later/on top)
     *
     * @return The rendering priority
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Check if this renderer is currently active
     *
     * @return true if active and should be rendered
     */
    default boolean isActive() {
        return true;
    }
}
