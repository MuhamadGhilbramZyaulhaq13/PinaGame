package com.pinagame.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Wadah generik untuk menyimpan "flag" progres cerita (mis. "day2_screenshot_taken",
 * "wrong_account_followed", dll). Disimpan sebagai String->String supaya:
 * 1) Mudah diserialisasi ke JSON oleh SaveManager.
 * 2) Tetap kompatibel ketika chapter baru menambahkan flag baru di masa depan —
 *    kita tidak perlu mengubah struktur class, cukup tambah key baru.
 */
public class StoryFlags {

    private final HashMap<String, String> flags;

    public StoryFlags() {
        this.flags = new HashMap<>();
    }

    /** Dipakai saat load dari SaveData yang sudah ada. */
    public StoryFlags(Map<String, String> existing) {
        this.flags = existing != null ? new HashMap<>(existing) : new HashMap<>();
    }

    public void setFlag(String key, String value) {
        flags.put(key, value);
    }

    public void setBoolean(String key, boolean value) {
        flags.put(key, Boolean.toString(value));
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (!flags.containsKey(key)) return defaultValue;
        return Boolean.parseBoolean(flags.get(key));
    }

    public String getString(String key, String defaultValue) {
        return flags.getOrDefault(key, defaultValue);
    }

    public boolean hasFlag(String key) {
        return flags.containsKey(key);
    }

    /** Referensi mentah — dipakai SaveManager untuk menulis balik ke SaveData sebelum save(). */
    public HashMap<String, String> raw() {
        return flags;
    }
}
