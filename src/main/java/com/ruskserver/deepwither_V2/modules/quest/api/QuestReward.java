package com.ruskserver.deepwither_V2.modules.quest.api;

import org.bukkit.entity.Player;

public interface QuestReward {

    default String getDungeonId() {
        return null;
    }

    default String getItemId() {
        return null;
    }

    default int getAmount() {
        return 1;
    }

    default String getDescription() {
        return "報酬";
    }

    default void grant(Player player) {
    }
}
