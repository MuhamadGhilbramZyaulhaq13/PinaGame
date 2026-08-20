package com.pinagame.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Struktur data mentah yang disimpan ke disk (di-serialize ke JSON oleh SaveManager).
 *
 * PENTING untuk pengembangan chapter baru di masa depan:
 * - JANGAN mengubah nama/tipe field yang sudah ada.
 * - Kalau perlu field baru, tambahkan dengan nilai default, jangan hapus field lama.
 * - Progres pemain lama (flags, currentChapter, dll) harus tetap terbaca walau
 *   struktur berkembang — lihat SaveManager#migrate().
 */
public class SaveData {

    /** Naikkan angka ini di SaveManager setiap kali struktur save berubah signifikan. */
    public int saveVersion = SaveManager.CURRENT_SAVE_VERSION;

    /** Chapter yang sedang/akan dimainkan pemain. */
    public int currentChapter = 1;

    /** Node dialog terakhir yang dicapai, untuk resume persis di titik itu. */
    public String currentDialogNode = "";

    /** Chapter-chapter yang sudah tamat dimainkan. */
    public HashSet<String> completedChapters = new HashSet<>();
    public HashMap<String, String> flags = new HashMap<>();

    public long lastSavedAtMillis = 0L;
}
