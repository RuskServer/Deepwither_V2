package com.ruskserver.deepwither_V2.modules.dialogue.event;

import com.ruskserver.deepwither_V2.modules.dialogue.api.DialogueGraph;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DialogueStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final DialogueGraph graph;
    private boolean cancelled;

    public DialogueStartEvent(Player player, DialogueGraph graph) {
        this.player = player;
        this.graph = graph;
    }

    public Player getPlayer() { return player; }
    public DialogueGraph getGraph() { return graph; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
