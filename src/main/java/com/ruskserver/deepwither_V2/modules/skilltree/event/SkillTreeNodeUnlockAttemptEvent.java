package com.ruskserver.deepwither_V2.modules.skilltree.event;

import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkillTreeNodeUnlockAttemptEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SkillTreeDefinition tree;
    private final SkillTreeNode node;
    private boolean cancelled;

    public SkillTreeNodeUnlockAttemptEvent(Player player, SkillTreeDefinition tree, SkillTreeNode node) {
        this.player = player;
        this.tree = tree;
        this.node = node;
    }

    public Player getPlayer() {
        return player;
    }

    public SkillTreeDefinition getTree() {
        return tree;
    }

    public SkillTreeNode getNode() {
        return node;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
