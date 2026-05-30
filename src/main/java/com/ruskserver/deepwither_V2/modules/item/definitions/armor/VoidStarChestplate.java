package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class VoidStarChestplate implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public VoidStarChestplate() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 12.0);
    }

    @Override
    public String getId() {
        return "void_star_chestplate";
    }

    @Override
    public Material getMaterial() {
        return Material.NETHERITE_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§f§l虚星のチェストプレート";
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
    public String getFlavorText() {
        return "LunarisAtelier製の安価なチェストプレート";
    }
}
