package com.ruskserver.deepwither_V2.modules.skilltree.api;

import org.bukkit.Material;

import java.util.List;

public interface SkillTreeDefinition {

    String getId();

    String getDisplayName();

    default Material getIcon() {
        return Material.BOOK;
    }

    List<SkillTreeNode> getNodes();
}
