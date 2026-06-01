package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonveilBoots implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public MoonveilBoots() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 16.0);
        this.baseStats.put(StatType.DEFENSE, 2.0);
        this.baseStats.put(StatType.SPEED, 0.01);
    }

    @Override
    public String getId() {
        return "moonveil_boots";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_BOOTS;
    }

    @Override
    public String getDisplayName() {
        return "§f§l月紡ぎの靴「ルナヴェイル・ブーツ」";
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
        return 800.0;
    }

    @Override
    public String getFlavorText() {
        return "魔導衝撃の反動を吸収し、軽やかな足取りを保つ革靴。月光粒子を模した装飾が、微弱な保護膜を生む。";
    }
}
