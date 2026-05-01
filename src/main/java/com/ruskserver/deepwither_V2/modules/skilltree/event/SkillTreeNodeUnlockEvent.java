package com.ruskserver.deepwither_V2.modules.skilltree.event;

import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeDefinition;
import com.ruskserver.deepwither_V2.modules.skilltree.api.SkillTreeNode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkillTreeNodeUnlockEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final SkillTreeDefinition tree;
    private final SkillTreeNode node;
    private final int newLevel;

    public SkillTreeNodeUnlockEvent(Player player, SkillTreeDefinition tree, SkillTreeNode node, int newLevel) {
        this.player = player;
        this.tree = tree;
        this.node = node;
        this.newLevel = newLevel;
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

    public int getNewLevel() {
        return newLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
