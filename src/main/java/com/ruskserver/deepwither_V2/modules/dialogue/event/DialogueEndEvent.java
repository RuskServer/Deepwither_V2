package com.ruskserver.deepwither_V2.modules.dialogue.event;

import com.ruskserver.deepwither_V2.modules.dialogue.api.DialogueResult;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class DialogueEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final DialogueResult result;

    public DialogueEndEvent(Player player, DialogueResult result) {
        this.player = player;
        this.result = result;
    }

    public Player getPlayer() { return player; }
    public DialogueResult getResult() { return result; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
