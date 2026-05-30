package com.ruskserver.deepwither_V2.modules.item.definitions.armor;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RCRubyAegisHood implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RCRubyAegisHood() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.DEFENSE, 12.0);
        this.baseStats.put(StatType.MAGIC_DEFENSE, 28.0);
    }

    @Override
    public String getId() {
        return "rc_ruby_aegis_hood";
    }

    @Override
    public Material getMaterial() {
        return Material.CHAINMAIL_HELMET;
    }

    @Override
    public String getDisplayName() {
        return "§4§l紅晶拒界フード《ルビア・エギス》";
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
        return "Redline Constructが開発した対魔・対物理両立型防具。エーテル合金製の内部骨格に、魔力遮断特性を持つルビー結晶を散布配置している。魔法を増幅せず、ただ拒絶するための装甲。";
    }
}
