package com.pinagame.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

/**
 * Mengatur perpindahan antara 3 "gaya visual" utama game ini:
 *   GARDEN_3D      -> avatar block/robot Pina di map kebun (gaya Roblox)
 *   PIXEL_ART_2D   -> adegan dunia nyata (Datt & Heri, dst) bergaya pixel retro
 *   SMARTPHONE_UI  -> simulasi antarmuka HP (Instagram, chat)
 *
 * SceneManager tidak membuat Screen sendiri — ia meminta SceneProvider (biasanya
 * diimplementasikan oleh GameMain) untuk membuatnya, supaya SceneManager tetap
 * generic/reusable dan tidak bergantung langsung ke class-class Screen konkret.
 */
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
        this.currentMode = mode;
        this.currentSceneId = sceneId;
        Screen screen = provider.createScreen(mode, sceneId);

        // TODO (lihat penjelasan di chat): bungkus Screen lama & baru dengan
        // TransitionScreen (fade-to-black atau cross-fade pakai FrameBuffer)
        // sebelum game.setScreen(...), supaya lompatan 3D -> pixel art -> UI HP
        // tidak terasa patah/kagok.
        game.setScreen(screen);
    }

    /** Dipanggil ketika node dialog punya action "CHANGE_SCENE". */
    public void onDialogSceneChangeRequested(String sceneId) {
        VisualMode mode = resolveModeFromSceneId(sceneId);
        changeTo(mode, sceneId);
    }

    /**
     * Menentukan mode visual dari prefix id scene. Konvensi penamaan id scene di JSON:
     *   "GARDEN_..."             -> GARDEN_3D
     *   "PIXEL_..."              -> PIXEL_ART_2D
     *   "SOCIAL_..." / "SMARTPHONE_..." -> SMARTPHONE_UI
     */
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
