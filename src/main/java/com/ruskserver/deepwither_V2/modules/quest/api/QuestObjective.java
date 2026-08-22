package com.ruskserver.deepwither_V2.modules.quest.api;

import org.bukkit.entity.Player;

public interface QuestObjective {

    default ObjectiveType getType() {
        return ObjectiveType.COLLECT_ITEM;
    }

    default String getProgressKey() {
        return getItemId() != null ? getItemId() : "obj";
    }

    default int getRequiredAmount() {
        return 1;
    }

    default String getItemId() {
        return null;
    }

    default String getDescription() {
        return getItemId() != null ? getItemId() : "目標";
    }

    default boolean isCompleted(int currentCount, Player player) {
        return currentCount >= getRequiredAmount();
    }

    default String getProgressDisplay(int currentCount, Player player) {
        boolean completed = isCompleted(currentCount, player);
        String color = completed ? "§a" : "§e";
        return " §7・" + getDescription() + ": " + color + Math.min(currentCount, getRequiredAmount()) + "§7/§a" + getRequiredAmount();
    }
}
