package com.ruskserver.deepwither_V2.modules.item.definitions.material;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Component
public class FrostPilgrimCoreItem implements CustomItem {
    @Override
    public String getId() {
        return "frost_pilgrim_core";
    }

    @Override
    public Material getMaterial() {
        return Material.BLUE_ICE;
    }

    @Override
    public String getDisplayName() {
        return "§b巡礼者の氷結核";
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
        return "氷結の巡礼者の心臓部。極寒の魔力が凝縮された結晶核。";
    }

    @Override
    public double getSellPrice() {
        return 500.0;
    }
}
