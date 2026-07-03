package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Component
public class DungeonMapItem implements CustomItem {

    @Override
    public String getId() {
        return "dungeon_map";
    }

    @Override
    public Material getMaterial() {
        return Material.FILLED_MAP;
    }

    @Override
    public String getDisplayName() {
        return "§b§lダンジョン地図";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return Collections.emptyMap();
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public String getFlavorText() {
        return "古代の遺跡へと続く道が記された不思議な地図。";
    }

    @Override
    public double getSellPrice() {
        return 0;
    }

    @Override
    public int getCustomModelData() {
        return 9001;
    }
}
