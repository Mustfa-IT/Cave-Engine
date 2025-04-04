package com.engine.editor;

import javax.inject.Singleton;

import com.engine.core.GameFrame;
import com.engine.core.GameWindow;

import dagger.Module;
import dagger.Provides;

@Module
public class EditorModule {

  @Provides
  @Singleton
  public Editor provideEditor(GameWindow gameWindow, GameFrame gameFrame) {
    return new Editor(gameWindow, gameFrame);
  }
}
