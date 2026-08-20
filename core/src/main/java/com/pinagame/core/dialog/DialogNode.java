package com.pinagame.core.dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Satu node/baris di dalam graph dialog. Bisa berupa:
 * - Baris teks biasa (speaker + text + next)
 * - Baris dengan percabangan (choices)
 * - Instruksi non-teks (action), misalnya pindah scene atau set flag
 *
 * Field "action" yang didukung DialogManager saat ini:
 *   "CHANGE_SCENE" -> actionTarget = id scene tujuan (lihat SceneManager)
 *   "SET_FLAG"     -> actionTarget = nama flag yang di-set true
 *   "END_CHAPTER"  -> menandai dialog/chapter selesai
 */
public class DialogNode {

    /** Diisi otomatis oleh DialogManager dari key map "nodes" kalau tidak diisi manual di JSON. */
    public String id;

    /** "Pina", "Datt", "Heri", "Narrator", atau null kalau node ini murni action. */
    public String speaker;

    public String text;

    /** Opsional: id scene visual untuk node ini (mis. "GARDEN_MAIN", "PIXEL_ROOM_DATT_HERI"). */
    public String scene;

    public String action;

    public String actionTarget;

    /** Opsional: node ini hanya "berlaku" kalau flag ini true; kalau tidak, loncat ke `next`. */
    public String requiresFlag;

    /** Node berikutnya jika tidak ada choices (linear). */
    public String next;

    public ArrayList<DialogChoice> choices = new ArrayList<>();
}
