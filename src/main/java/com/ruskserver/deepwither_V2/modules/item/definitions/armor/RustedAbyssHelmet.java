package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RustedAbyssHelmet implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RustedAbyssHelmet() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 6.0);
    }

    @Override
    public String getId() {
        return "rusted_abyss_helmet";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§f§l錆朽ちた深淵板ヘルメット";
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
}
