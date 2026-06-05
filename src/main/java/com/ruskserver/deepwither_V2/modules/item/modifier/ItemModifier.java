package com.ruskserver.deepwither_V2.modules.item.modifier;

import com.ruskserver.deepwither_V2.core.stat.StatType;

public class ItemModifier {

    private final StatType statType;
    private final double value;
    private final boolean isAddedStat;

    public ItemModifier(StatType statType, double value, boolean isAddedStat) {
        this.statType = statType;
        this.value = value;
        this.isAddedStat = isAddedStat;
    }

    public StatType getStatType() {
        return statType;
    }

    public double getValue() {
        return value;
    }

    public boolean isAddedStat() {
        return isAddedStat;
    }
}
