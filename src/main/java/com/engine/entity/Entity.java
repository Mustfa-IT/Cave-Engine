package com.engine.entity;

/**
 * Interface representing a game entity
 */
public interface Entity {
    String getId();
    void initialize();
    void update(double deltaTime);
}
