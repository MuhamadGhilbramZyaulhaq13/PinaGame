package com.pinagame.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class SaveData {

    public int saveVersion = SaveManager.CURRENT_SAVE_VERSION;

    public int currentChapter = 1;

    public String currentDialogNode = "";

    public HashSet<String> completedChapters = new HashSet<>();
    public HashMap<String, String> flags = new HashMap<>();

    public long lastSavedAtMillis = 0L;
}
