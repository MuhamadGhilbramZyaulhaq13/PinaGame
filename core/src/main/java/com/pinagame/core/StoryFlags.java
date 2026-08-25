package com.pinagame.core;

import java.util.HashMap;
import java.util.Map;


public class StoryFlags {

    private final HashMap<String, String> flags;

    public StoryFlags() {
        this.flags = new HashMap<>();
    }

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

    public HashMap<String, String> raw() {
        return flags;
    }
}
