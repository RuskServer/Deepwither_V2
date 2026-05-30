package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RubySword03 implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RubySword03() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 60.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.6);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 145.0);
    }

    @Override
    public String getId() {
        return "rc_ruby_sword_03";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_SWORD;
    }

    @Override
    public String getDisplayName() {
        return "§c§l紅晶剣《ルビア・ヴェイン》";
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
        return "Redline Construct製の生体剣。金属骨格の代わりに、ルビー結晶を血管状の生体繊維で固定する異端設計。血を吸うことはないが、振るうたびに内部で脈動するような振動が伝わる。";
    }

    @Override
    public int getCustomModelData() {
        return 27;
    }

    @Override
    public String getWeaponType() {
        return "剣";
    }
}
