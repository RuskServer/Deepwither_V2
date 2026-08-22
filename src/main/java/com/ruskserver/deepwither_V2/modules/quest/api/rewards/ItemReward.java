package com.ruskserver.deepwither_V2.modules.quest.api.rewards;

import com.ruskserver.deepwither_V2.modules.quest.api.QuestReward;

public class ItemReward implements QuestReward {

    private final String itemId;
    private final int amount;
    private final String description;

    public ItemReward(String itemId, int amount, String description) {
        this.itemId = itemId;
        this.amount = amount;
        this.description = description;
    }

    public ItemReward(String itemId, int amount) {
        this(itemId, amount, itemId + " x" + amount);
    }

    @Override
    public String getItemId() {
        return itemId;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
