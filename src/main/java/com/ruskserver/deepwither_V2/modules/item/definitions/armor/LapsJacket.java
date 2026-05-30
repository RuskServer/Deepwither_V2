package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class LapsJacket implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public LapsJacket() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 12.0);
        this.baseStats.put(StatType.SPEED, 0.01);
    }

    @Override
    public String getId() {
        return "laps_jacket";
    }

    @Override
    public Material getMaterial() {
        return Material.DIAMOND_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§b§l機動軽装胸甲「ラプス・ジャケット」";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public String getFlavorText() {
        return "LazwardDynamicsが民間向け傭兵市場に投入した軽防護戦術ジャケット。装甲板は薄いが衝撃分散材により最低限の生存性を確保しつつ、長距離移動を妨げない柔軟性を維持している。";
    }
}
