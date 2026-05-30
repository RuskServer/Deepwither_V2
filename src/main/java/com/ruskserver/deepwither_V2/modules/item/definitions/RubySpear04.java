package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class RubySpear04 implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public RubySpear04() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 54.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 1.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 100.0);
    }

    @Override
    public String getId() {
        return "rc_ruby_spear_04";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public String getDisplayName() {
        return "§c§l紅晶槍《ルビア・シナプス》";
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
        return "Redline Construct製、神経伝達用に調整された生体軸と、高純度ルビー結晶穂先を接続した刺突槍。使用者の動きに即応する反応速度を持ち、生き物のように\"先に届く\"感覚を与える。";
    }

    @Override
    public int getCustomModelData() {
        return 7;
    }

    @Override
    public String getWeaponType() {
        return "槍";
    }
}
