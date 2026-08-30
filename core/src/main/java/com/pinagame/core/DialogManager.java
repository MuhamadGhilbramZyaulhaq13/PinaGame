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
 * Action yang dikenali:
 *   CHANGE_SCENE   -> actionTarget = id scene tujuan, lanjut otomatis ke node berikutnya
 *   SET_FLAG       -> actionTarget = nama flag yang di-set true
 *   END_CHAPTER    -> tandai chapter tamat
 *   WAIT_APPROACH  -> PAUSE di sini (tidak auto-continue), menunggu screen (mis.
 *                     GardenScreen) memanggil continueFromPause() sendiri --
 *                     dipakai supaya narasi pembuka tiap hari muncul otomatis,
 *                     TAPI obrolan karakter baru lanjut setelah pemain jalanin
 *                     Pina mendekati NPC.
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

    /** Jeda minimum (ms) sebuah baris tampil sebelum tap "lanjut" mulai dianggap. */
    private static final long MIN_LINE_DISPLAY_MS = 3000L;

    private DialogGraph graph;
    private DialogNode currentNode;
    private boolean ended = false;
    private long lineShownAtMillis = 0L;
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
        this.ended = false;
        this.lineShownAtMillis = 0L;

        for (Map.Entry<String, DialogNode> entry : graph.nodes.entrySet()) {
            if (entry.getValue().id == null) {
                entry.getValue().id = entry.getKey();
            }
        }
    }

    /** nodeId null/kosong -> mulai dari graph.startNode. */
    public void startFrom(String nodeId) {
        String target = (nodeId == null || nodeId.isEmpty()) ? graph.startNode : nodeId;
        goToNode(target);
    }

    public void goToNode(String nodeId) {
        if (ended) return;

        DialogNode node = graph.nodes.get(nodeId);
        if (node == null) {
            Gdx.app.error("DialogManager", "Node tidak ditemukan: " + nodeId);
            notifyEnd();
            return;
        }

        if (node.requiresFlag != null && !flags.getBoolean(node.requiresFlag)) {
            if (node.next != null) {
                goToNode(node.next);
            } else {
                notifyEnd();
            }
            return;
        }

        currentNode = node;

        if ("CHANGE_SCENE".equals(node.action)) {
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
            && !isWaitAction(node.action)) {
            // Bukan salah satu action yang dikenali. Kasus paling umum: penulis JSON
            // gak sengaja nulis teks narasi di field "action", padahal seharusnya di
            // field "text". Kasih warning jelas di console daripada node ini diam-diam
            // tidak melakukan apa pun.
            Gdx.app.error("DialogManager", "Node '" + node.id + "' punya action tidak "
                + "dikenal: \"" + node.action + "\". action cuma boleh CHANGE_SCENE / "
                + "SET_FLAG / END_CHAPTER / WAIT_xxx. Kalau maksudnya teks narasi, "
                + "taruh di field \"text\" (dengan \"speaker\": \"Narrator\"), bukan di \"action\".");
        }

        if (node.text != null) {
            notifyLine(node.speaker, node.text);
        }

        if (node.choices != null && !node.choices.isEmpty()) {
            notifyChoices(node.choices);
        } else if (node.action != null && node.text == null) {
            // Node murni action (tanpa teks) -> langsung lanjut otomatis ke node
            // berikutnya, KECUALI action ber-prefix WAIT_ (mis. WAIT_APPROACH,
            // WAIT_FOLLOW): itu sengaja PAUSE, menunggu screen yang bersangkutan
            // memanggil continueFromPause() sendiri lewat pemicu masing-masing
            // (jalan mendekat, klik tombol tertentu, dll).
            if (!isWaitAction(node.action) && node.next != null) {
                goToNode(node.next);
            }
        }
    }

    /** true untuk action apa pun yang diawali "WAIT_" -- lihat catatan di goToNode(). */
    private static boolean isWaitAction(String action) {
        return action != null && action.startsWith("WAIT_");
    }

    /** Dipanggil UI ketika pemain tap layar untuk lanjut ke baris berikutnya. */
    public void advance() {
        if (ended) return;
        if (currentNode == null) return;

        // Node yang lagi nampilin pilihan sengaja tidak punya "next" -- dia nunggu
        // pemain klik salah satu TOMBOL pilihan (lewat selectChoice()).
        if (currentNode.choices != null && !currentNode.choices.isEmpty()) {
            return;
        }

        // Node ber-action WAIT_xxx (mis. WAIT_APPROACH, WAIT_FOLLOW) MUTLAK cuma
        // bisa dilewati lewat continueFromPause() yang dipicu screen terkait --
        // tap/klik biasa TIDAK PERNAH boleh melewatinya, walau currentNode.next ada.
        if (isWaitAction(currentNode.action)) {
            return;
        }

        // Jeda minimum supaya tap berkali-kali (tidak sabar nunggu baca) tidak
        // langsung nge-skip beberapa baris sekaligus.
        if (System.currentTimeMillis() - lineShownAtMillis < MIN_LINE_DISPLAY_MS) {
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

    public boolean hasStarted() {
        return currentNode != null;
    }

    /**
     * Lanjut dari node yang lagi "dijeda" (mis. abis node WAIT_APPROACH) ke node
     * berikutnya. Dipanggil GardenScreen ketika pemain jalan mendekati NPC.
     */
    public void continueFromPause() {
        if (ended) return;
        if (currentNode != null && currentNode.next != null) {
            goToNode(currentNode.next);
        }
    }

    /** Reset total status dialog. Dipakai GameMain saat pemain pilih "Mulai Baru". */
    public void reset() {
        this.currentNode = null;
        this.ended = false;
        this.lineShownAtMillis = 0L;
        this.graph = null;
    }

    private void notifyLine(String speaker, String text) {
        lineShownAtMillis = System.currentTimeMillis();
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
