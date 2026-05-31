package com.ruskserver.deepwither_V2.modules.item.definitions.material;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Component
public class AbyssShardItem implements CustomItem {
    @Override
    public String getId() {
        return "abyss_shard";
    }

    @Override
    public Material getMaterial() {
        return Material.AMETHYST_SHARD;
    }

    @Override
    public String getDisplayName() {
        return "§9§l深淵の破片";
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
        return "闇の深淵から採掘された結晶。";
    }

    @Override
    public double getSellPrice() {
        return 500.0;
    }
}
