package com.ruskserver.deepwither_V2.modules.item.definitions.material;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Component
public class EternalIceShardItem implements CustomItem {
    @Override
    public String getId() {
        return "eternal_ice_shard";
    }

    @Override
    public Material getMaterial() {
        return Material.AMETHYST_SHARD;
    }

    @Override
    public String getDisplayName() {
        return "§d永劫の氷片";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return Collections.emptyMap();
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.RARE;
    }

    @Override
    public String getFlavorText() {
        return "永劫の残氷殿の奥底でしか採れない、時を凍らせた氷晶。";
    }

    @Override
    public double getSellPrice() {
        return 2500.0;
    }
}
