package com.pinagame.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.util.HashMap;
import java.util.HashSet;


public class SaveManager {

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

    private SaveData migrate(SaveData data) {
        if (data.flags == null) data.flags = new HashMap<>();
        if (data.completedChapters == null) data.completedChapters = new HashSet<>();

        int version = data.saveVersion;


        data.saveVersion = CURRENT_SAVE_VERSION;
        return data;
    }
}
