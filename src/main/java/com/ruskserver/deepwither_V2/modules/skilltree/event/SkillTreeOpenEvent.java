package com.ruskserver.deepwither_V2.modules.skilltree.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkillTreeOpenEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String treeId;

    public SkillTreeOpenEvent(Player player, String treeId) {
        this.player = player;
        this.treeId = treeId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getTreeId() {
        return treeId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
