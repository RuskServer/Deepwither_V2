package com.ruskserver.deepwither_V2.modules.dialogue.service;

import com.ruskserver.deepwither_V2.modules.dialogue.api.DialogueChoice;
import com.ruskserver.deepwither_V2.modules.dialogue.api.DialogueGraph;
import com.ruskserver.deepwither_V2.modules.dialogue.api.DialogueResult;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DialogueSession {

    private final Player player;
    private final DialogueGraph graph;
    private final CompletableFuture<DialogueResult> future;
    private final Map<String, String> flags;
    private final List<String> visitedNodes;
    private final Location startLocation;
    private String currentNodeId;
    private boolean awaitingInput;
    private boolean ended;
    private int selectedIndex;
    private List<DialogueChoice> cachedChoices;

    public DialogueSession(Player player, DialogueGraph graph, CompletableFuture<DialogueResult> future) {
        this.player = player;
        this.graph = graph;
        this.future = future;
        this.flags = new HashMap<>();
        this.visitedNodes = new ArrayList<>();
        this.startLocation = player.getLocation().clone();
        this.currentNodeId = graph.startNodeId();
        this.awaitingInput = false;
        this.ended = false;
        this.selectedIndex = 0;
        this.cachedChoices = List.of();
    }

    public Player player() {
        return player;
    }

    public DialogueGraph graph() {
        return graph;
    }

    public CompletableFuture<DialogueResult> future() {
        return future;
    }

    public Map<String, String> flags() {
        return flags;
    }

    public Location startLocation() {
        return startLocation;
    }

    public String currentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String nodeId) {
        this.currentNodeId = nodeId;
        if (nodeId != null) {
            visitedNodes.add(nodeId);
        }
    }

    public boolean isAwaitingInput() {
        return awaitingInput;
    }

    public void setAwaitingInput(boolean awaiting) {
        this.awaitingInput = awaiting;
    }

    public boolean isEnded() {
        return ended;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }

    public List<DialogueChoice> cachedChoices() {
        return cachedChoices;
    }

    public void setCachedChoices(List<DialogueChoice> choices) {
        this.cachedChoices = choices != null ? choices : List.of();
    }

    public void complete(@Nullable String endNodeId, @Nullable String selectedChoiceText) {
        this.ended = true;
        this.awaitingInput = false;
        this.currentNodeId = endNodeId;
        future.complete(new DialogueResult(graph.id(), endNodeId, selectedChoiceText, Map.copyOf(flags)));
    }

    public void completeWithCancellation() {
        this.ended = true;
        this.awaitingInput = false;
        future.complete(new DialogueResult(graph.id(), currentNodeId, null, Map.copyOf(flags)));
    }
}
