package com.ruskserver.deepwither_V2.modules.item.definitions;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

@Component
public class GhoulVisceraItem implements CustomItem {

    private final Map<StatType, Double> baseStats;

    @Inject
    public GhoulVisceraItem() {
        this.baseStats = new EnumMap<>(StatType.class);
    }

    @Override
    public String getId() {
        return "ghoul_viscera";
    }

    @Override
    public Material getMaterial() {
        return Material.ROTTEN_FLESH;
    }

    @Override
    public String getDisplayName() {
        return "グールの内臓";
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
        return "腐った肉の塊。料理には使えないが、特殊な調合に使われるかもしれない。";
    }

    @Override
    public double getSellPrice() {
        return 12.0;
    }

    @Override
    public int getCustomModelData() {
        return 2;
    }
}
