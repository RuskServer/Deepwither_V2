package com.ruskserver.deepwither_V2.modules.item.definitions.material;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;
import com.ruskserver.deepwither_V2.modules.item.api.ItemRarity;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

@Component
public class VoidStarDustItem implements CustomItem {
    @Override
    public String getId() {
        return "void_star_dust";
    }

    @Override
    public Material getMaterial() {
        return Material.SUGAR; // 虚無的な白い粉として
    }

    @Override
    public String getDisplayName() {
        return "§d§l虚星の塵";
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
        return "崩壊した星の核から溢れ出した塵。";
    }

    @Override
    public double getSellPrice() {
        return 1200.0;
    }
}
