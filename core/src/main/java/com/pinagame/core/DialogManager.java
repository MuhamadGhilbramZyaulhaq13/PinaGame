package com.pinagame.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.pinagame.core.dialog.DialogChoice;
import com.pinagame.core.dialog.DialogGraph;
import com.pinagame.core.dialog.DialogNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Menjalankan satu DialogGraph: menampilkan baris demi baris, menangani percabangan
 * pilihan, dan memicu efek samping (ganti scene, set flag) lewat field "action" di node.
 *
 * NOTE TEKNIS: com.badlogic.gdx.utils.Json (dipakai di loadGraph) kadang butuh bantuan
 * untuk membaca Map<String, DialogNode> secara generik. Kalau saat testing field "nodes"
 * ternyata kosong setelah parsing, tambahkan sebelum fromJson():
 *   json.setElementType(DialogGraph.class, "nodes", DialogNode.class);
 * atau, kalau masih rewel, ganti ke library Gson yang penanganan generic-nya lebih longgar.
 */
public class DialogManager {

    public interface DialogListener {
        void onLine(String speaker, String text);
        void onChoices(List<DialogChoice> choices);
        void onSceneChangeRequested(String sceneId);
        void onDialogEnd();
    }

    private DialogGraph graph;
    private DialogNode currentNode;
    private boolean ended = false;
    private final StoryFlags flags;
    private final List<DialogListener> listeners = new CopyOnWriteArrayList<>();

    public DialogManager(StoryFlags flags) {
        this.flags = flags;
    }

