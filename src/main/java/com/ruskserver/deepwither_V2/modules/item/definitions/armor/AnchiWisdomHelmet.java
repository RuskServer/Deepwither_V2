package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AnchiWisdomHelmet implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public AnchiWisdomHelmet() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 6.0);
    }

    @Override
    public String getId() {
        return "anchi_wisdom_helmet";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§f§l黯智のヘルメット";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public double getSellPrice() {
        return 300.0;
    }

    @Override
    public String getFlavorText() {
        return "LunarisAtelier製の安価なヘルメット";
    }
}
