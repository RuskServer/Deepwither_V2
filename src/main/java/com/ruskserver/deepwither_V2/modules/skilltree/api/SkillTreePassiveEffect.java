package com.ruskserver.deepwither_V2.modules.skilltree.api;

import org.bukkit.entity.Player;

public interface SkillTreePassiveEffect {

    SkillTreePassiveEffect NONE = new SkillTreePassiveEffect() {
    };

    default void apply(Player player, int level, SkillTreeContext context) {
    }

    default void clear(Player player, SkillTreeContext context) {
    }
}
