package com.ruskserver.deepwither_V2.modules.quest.api.objectives;

import com.ruskserver.deepwither_V2.modules.quest.api.ObjectiveType;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestObjective;
import org.bukkit.entity.Player;

public class CraftItemObjective implements QuestObjective {

    private final String itemId;
    private final int requiredAmount;
    private final String description;

    public CraftItemObjective(String itemId, int requiredAmount, String description) {
        this.itemId = itemId;
        this.requiredAmount = requiredAmount;
        this.description = description;
    }

    public CraftItemObjective(String itemId, int requiredAmount) {
        this(itemId, requiredAmount, itemId + " 合成");
    }

    @Override
    public ObjectiveType getType() {
        return ObjectiveType.CRAFT_ITEM;
    }

    @Override
    public String getItemId() {
        return itemId;
    }

    @Override
    public String getProgressKey() {
        return "craft_" + itemId;
    }

    @Override
    public int getRequiredAmount() {
        return requiredAmount;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isCompleted(int currentCount, Player player) {
        return currentCount >= requiredAmount;
    }
}
