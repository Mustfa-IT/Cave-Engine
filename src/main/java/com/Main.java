package com;

import java.awt.Color;

import com.engine.components.RenderableComponent;
import com.engine.components.Transform;
import com.engine.core.GameEngine;
import com.engine.graph.Square;

public class Main {
  public static void main(String[] args) {
    GameEngine game = new GameEngine();

    Square sq = new Square(new Transform(20, 20, 0, 1, 1), Color.RED, 100);
    game.world.createEntity(
        "moving-square",
        new RenderableComponent(sq) // Ensure this is a valid Renderable
    );
    game.start();
  }
}
