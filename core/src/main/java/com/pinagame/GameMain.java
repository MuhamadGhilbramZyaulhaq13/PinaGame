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
import com.badlogic.gdx.Gdx;

import java.util.List;


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

        dialogManager.addListener(new DialogManager.DialogListener() {
            @Override public void onLine(String speaker, String text) {

                persist();
            }
            @Override public void onChoices(List<DialogChoice> choices) { /* ditangani per-screen (BaseGameScreen) */ }
            @Override public void onSceneChangeRequested(String sceneId) {
                sceneManager.onDialogSceneChangeRequested(sceneId);
            }
            @Override public void onDialogEnd() { onChapterFinished(); }
        });

        sceneManager = new SceneManager(this, this);

        chapterManager = new ChapterManager(dialogManager, sceneManager);
        chapterManager.loadManifest("data/chapters_manifest.json");

        int chapterToStart = saveData.currentChapter;
        String resumeNode = saveData.currentDialogNode;
        if (!chapterManager.hasChapter(chapterToStart)) {
            Gdx.app.log("GameMain", "Chapter " + chapterToStart + " belum tersedia, "
                + "fallback ke chapter " + chapterManager.getHighestAvailableChapterId());
            chapterToStart = chapterManager.getHighestAvailableChapterId();
            resumeNode = null;
        }
        chapterManager.startChapter(chapterToStart, resumeNode);
    }

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



    public void persist() {
        saveData.currentDialogNode = dialogManager.getCurrentNodeId();
        saveData.flags = flags.raw();
        saveManager.save(saveData);
    }

    public DialogManager getDialogManager() { return dialogManager; }
    public SceneManager getSceneManager() { return sceneManager; }
    public StoryFlags getFlags() { return flags; }
}
