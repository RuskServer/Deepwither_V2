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
public class GhoulEssenceItem implements CustomItem {

    private final Map<StatType, Double> baseStats;

    @Inject
    public GhoulEssenceItem() {
        this.baseStats = new EnumMap<>(StatType.class);
    }

    @Override
    public String getId() {
        return "ghoul_essence";
    }

    @Override
    public Material getMaterial() {
        return Material.END_ROD;
    }

    @Override
    public String getDisplayName() {
        return "グールの精髄";
    }

    @Override
    public Map<StatType, Double> getBaseStats() {
        return baseStats;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.UNCOMMON;
    }

    @Override
    public String getFlavorText() {
        return "グールの命の源と成り得る、青白いエネルギーの結晶。";
    }

    @Override
    public double getSellPrice() {
        return 25.0;
    }

    @Override
    public int getCustomModelData() {
        return 3;
    }
}
