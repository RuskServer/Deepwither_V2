package com.ruskserver.deepwither_V2.modules.item.modifier;

import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.BowItem;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.WandItem;

import java.util.Map;

public enum ItemCategory {

    MELEE_WEAPON,
    MAGIC_WEAPON,
    RANGED_WEAPON,
    ARMOR,
    HYBRID;

    private static final double ARMOR_WEIGHT_THRESHOLD = 0.4;

    public static ItemCategory fromItem(CustomItem item) {
        if (item instanceof WandItem) return MAGIC_WEAPON;
        if (item instanceof BowItem) return RANGED_WEAPON;

        Map<StatType, Double> baseStats = item.getBaseStats();
        if (baseStats.isEmpty()) return HYBRID;

        double total = baseStats.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) return HYBRID;

        double defWeight = weightOf(baseStats, StatType.DEFENSE, StatType.MAGIC_DEFENSE) / total;
        double atkWeight = weightOf(baseStats, StatType.ATTACK_DAMAGE) / total;
        double magicWeight = weightOf(baseStats, StatType.MAGIC_DAMAGE, StatType.MAX_MANA, StatType.COOLDOWN_REDUCTION) / total;

        if (defWeight >= ARMOR_WEIGHT_THRESHOLD) return ARMOR;
        if (magicWeight > atkWeight) return MAGIC_WEAPON;
        if (atkWeight > 0) return MELEE_WEAPON;

        return HYBRID;
    }

    private static double weightOf(Map<StatType, Double> stats, StatType... types) {
        double sum = 0;
        for (StatType type : types) {
            sum += stats.getOrDefault(type, 0.0);
        }
        return sum;
    }
}
