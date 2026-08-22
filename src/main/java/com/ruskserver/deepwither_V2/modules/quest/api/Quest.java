package com.ruskserver.deepwither_V2.modules.quest.api;

import java.util.List;

public interface Quest {
    String getId();

    default String getTitle() {
        return getId();
    }

    default QuestCategory getCategory() {
        return QuestCategory.MAIN;
    }

    default String getDescription() {
        return "";
    }

    String getNpcName();

    default QuestType getType() {
        return QuestType.COLLECTION;
    }

    default String getDungeonId() {
        return null;
    }

    default boolean isRepeatable() {
        return getCategory() == QuestCategory.DAILY;
    }

    List<? extends QuestObjective> getObjectives();

    List<? extends QuestReward> getRewards();
}
