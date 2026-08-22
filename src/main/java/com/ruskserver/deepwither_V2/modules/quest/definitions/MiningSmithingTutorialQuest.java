package com.ruskserver.deepwither_V2.modules.quest.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.modules.quest.api.Quest;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestCategory;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestObjective;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestReward;
import com.ruskserver.deepwither_V2.modules.quest.api.objectives.CollectItemObjective;
import com.ruskserver.deepwither_V2.modules.quest.api.rewards.ItemReward;

import java.util.List;

@Component
public class MiningSmithingTutorialQuest implements Quest {

    public static final String ID = "tutorial_mining_smithing";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return "採掘と鍛冶の基礎";
    }

    @Override
    public QuestCategory getCategory() {
        return QuestCategory.TUTORIAL;
    }

    @Override
    public String getDescription() {
        return "鉱脈から粗金塊を掘り出し、金精インゴット精錬と鉄ピッケル合成を体験する。";
    }

    @Override
    public String getNpcName() {
        return "鍛冶屋";
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public List<? extends QuestObjective> getObjectives() {
        return List.of(
                new CollectItemObjective("raw_gold_chunk", 10, "粗金塊収集"),
                new CollectItemObjective("auric_ingot", 2, "金精インゴット精錬"),
                new CollectItemObjective("used_iron_pickaxe", 1, "中古の鉄ピッケル合成")
        );
    }

    @Override
    public List<? extends QuestReward> getRewards() {
        return List.of(
                new ItemReward("healing_potion", 2, "治癒のポーション x2"),
                new ItemReward("mana_potion", 2, "マナポーション x2"),
                new ItemReward("raw_gold_chunk", 5, "粗金塊 x5")
        );
    }
}
