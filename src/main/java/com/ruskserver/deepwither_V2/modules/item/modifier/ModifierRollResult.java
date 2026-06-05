package com.ruskserver.deepwither_V2.modules.item.modifier;

import com.ruskserver.deepwither_V2.core.stat.StatType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModifierRollResult {

    private final Map<StatType, Double> baseModifiers;
    private final Map<StatType, Double> addedStats;
    private final List<SpecialEffectInstance> specialEffects;

    public ModifierRollResult(Map<StatType, Double> baseModifiers,
                              Map<StatType, Double> addedStats,
                              List<SpecialEffectInstance> specialEffects) {
        this.baseModifiers = baseModifiers;
        this.addedStats = addedStats;
        this.specialEffects = specialEffects;
    }

    public Map<StatType, Double> getBaseModifiers() {
        return baseModifiers;
    }

    public Map<StatType, Double> getAddedStats() {
        return addedStats;
    }

    public List<SpecialEffectInstance> getSpecialEffects() {
        return specialEffects;
    }

    public boolean hasAnyModifier() {
        return !baseModifiers.isEmpty() || !addedStats.isEmpty() || !specialEffects.isEmpty();
    }
}
