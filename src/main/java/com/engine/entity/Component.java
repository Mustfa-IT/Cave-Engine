package com.engine.entity;

/**
 * Base interface for all components
 */
public interface Component {
    void initialize();
    void update(double deltaTime);
}
