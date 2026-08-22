package com.ruskserver.deepwither_V2.modules.quest.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.quest.api.Quest;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestCategory;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestObjective;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestReward;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestType;
import com.ruskserver.deepwither_V2.modules.quest.api.objectives.CollectItemObjective;
import com.ruskserver.deepwither_V2.modules.quest.api.rewards.DungeonMapReward;

import java.util.List;

@Component
public class DailyCollectionQuest implements Quest {

    public static final String ID = "daily_collection";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return "村長の収集依頼";
    }

    @Override
    public QuestCategory getCategory() {
        return QuestCategory.DAILY;
    }

    @Override
    public String getNpcName() {
        return "村長";
    }

    @Override
    public QuestType getType() {
        return QuestType.COLLECTION;
    }

    @Override
    public String getDungeonId() {
        return "eternal_ice";
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public List<? extends QuestObjective> getObjectives() {
        return List.of(
                new CollectItemObjective("ghoul_viscera", 20, "グールの臓"),
                new CollectItemObjective("ghoul_remnant", 10, "グールの残滓"),
                new CollectItemObjective("moonlight_residue", 15, "月光の残滓")
        );
    }

    @Override
    public List<? extends QuestReward> getRewards() {
        return List.of(new DungeonMapReward("eternal_ice"));
    }
}
