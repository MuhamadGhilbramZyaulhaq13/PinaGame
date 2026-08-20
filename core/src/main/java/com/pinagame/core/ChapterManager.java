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
     * PENTING: kalau resumeNodeId KOSONG (chapter baru dimulai dari nol), dialog TIDAK
     * langsung dijalankan di sini. Ini supaya scene yang butuh trigger gameplay dulu
     * (mis. GardenScreen: pemain harus jalan mendekati NPC Datt) tidak "diserobot" oleh
     * dialog yang auto-mulai. Screen awal itu sendiri yang memanggil dialogManager.startFrom(null)
     * pada momen yang tepat (lihat GardenScreen#checkNpcTrigger).
     *
     * Kalau resumeNodeId TERISI (lanjut dari save lama di tengah dialog), kita langsung
     * lompat ke node itu — pemain sudah pernah melewati trigger-nya sebelumnya.
     */
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
        // Kalau kosong: dialog menunggu dipicu screen awal (trigger gameplay atau cutscene).
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

    public Chapter getActiveChapter() {
        return activeChapter;
    }
}
