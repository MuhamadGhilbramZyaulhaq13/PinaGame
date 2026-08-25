package com.pinagame.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

import java.util.ArrayList;
import java.util.List;


public class ChapterManager {

    public static class Chapter {
        public int id;
        public String title;

        public String dialogFile;

        public String initialSceneId;
    }

    public static class Manifest {
        public ArrayList<Chapter> chapters = new ArrayList<>();
    }

    private Manifest manifest;
    private Chapter activeChapter;

    private final DialogManager dialogManager;
    private final SceneManager sceneManager;

    public ChapterManager(DialogManager dialogManager, SceneManager sceneManager) {
        this.dialogManager = dialogManager;
        this.sceneManager = sceneManager;
    }

    public void loadManifest(String internalPath) {
        Json json = new Json();
        String raw = Gdx.files.internal(internalPath).readString("UTF-8");
        this.manifest = json.fromJson(Manifest.class, raw);
    }


    public void startChapter(int chapterId, String resumeNodeId) {
        activeChapter = findChapter(chapterId);
        if (activeChapter == null) {
            Gdx.app.error("ChapterManager", "Chapter tidak ditemukan: " + chapterId);
            return;
        }
        dialogManager.loadGraph(activeChapter.dialogFile);
        sceneManager.onDialogSceneChangeRequested(activeChapter.initialSceneId);

        if (resumeNodeId != null && !resumeNodeId.isEmpty()) {
            dialogManager.startFrom(resumeNodeId);
        }
    }

    public void markChapterCompleted(SaveData data) {
        if (activeChapter == null) return;
        data.completedChapters.add(String.valueOf(activeChapter.id));
    }

    private Chapter findChapter(int id) {
        for (Chapter c : manifest.chapters) {
            if (c.id == id) return c;
        }
        return null;
    }

    public boolean hasChapter(int id) {
        return findChapter(id) != null;
    }

    public int getHighestAvailableChapterId() {
        int max = 1;
        for (Chapter c : manifest.chapters) {
            if (c.id > max) max = c.id;
        }
        return max;
    }

    public Chapter getActiveChapter() {
        return activeChapter;
    }
}
