package com.ruskserver.deepwither_V2.modules.quest.api;

import java.util.List;

public interface Quest {
    String getId();
    String getNpcName();
    QuestType getType();
    String getDungeonId();
    List<? extends QuestObjective> getObjectives();
    List<? extends QuestReward> getRewards();
}
