package com.ruskserver.deepwither_V2.modules.quest.api.objectives;

import com.ruskserver.deepwither_V2.modules.quest.api.ObjectiveType;
import com.ruskserver.deepwither_V2.modules.quest.api.QuestObjective;
import org.bukkit.entity.Player;

public class KillMobObjective implements QuestObjective {

    private final String mobId;
    private final int requiredAmount;
    private final String description;

    public KillMobObjective(String mobId, int requiredAmount, String description) {
        this.mobId = mobId;
        this.requiredAmount = requiredAmount;
        this.description = description;
    }

    public KillMobObjective(String mobId, int requiredAmount) {
        this(mobId, requiredAmount, mobId + " 討伐");
    }

    public String getMobId() {
        return mobId;
    }

    @Override
    public ObjectiveType getType() {
        return ObjectiveType.KILL_MOB;
    }

    @Override
    public String getProgressKey() {
        return "kill_" + mobId;
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
