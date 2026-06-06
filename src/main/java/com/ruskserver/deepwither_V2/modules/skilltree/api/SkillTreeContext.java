package com.ruskserver.deepwither_V2.modules.skilltree.api;

import com.ruskserver.deepwither_V2.modules.skilltree.provider.CharacterSkillTreeProvider;
import org.bukkit.entity.Player;

public class SkillTreeContext {

    private final Player player;
    private final SkillTreeDefinition tree;
    private final SkillTreeNode node;
    private final CharacterSkillTreeProvider.SkillTreeData data;

    public SkillTreeContext(Player player, SkillTreeDefinition tree, SkillTreeNode node, CharacterSkillTreeProvider.SkillTreeData data) {
        this.player = player;
        this.tree = tree;
        this.node = node;
        this.data = data;
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

    public CharacterSkillTreeProvider.SkillTreeData getData() {
        return data;
    }
}
