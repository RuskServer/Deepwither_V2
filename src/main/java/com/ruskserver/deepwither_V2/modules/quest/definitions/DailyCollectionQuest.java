package com.ruskserver.deepwither_V2.modules.quest.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.quest.api.Quest;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestObjective;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestReward;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestType;

import java.util.List;

@Component
public class DailyCollectionQuest implements Quest {

    @Override
    public String getId() {
        return "daily_collection";
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
    public List<? extends QuestObjective> getObjectives() {
        return List.of(
                new CollectItemObjective("ghoul_viscera", 10),
                new CollectItemObjective("ghoul_remnant", 5),
                new CollectItemObjective("moonlight_residue", 8)
        );
    }

    @Override
    public List<? extends QuestReward> getRewards() {
        return List.of(new DungeonMapReward("eternal_ice"));
    }

    public record CollectItemObjective(String itemId, int requiredAmount) implements QuestObjective {
        @Override
        public String getItemId() { return itemId; }
        @Override
        public int getRequiredAmount() { return requiredAmount; }
    }

    public record DungeonMapReward(String dungeonId) implements QuestReward {
        @Override
        public String getDungeonId() { return dungeonId; }
    }
}
