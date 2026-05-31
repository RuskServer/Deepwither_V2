package com.ruskserver.deepwither_V2.modules.item.definitions.material;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GhoulRemnantItem implements CustomItem {

    private final Map<StatType, Double> baseStats;

    @Inject
    public GhoulRemnantItem() {
        this.baseStats = new EnumMap<>(StatType.class);
    }

    @Override
    public String getId() {
        return "ghoul_remnant";
    }

    @Override
    public Material getMaterial() {
        return Material.GUNPOWDER;
    }

    @Override
    public String getDisplayName() {
        return "グールの残滓";
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
        return "グールが放つ不吉な気配を含んだ、褐色の粉々に砕けた骨。";
    }

    @Override
    public double getSellPrice() {
        return 15.0;
    }

    @Override
    public int getCustomModelData() {
        return 1;
    }
}
