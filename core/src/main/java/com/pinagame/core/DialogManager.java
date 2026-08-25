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

    public void loadGraph(String internalPath) {
        Json json = new Json();
        String raw = Gdx.files.internal(internalPath).readString("UTF-8");
        this.graph = json.fromJson(DialogGraph.class, raw);
        this.ended = false;

        for (Map.Entry<String, DialogNode> entry : graph.nodes.entrySet()) {
            if (entry.getValue().id == null) {
                entry.getValue().id = entry.getKey();
            }
        }
    }

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

        if (node.text != null) {
            notifyLine(node.speaker, node.text);
        }

        if (node.choices != null && !node.choices.isEmpty()) {
            notifyChoices(node.choices);
        } else if (node.action != null && node.text == null) {
            if (node.next != null) goToNode(node.next);
        }
    }

    public void advance() {
        if (ended) return;
        if (currentNode == null) return;
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
    public boolean hasStarted() {
        return currentNode != null;
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
