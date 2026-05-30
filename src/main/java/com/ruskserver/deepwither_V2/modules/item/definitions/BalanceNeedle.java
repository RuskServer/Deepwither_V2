package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class BalanceNeedle implements CustomItem {

    private final Map<StatType, Double> baseStats;

    public BalanceNeedle() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.ATTACK_DAMAGE, 80.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 100.0);
        this.baseStats.put(StatType.ATTACK_SPEED, 0.85);
    }

    @Override
    public String getId() {
        return "hc_l19_balance_needle";
    }

    @Override
    public Material getMaterial() {
        return Material.IRON_AXE;
    }

    @Override
    public String getDisplayName() {
        return "§f§lHC-L19 『Balance Needle』";
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
        return "Harmonics Circleが各地の都市防衛軍向けに供給しているスタンダード量産槍。素材には同社が独自に生成する“魔力減衰合金（Mana-Damped Alloy）”が使用されている。";
    }

    @Override
    public int getCustomModelData() {
        return 5;
    }

    @Override
    public String getWeaponType() {
        return "槍";
    }
}
