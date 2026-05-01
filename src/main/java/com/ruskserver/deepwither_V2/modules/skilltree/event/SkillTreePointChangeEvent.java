package com.ruskserver.deepwither_V2.modules.skilltree.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkillTreePointChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int before;
    private final int after;
    private final String reason;

    public SkillTreePointChangeEvent(Player player, int before, int after, String reason) {
        this.player = player;
        this.before = before;
        this.after = after;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public int getBefore() {
        return before;
    }

    public int getAfter() {
        return after;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
