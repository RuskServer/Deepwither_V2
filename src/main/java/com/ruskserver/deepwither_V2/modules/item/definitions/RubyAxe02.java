package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RubyAxe02 implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RubyAxe02() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 80.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.85);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 5.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
    }

    @Override
    public String getId() {
        return "rc_ruby_axe_02";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public String getDisplayName() {
        return "§c§l紅晶戦斧《ルビア・カルナス》";
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
        return "Redline Construct製、生体構造体の筋束を束ね、巨大なルビー結晶刃を支える戦斧。切断力を最優先に設計されており、破壊の瞬間だけ、内部構造が強く収縮する。";
    }

    @Override
    public int getCustomModelData() {
        return 8;
    }

    @Override
    public String getWeaponType() {
        return "斧";
    }
}
