package com.pinagame.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;


public class SceneManager {

    public enum VisualMode {
        GARDEN_3D,
        PIXEL_ART_2D,
        SMARTPHONE_UI
    }

    public interface SceneProvider {
        Screen createScreen(VisualMode mode, String sceneId);
    }

    private final Game game;
    private final SceneProvider provider;
    private VisualMode currentMode;
    private String currentSceneId;

    public SceneManager(Game game, SceneProvider provider) {
        this.game = game;
        this.provider = provider;
    }

    public void changeTo(VisualMode mode, String sceneId) {
        Screen oldScreen = game.getScreen();
        this.currentMode = mode;
        this.currentSceneId = sceneId;
        Screen screen = provider.createScreen(mode, sceneId);


        game.setScreen(screen);
        if (oldScreen != null) {
            oldScreen.dispose();
        }
    }

    public void onDialogSceneChangeRequested(String sceneId) {
        VisualMode mode = resolveModeFromSceneId(sceneId);
        changeTo(mode, sceneId);
    }


    private VisualMode resolveModeFromSceneId(String sceneId) {
        if (sceneId == null) return currentMode;
        if (sceneId.startsWith("GARDEN")) return VisualMode.GARDEN_3D;
        if (sceneId.startsWith("PIXEL")) return VisualMode.PIXEL_ART_2D;
        if (sceneId.startsWith("SOCIAL") || sceneId.startsWith("SMARTPHONE")) return VisualMode.SMARTPHONE_UI;
        return currentMode;
    }

    public VisualMode getCurrentMode() {
        return currentMode;
    }

    public String getCurrentSceneId() {
        return currentSceneId;
    }
}
