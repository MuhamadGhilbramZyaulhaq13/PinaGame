package com.pinagame.core.dialog;

import java.util.HashMap;
import java.util.Map;

/**
 * Representasi 1 file dialog JSON (biasanya = 1 chapter, berisi banyak "hari").
 * Lihat contoh: assets/data/chapters/chapter1.json
 */
public class DialogGraph {

    public int chapter;

    public String startNode;

    /** Key = id node (harus sama dengan DialogNode#id kalau diisi manual). */
    public HashMap<String, DialogNode> nodes = new HashMap<>();
}
