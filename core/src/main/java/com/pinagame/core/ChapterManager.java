package com.pinagame.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * Mengelola daftar chapter (dibaca dari chapters_manifest.json) dan proses
 * mulai / lanjutkan sebuah chapter. Menambah chapter baru di masa depan cukup:
 *   1) Tambah file dialog baru (mis. chapter2.json)
 *   2) Tambah 1 entry baru di chapters_manifest.json
 * — tanpa perlu mengubah kode Java sama sekali.
 */
public class ChapterManager {

    public static class Chapter {
        public int id;
        public String title;
        /** Path internal ke file dialog JSON, mis. "data/chapters/chapter1.json" */
        public String dialogFile;
        /** Id scene visual awal saat chapter ini dimulai, mis. "GARDEN_MAIN" */
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

    /**
     * resumeNodeId: null/kosong untuk mulai chapter dari awal, atau id node terakhir untuk lanjut.
     *
     * Dialog SELALU langsung dijalankan di sini (baik chapter baru maupun resume) --
     * narasi pembuka tiap hari otomatis tampil tanpa perlu trigger gameplay dulu.
     * Titik "tunggu pemain mendekat" sekarang diatur lewat node ber-action
     * WAIT_APPROACH di dalam JSON dialog itu sendiri (lihat DialogManager), bukan
     * di sini -- screen (mis. GardenScreen) yang memanggil continueFromPause()
     * begitu pemain jalan mendekati NPC.
     */
    public void startChapter(int chapterId, String resumeNodeId) {
        activeChapter = findChapter(chapterId);
        if (activeChapter == null) {
            Gdx.app.error("ChapterManager", "Chapter tidak ditemukan: " + chapterId);
            return;
        }
        dialogManager.loadGraph(activeChapter.dialogFile);
        sceneManager.onDialogSceneChangeRequested(activeChapter.initialSceneId);
        dialogManager.startFrom(resumeNodeId);
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

    /** Dipakai GameMain untuk cek apakah chapter tertentu sudah ada sebelum mulai/lanjutkan. */
    public boolean hasChapter(int id) {
        return findChapter(id) != null;
    }

    /** Fallback kalau saveData.currentChapter menunjuk ke chapter yang belum dibuat. */
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
