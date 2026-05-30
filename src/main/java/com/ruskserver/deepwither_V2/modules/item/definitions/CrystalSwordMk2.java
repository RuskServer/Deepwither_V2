package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class CrystalSwordMk2 implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public CrystalSwordMk2() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 68.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 12.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 120.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.35);
        this.baseStats.put(StatType.COOLDOWN_REDUCTION, 7.0);
        this.baseStats.put(StatType.MAGIC_DAMAGE, 30.0);
    }

    @Override
    public String getId() {
        return "crystal_sword_mk2";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§d§lCrystal Sword MK2";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.EPIC;
    }

    @Override
    public String getFlavorText() {
        return "Lunaris Atelierが“Crystal Sword”の戦術設計を再構築し、刀身に高純度ルナ結晶合金と新世代魔力導路《AstraLine-II》を採用した改良モデル。従来機より出力・反応性・魔力増幅が大幅に向上した。";
    }

    @Override
    public int getCustomModelData() {
        return 6;
    }

    @Override
    public double getSellPrice() {
        return 3500.0;
    }

    @Override
    public String getWeaponType() {
        return "剣";
    }
}
