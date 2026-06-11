package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.BowItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class SilverBranchBow implements BowItem {

    private final Map<StatType, Double> baseStats;

    public SilverBranchBow() {
        this.baseStats = new EnumMap<>(StatType.class);
        this.baseStats.put(StatType.RANGED_DAMAGE, 15.0);
        this.baseStats.put(StatType.CRITICAL_CHANCE, 8.0);
        this.baseStats.put(StatType.CRITICAL_DAMAGE, 150.0);
    }

    @Override
    public String getId() {
        return "silver_branch_bow";
    }

    @Override
    public Material getMaterial() {
        return Material.BOW;
    }

    @Override
    public String getDisplayName() {
        return "§f§l銀枝の弓";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "LunarisAtelier製の月光樹を模した量産訓練弓";
    }

    @Override
    public double getSellPrice() {
        return 50.0;
    }

    @Override
    public String getWeaponType() {
        return "弓";
    }
}
