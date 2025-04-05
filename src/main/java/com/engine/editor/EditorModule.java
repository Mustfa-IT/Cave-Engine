package com.engine.editor;

import javax.inject.Singleton;

import com.engine.core.GameFrame;
import com.engine.core.GameWindow;
import com.engine.ui.UISystem;

import dagger.Module;
import dagger.Provides;

@Module
public class EditorModule {

  @Provides
  @Singleton
  public Editor provideEditor(GameWindow gameWindow, GameFrame gameFrame, UISystem uiSystem) {
    Editor editor = new Editor(gameWindow, gameFrame);
    // Connect editor with UI system
    editor.setUISystem(uiSystem);
    uiSystem.setEditor(editor);
    return editor;
  }
}
