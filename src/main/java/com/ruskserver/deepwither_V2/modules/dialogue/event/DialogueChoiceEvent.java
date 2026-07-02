package com.ruskserver.deepwither_V2.modules.dialogue.event;

import com.ruskserver.deepwither_V2.modules.dialogue.api.DialogueChoice;
import com.ruskserver.deepwither_V2.modules.dialogue.api.DialogueGraph;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DialogueChoiceEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final DialogueGraph graph;
    private final DialogueChoice choice;
    private final Map<String, String> flags;
    private boolean cancelled;

    public DialogueChoiceEvent(Player player, DialogueGraph graph, DialogueChoice choice, Map<String, String> flags) {
        this.player = player;
        this.graph = graph;
        this.choice = choice;
        this.flags = flags;
    }

    public Player getPlayer() { return player; }
    public DialogueGraph getGraph() { return graph; }
    public DialogueChoice getChoice() { return choice; }
    public String getSelectedChoiceText() { return choice.text(); }
    public Map<String, String> getFlags() { return flags; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
