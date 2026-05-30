package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.BowItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RubyBow07 implements BowItem {

    private final Map<StatType, Double> baseStats;

    public RubyBow07() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 62.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 10.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 155.0);
    }

    @Override
    public String getId() {
        return "rc_ruby_bow_07";
    }

    @Override
    public Material getMaterial() {
        return Material.BOW;
    }

    @Override
    public String getDisplayName() {
        return "§c§l紅晶弓《ルビア・カーナ》";
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
        return "Redline Construct製、生体構造体素材を基盤に高純度ルビー結晶を神経束のように編み込んだ特殊弓。血を啜ることはないが、引き絞るたびに内部構造が脈動するかのように共鳴する。";
    }

    @Override
    public int getCustomModelData() {
        return 2;
    }

    @Override
    public String getWeaponType() {
        return "弓";
    }
}
