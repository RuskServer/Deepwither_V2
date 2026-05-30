package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class MoonveilTunic implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public MoonveilTunic() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 28.0);
        this.baseStats.put(StatType.DEFENSE, 5.0);
        this.baseStats.put(StatType.MAX_MANA, 30.0);
    }

    @Override
    public String getId() {
        return "moonveil_tunic";
    }

    @Override
    public Material getMaterial() {
        return Material.LEATHER_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§f§l月紡ぎの法衣「ルナヴェイル・チュニック」";
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
        return "月光銀糸を最も多く使用した中心装備。属性魔法の衝撃を拡散し、大幅な魔法耐性を得られる。";
    }
}
