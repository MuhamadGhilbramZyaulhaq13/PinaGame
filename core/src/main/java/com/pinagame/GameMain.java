package com.pinagame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.pinagame.core.ChapterManager;
import com.pinagame.core.DialogManager;
import com.pinagame.core.SaveData;
import com.pinagame.core.SaveManager;
import com.pinagame.core.SceneManager;
import com.pinagame.core.SceneManager.VisualMode;
import com.pinagame.core.StoryFlags;
import com.pinagame.core.dialog.DialogChoice;
import com.pinagame.screens.GardenScreen;
import com.pinagame.screens.PixelArtScreen;
import com.pinagame.screens.SocialMediaScreen;

import java.util.List;

/**
 * Class utama libGDX (didaftarkan sebagai ApplicationListener oleh launcher
 * Desktop/Android). Menyatukan seluruh manager dan menangani load save saat
 * game dibuka serta auto-save saat progres berubah.
 */
public class GameMain extends Game implements SceneManager.SceneProvider {

    private SaveManager saveManager;
    private SaveData saveData;
    private StoryFlags flags;
    private DialogManager dialogManager;
    private SceneManager sceneManager;
    private ChapterManager chapterManager;

    @Override
    public void create() {
        saveManager = new SaveManager();
        saveData = saveManager.load();
        flags = new StoryFlags(saveData.flags);

        dialogManager = new DialogManager(flags);
        // Listener global: forward perubahan scene & akhir dialog ke level game,
        // terlepas dari screen mana yang sedang aktif menampilkan teksnya.
        dialogManager.addListener(new DialogManager.DialogListener() {
            @Override public void onLine(String speaker, String text) {}
            @Override public void onChoices(List<DialogChoice> choices) { /* ditangani per-screen (BaseGameScreen) */ }
            @Override public void onSceneChangeRequested(String sceneId) {
                sceneManager.onDialogSceneChangeRequested(sceneId);
            }
            @Override public void onDialogEnd() { onChapterFinished(); }
        });

        sceneManager = new SceneManager(this, this);

        chapterManager = new ChapterManager(dialogManager, sceneManager);
        chapterManager.loadManifest("data/chapters_manifest.json");

        chapterManager.startChapter(saveData.currentChapter, saveData.currentDialogNode);
    }

    /** Diimplementasikan dari SceneManager.SceneProvider — dipanggil tiap kali scene berganti. */
    @Override
    public Screen createScreen(VisualMode mode, String sceneId) {
        switch (mode) {
            case GARDEN_3D: return new GardenScreen(this, dialogManager, sceneId);
            case PIXEL_ART_2D: return new PixelArtScreen(this, dialogManager, sceneId);
            case SMARTPHONE_UI: return new SocialMediaScreen(this, dialogManager, sceneId);
            default: throw new IllegalArgumentException("Mode visual tidak dikenal: " + mode);
        }
    }

    private void onChapterFinished() {
        chapterManager.markChapterCompleted(saveData);
        saveData.currentChapter += 1;
        saveData.currentDialogNode = "";
        persist();
        // TODO: kalau chapter berikutnya belum tersedia di manifest (misal masih dalam
        // pengembangan), tampilkan screen "To be continued..." alih-alih memanggil
        // chapterManager.startChapter() untuk chapter yang belum ada.
    }

    /**
     * Simpan progres ke disk. Panggil ini secara berkala — idealnya lewat listener
     * tambahan di DialogManager yang trigger tiap kali goToNode() berpindah, bukan
     * hanya di akhir chapter, supaya progres di tengah chapter pun tidak hilang.
     */
    public void persist() {
        saveData.currentDialogNode = dialogManager.getCurrentNodeId();
        saveData.flags = flags.raw();
        saveManager.save(saveData);
    }

    public DialogManager getDialogManager() { return dialogManager; }
    public SceneManager getSceneManager() { return sceneManager; }
    public StoryFlags getFlags() { return flags; }
}
