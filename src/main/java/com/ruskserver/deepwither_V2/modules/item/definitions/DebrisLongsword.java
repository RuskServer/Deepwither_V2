package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class DebrisLongsword implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public DebrisLongsword() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 10.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 150.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.2);
    }

    @Override
    public String getId() {
        return "debris_longsword";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§b§l瓦礫のロングソード";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return null;
    }

    @Override
    public String getWeaponType() {
        return "剣";
    }
}
