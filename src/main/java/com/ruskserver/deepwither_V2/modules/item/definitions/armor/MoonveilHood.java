package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonveilHood implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public MoonveilHood() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 18.0);
        this.baseStats.put(StatType.DEFENSE, 3.0);
        this.baseStats.put(StatType.MAX_MANA, 20.0);
    }

    @Override
    public String getId() {
        return "moonveil_hood";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§f§l月紡ぎのフード「ルナヴェイル・フード」";
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
        return "Lunaris Atelierが魔力干渉対策のために製造した軽量フード。月光銀糸が魔法への抵抗を高める。";
    }
}
