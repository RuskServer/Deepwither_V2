package com.ruskserver.deepwither_V2.modules.item.definitions.material;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Component
public class MoonlightResidueItem implements CustomItem {
    @Override
    public String getId() {
        return "moonlight_residue";
    }

    @Override
    public Material getMaterial() {
        return Material.GLOWSTONE_DUST;
    }

    @Override
    public String getDisplayName() {
        return "§f§l月光の残滓";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return Collections.emptyMap();
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.COMMON;
    }

    @Override
    public String getFlavorText() {
        return "月光が凝固して残った微かな欠片。";
    }

    @Override
    public double getSellPrice() {
        return 150.0;
    }
}
