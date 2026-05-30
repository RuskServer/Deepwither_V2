package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RCRubyAegisChestplate implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RCRubyAegisChestplate() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 22.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 42.0);
    }

    @Override
    public String getId() {
        return "rc_ruby_aegis_chestplate";
    }

    @Override
    public Material getMaterial() {
        return Material.CHAINMAIL_CHESTPLATE;
    }

    @Override
    public String getDisplayName() {
        return "§4§l紅晶拒界装甲《ルビア・エギス》";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.LEGENDARY;
    }

    @Override
    public String getFlavorText() {
        return "エーテル合金製の高密度装甲殻に、魔力共振を破壊するルビー結晶層を組み込んだ主装甲。生体反応を排した設計により、魔導攻撃を『意味のない振動』へと変換する。";
    }
}