    public void addListener(DialogListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(DialogListener listener) {
        listeners.remove(listener);
    }

    /** Load file dialog dari assets, contoh: "data/chapters/chapter1.json" */
    public void loadGraph(String internalPath) {
        Json json = new Json();
        String raw = Gdx.files.internal(internalPath).readString("UTF-8");
        this.graph = json.fromJson(DialogGraph.class, raw);
        this.ended = false; // chapter baru dimuat -> reset status "sudah tamat"

        // Isi id node otomatis dari key map kalau penulis JSON tidak mengisi field "id" manual.
        for (Map.Entry<String, DialogNode> entry : graph.nodes.entrySet()) {
            if (entry.getValue().id == null) {
                entry.getValue().id = entry.getKey();
            }
        }
    }

    /** nodeId null/kosong -> mulai dari graph.startNode (dipakai untuk chapter baru). */
    public void startFrom(String nodeId) {
        String target = (nodeId == null || nodeId.isEmpty()) ? graph.startNode : nodeId;
        goToNode(target);
    }

    public void goToNode(String nodeId) {
        if (ended) return; // chapter sudah tamat, abaikan navigasi lanjutan sampai chapter baru dimuat

        DialogNode node = graph.nodes.get(nodeId);
        if (node == null) {
            Gdx.app.error("DialogManager", "Node tidak ditemukan: " + nodeId);
            notifyEnd();
            return;
        }

        // Node bersyarat: kalau flag belum terpenuhi, loncat ke fallback "next".
        if (node.requiresFlag != null && !flags.getBoolean(node.requiresFlag)) {
            if (node.next != null) {
                goToNode(node.next);
            } else {
                notifyEnd();
            }
            return;
        }

        currentNode = node;

        boolean isSceneChangingAction = "CHANGE_SCENE".equals(node.action) || "DAY_BREAK".equals(node.action);
        if (isSceneChangingAction) {
            notifySceneChange(node.actionTarget);
        }
        if ("SET_FLAG".equals(node.action) && node.actionTarget != null) {
            flags.setBoolean(node.actionTarget, true);
        }
        if ("END_CHAPTER".equals(node.action)) {
            notifyEnd();
            return;
        }
        if (node.action != null
            && !"CHANGE_SCENE".equals(node.action)
            && !"SET_FLAG".equals(node.action)
            && !"DAY_BREAK".equals(node.action)) {
            // Bukan salah satu dari 4 action yang dikenali. Kasus paling umum:
            // penulis JSON gak sengaja nulis teks narasi di field "action",
            // padahal seharusnya di field "text". Kasih warning jelas di console
            // daripada node ini diam-diam tidak melakukan apa pun.
            Gdx.app.error("DialogManager", "Node '" + node.id + "' punya action tidak "
                + "dikenal: \"" + node.action + "\". action cuma boleh CHANGE_SCENE / "
                + "SET_FLAG / END_CHAPTER / DAY_BREAK. Kalau maksudnya teks narasi, taruh "
                + "di field \"text\" (dengan \"speaker\": \"Narrator\"), bukan di \"action\".");
        }

        if (node.text != null) {
            notifyLine(node.speaker, node.text);
        }

        if (node.choices != null && !node.choices.isEmpty()) {
            notifyChoices(node.choices);
        } else if (node.action != null && node.text == null) {
            // Node murni action (tanpa teks) -> langsung lanjut otomatis ke node
            // berikutnya, KECUALI DAY_BREAK: itu sengaja PAUSE di sini, menunggu
            // screen baru (mis. GardenScreen) memicu continueFromPause() sendiri
            // -- misalnya begitu pemain jalan mendekati NPC lagi di hari baru.
            if (!"DAY_BREAK".equals(node.action) && node.next != null) {
                goToNode(node.next);
            }
        }
        // Kalau ada node.text tapi tanpa choices, UI menunggu tap layar -> panggil advance().
    }

    /** Dipanggil UI ketika pemain tap layar untuk lanjut ke baris berikutnya. */
    public void advance() {
        if (ended) return;
        if (currentNode == null) return;

        // Node yang lagi nampilin pilihan sengaja tidak punya "next" -- dia nunggu
        // pemain klik salah satu TOMBOL pilihan (lewat selectChoice()), bukan klik
        // sembarangan di background. Abaikan klik "lanjut" biasa di kondisi ini,
        // supaya tidak salah dianggap "dialog tamat".
        if (currentNode.choices != null && !currentNode.choices.isEmpty()) {
            return;
        }

        if (currentNode.next != null) {
            goToNode(currentNode.next);
        } else {
            notifyEnd();
        }
    }

    public void selectChoice(int index) {
        if (currentNode == null || currentNode.choices == null) return;
        DialogChoice choice = currentNode.choices.get(index);
        if (choice.setFlag != null) {
            flags.setFlag(choice.setFlag, choice.setFlagValue != null ? choice.setFlagValue : "true");
        }
        goToNode(choice.next);
    }

    public String getCurrentNodeId() {
        return currentNode != null ? currentNode.id : null;
    }

    /**
     * true kalau dialog sudah menampilkan minimal 1 node (baik dari chapter baru
     * mulai, resume dari save, atau lagi di tengah alur CHANGE_SCENE). Dipakai
     * GardenScreen supaya trigger "jalan mendekat = mulai dialog" cuma aktif di
     * pertemuan PALING PERTAMA, bukan tiap kali balik ke Garden di tengah cerita.
     */
    public boolean hasStarted() {
        return currentNode != null;
    }

    /**
     * Lanjut dari node yang lagi "dijeda" (mis. abis node DAY_BREAK) ke node
     * berikutnya. BEDA dari startFrom(null) yang selalu balik ke node PALING AWAL
     * chapter -- ini melanjutkan persis dari titik jeda saat ini. Dipanggil
     * GardenScreen ketika pemain jalan mendekati NPC lagi di hari yang baru.
     */
    public void continueFromPause() {
        if (ended) return;
        if (currentNode != null && currentNode.next != null) {
            goToNode(currentNode.next);
        }
    }

    /**
     * Reset total status dialog (currentNode, ended, graph). Dipakai GameMain saat
     * pemain pilih "Mulai Baru" dari menu utama, SEBELUM chapter baru di-load --
     * tanpa ini, currentNode lama masih nyangkut dan bikin hasStarted() salah
     * ngasih tau screen kalau ini "kunjungan susulan", padahal ini awal permainan.
     */
    public void reset() {
        this.currentNode = null;
        this.ended = false;
        this.graph = null;
    }

    private void notifyLine(String speaker, String text) {
        for (DialogListener l : listeners) l.onLine(speaker, text);
    }

    private void notifyChoices(List<DialogChoice> choices) {
        for (DialogListener l : listeners) l.onChoices(choices);
    }

    private void notifySceneChange(String sceneId) {
        for (DialogListener l : listeners) l.onSceneChangeRequested(sceneId);
    }

    private void notifyEnd() {
        ended = true;
        for (DialogListener l : listeners) l.onDialogEnd();
    }
}
