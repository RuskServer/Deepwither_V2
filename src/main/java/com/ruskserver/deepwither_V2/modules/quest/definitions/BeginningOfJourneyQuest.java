package com.ruskserver.deepwither_V2.modules.quest.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.quest.api.Quest;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestCategory;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestObjective;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestReward;
import com.ruskserver.deepwither_V2.modules.quest.api.objectives.KillMobObjective;
import com.ruskserver.deepwither_V2.modules.quest.api.rewards.ItemReward;

import java.util.List;

@Component
public class BeginningOfJourneyQuest implements Quest {

    public static final String ID = "main_beginning_of_journey";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return "旅の始まり";
    }

    @Override
    public QuestCategory getCategory() {
        return QuestCategory.MAIN;
    }

    @Override
    public String getDescription() {
        return "近くの古い砦に巣食うグールを討伐し、腕を磨く。";
    }

    @Override
    public String getNpcName() {
        return "村長";
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public List<? extends QuestObjective> getObjectives() {
        return List.of(
                new KillMobObjective("ghoul", 5, "砦のグール討伐")
        );
    }

    @Override
    public List<? extends QuestReward> getRewards() {
        return List.of(
                new ItemReward("starter_sword", 1, "初心者の剣"),
                new ItemReward("rusted_abyss_chestplate", 1, "錆びた深淵の胸当て"),
                new ItemReward("healing_potion", 3, "治癒のポーション x3")
        );
    }
}
