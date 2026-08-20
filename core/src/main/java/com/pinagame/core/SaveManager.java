package com.pinagame.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Bertanggung jawab menyimpan & memuat progres pemain ke/dari file lokal
 * (bukan Preferences bawaan Android, supaya struktur data kompleks seperti
 * Map<String,String> dan Set<String> lebih gampang dikelola & di-migrate).
 */
public class SaveManager {

    /** Naikkan tiap kali struktur SaveData berubah, lalu tambah case migrasi di migrate(). */
    public static final int CURRENT_SAVE_VERSION = 1;

    private static final String SAVE_FILE_NAME = "pina_save.json";

    private final Json json;

    public SaveManager() {
        this.json = new Json();
        this.json.setOutputType(JsonWriter.OutputType.json);
    }

    private FileHandle saveFile() {
        return Gdx.files.local(SAVE_FILE_NAME);
    }

    public boolean hasSave() {
        return saveFile().exists();
    }

    /** Selalu berhasil mengembalikan SaveData yang valid — kalau save rusak/belum ada, buat baru. */
    public SaveData load() {
        FileHandle file = saveFile();
        if (!file.exists()) {
            return new SaveData();
        }
        try {
            SaveData data = json.fromJson(SaveData.class, file.readString("UTF-8"));
            return migrate(data);
        } catch (Exception e) {
            Gdx.app.error("SaveManager", "Gagal membaca save, membuat save baru.", e);
            return new SaveData();
        }
    }

    public void save(SaveData data) {
        data.lastSavedAtMillis = System.currentTimeMillis();
        data.saveVersion = CURRENT_SAVE_VERSION;
        saveFile().writeString(json.prettyPrint(data), false, "UTF-8");
    }

    /**
     * Migrasi save versi lama ke struktur terbaru secara ADDITIVE (tambah, tidak menghapus).
     * Pola untuk chapter baru di masa depan:
     *
     *   if (version < 2) {
     *       data.flags.putIfAbsent("chapter2_intro_seen", "false");
     *       version = 2;
     *   }
     *   if (version < 3) {
     *       ... migrasi berikutnya ...
     *   }
     *
     * Dengan begini, pemain yang meng-update game tidak kehilangan progres chapter
     * sebelumnya walau chapter baru menambah flag/field baru.
     */
    private SaveData migrate(SaveData data) {
        if (data.flags == null) data.flags = new HashMap<>();
        if (data.completedChapters == null) data.completedChapters = new HashSet<>();

        int version = data.saveVersion;

        // Tambahkan blok migrasi baru di sini seiring bertambahnya chapter.
        // if (version < 2) { ... version = 2; }

        data.saveVersion = CURRENT_SAVE_VERSION;
        return data;
    }
}
