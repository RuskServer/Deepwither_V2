package com.ruskserver.deepwither_V2.modules.quest.api.rewards;

import com.ruskserver.deepwither_V2.modules.quest.api.QuestReward;

public class DungeonMapReward implements QuestReward {

    private final String dungeonId;

    public DungeonMapReward(String dungeonId) {
        this.dungeonId = dungeonId;
    }

    @Override
    public String getDungeonId() {
        return dungeonId;
    }

    @Override
    public String getDescription() {
        return "ダンジョン地図: " + dungeonId;
    }
}
