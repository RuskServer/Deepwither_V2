package com.ruskserver.deepwither_V2.modules.item.modifier;

import com.ruskserver.deepwither_V2.core.stat.StatType;

import java.util.EnumMap;
import java.util.Map;

public enum StatAffinity {
    PRIMARY(10.0),
    SECONDARY(5.0),
    RELATED(2.0),
    UNRELATED(0.5),
    BLOCKED(0.0);

    private final double weight;

    StatAffinity(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    private static final Map<ItemCategory, Map<StatType, StatAffinity>> AFFINITY_MATRIX = buildMatrix();

    public static StatAffinity of(ItemCategory category, StatType stat) {
        Map<StatType, StatAffinity> row = AFFINITY_MATRIX.get(category);
        if (row == null) return UNRELATED;
        return row.getOrDefault(stat, UNRELATED);
    }

    private static Map<ItemCategory, Map<StatType, StatAffinity>> buildMatrix() {
        Map<ItemCategory, Map<StatType, StatAffinity>> matrix = new EnumMap<>(ItemCategory.class);

        matrix.put(ItemCategory.MELEE_WEAPON, meleeMap());
        matrix.put(ItemCategory.MAGIC_WEAPON, magicMap());
        matrix.put(ItemCategory.RANGED_WEAPON, rangedMap());
        matrix.put(ItemCategory.ARMOR, armorMap());
        matrix.put(ItemCategory.HYBRID, hybridMap());

        return matrix;
    }

    private static Map<StatType, StatAffinity> meleeMap() {
        Map<StatType, StatAffinity> m = new EnumMap<>(StatType.class);
        m.put(StatType.ATTACK_DAMAGE, PRIMARY);
        m.put(StatType.CRITICAL_CHANCE, PRIMARY);
        m.put(StatType.CRITICAL_DAMAGE, PRIMARY);
        m.put(StatType.ATTACK_SPEED, SECONDARY);
        m.put(StatType.DEFENSE, SECONDARY);
        m.put(StatType.HEALTH, SECONDARY);
        m.put(StatType.FIRE_DAMAGE, SECONDARY);
        m.put(StatType.ICE_DAMAGE, SECONDARY);
        m.put(StatType.LIGHTNING_DAMAGE, SECONDARY);
        m.put(StatType.SPEED, RELATED);
        m.put(StatType.COOLDOWN_REDUCTION, RELATED);
        m.put(StatType.MAGIC_DEFENSE, RELATED);
        m.put(StatType.MAGIC_DAMAGE, BLOCKED);
        m.put(StatType.MAX_MANA, BLOCKED);
        return m;
    }

    private static Map<StatType, StatAffinity> magicMap() {
        Map<StatType, StatAffinity> m = new EnumMap<>(StatType.class);
        m.put(StatType.MAGIC_DAMAGE, PRIMARY);
        m.put(StatType.MAX_MANA, PRIMARY);
        m.put(StatType.COOLDOWN_REDUCTION, PRIMARY);
        m.put(StatType.CRITICAL_CHANCE, SECONDARY);
        m.put(StatType.CRITICAL_DAMAGE, SECONDARY);
        m.put(StatType.MAGIC_DEFENSE, SECONDARY);
        m.put(StatType.ATTACK_SPEED, SECONDARY);
        m.put(StatType.FIRE_DAMAGE, SECONDARY);
        m.put(StatType.ICE_DAMAGE, SECONDARY);
        m.put(StatType.LIGHTNING_DAMAGE, SECONDARY);
        m.put(StatType.HEALTH, RELATED);
        m.put(StatType.SPEED, RELATED);
        m.put(StatType.ATTACK_DAMAGE, BLOCKED);
        m.put(StatType.DEFENSE, BLOCKED);
        return m;
    }

    private static Map<StatType, StatAffinity> rangedMap() {
        Map<StatType, StatAffinity> m = new EnumMap<>(StatType.class);
        m.put(StatType.ATTACK_DAMAGE, PRIMARY);
        m.put(StatType.CRITICAL_CHANCE, PRIMARY);
        m.put(StatType.CRITICAL_DAMAGE, PRIMARY);
        m.put(StatType.ATTACK_SPEED, SECONDARY);
        m.put(StatType.SPEED, SECONDARY);
        m.put(StatType.HEALTH, RELATED);
        m.put(StatType.COOLDOWN_REDUCTION, RELATED);
        m.put(StatType.FIRE_DAMAGE, SECONDARY);
        m.put(StatType.ICE_DAMAGE, SECONDARY);
        m.put(StatType.LIGHTNING_DAMAGE, SECONDARY);
        m.put(StatType.DEFENSE, RELATED);
        m.put(StatType.MAGIC_DEFENSE, RELATED);
        m.put(StatType.MAGIC_DAMAGE, UNRELATED);
        m.put(StatType.MAX_MANA, BLOCKED);
        return m;
    }

    private static Map<StatType, StatAffinity> armorMap() {
        Map<StatType, StatAffinity> m = new EnumMap<>(StatType.class);
        m.put(StatType.DEFENSE, PRIMARY);
        m.put(StatType.MAGIC_DEFENSE, PRIMARY);
        m.put(StatType.HEALTH, PRIMARY);
        m.put(StatType.ATTACK_DAMAGE, SECONDARY);
        m.put(StatType.MAGIC_DAMAGE, SECONDARY);
        m.put(StatType.COOLDOWN_REDUCTION, SECONDARY);
        m.put(StatType.SPEED, RELATED);
        m.put(StatType.MAX_MANA, RELATED);
        m.put(StatType.CRITICAL_CHANCE, RELATED);
        m.put(StatType.CRITICAL_DAMAGE, RELATED);
        m.put(StatType.ATTACK_SPEED, UNRELATED);
        m.put(StatType.FIRE_DAMAGE, UNRELATED);
        m.put(StatType.ICE_DAMAGE, UNRELATED);
        m.put(StatType.LIGHTNING_DAMAGE, UNRELATED);
        return m;
    }

    private static Map<StatType, StatAffinity> hybridMap() {
        Map<StatType, StatAffinity> m = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            m.put(type, SECONDARY);
        }
        return m;
    }
}
